/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.async.api.EventContext;
import com.tc.async.api.Stage;
import com.tc.async.impl.MockSink;
import com.tc.async.impl.MockStage;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.msg.NullMessageRecycler;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.impl.TestObjectManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.tx.BatchedTransactionProcessor;
import com.tc.objectserver.tx.BatchedTransactionProcessorImpl;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TestTransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchReader;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceID;
import com.tc.util.SequenceValidator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ProcessTransactionHandlerTest extends TCTestCase {

  private TestServerConfigurationContext    cctxt;
  private Map                               stageMap = new HashMap();
  private TestObjectManager                 objectManager;
  private ProcessTransactionHandler         handler;
  private TestTransactionBatchReaderFactory transactionBatchReaderFactory;
  private SynchronizedRef                   batchReader;
  private TestTransactionBatchManager       transactionBatchManager;
  private TestGlobalTransactionManager      gtxm;
  private SequenceValidator                 sequenceValidator;
  private BatchedTransactionProcessorImpl   batchTxnProcessor;

  public void setUp() throws Exception {
    objectManager = new TestObjectManager();
    transactionBatchManager = new TestTransactionBatchManager();
    gtxm = new TestGlobalTransactionManager();
    sequenceValidator = new SequenceValidator(0);
    MockStage batchTxnStage;
    stageMap.put(ServerConfigurationContext.BATCH_TRANSACTION_LOOKUP_STAGE,
                 (batchTxnStage = new MockStage(ServerConfigurationContext.BATCH_TRANSACTION_LOOKUP_STAGE)));
    batchTxnProcessor = new BatchedTransactionProcessorImpl(-1, sequenceValidator, objectManager, gtxm, batchTxnStage
        .getSink());
    handler = new ProcessTransactionHandler(transactionBatchManager, batchTxnProcessor, new NullMessageRecycler());

    transactionBatchReaderFactory = new TestTransactionBatchReaderFactory();
    cctxt = new TestServerConfigurationContext();
    batchReader = new SynchronizedRef(null);
    handler.initialize(cctxt);
  }

  public void tests() throws Exception {

    TestTransactionBatchReader batch = new TestTransactionBatchReader();
    batch.channelID = new ChannelID(1);
    batch.batchID = new TxnBatchID(1);

    final List dnaList = Collections.EMPTY_LIST;
    final Map newRootsMap = Collections.EMPTY_MAP;
    ServerTransaction serverTransaction = new ServerTransactionImpl(batch.batchID, new TransactionID(1),
                                                                    new SequenceID(1), new LockID[0], batch.channelID,
                                                                    dnaList, new ObjectStringSerializer(), newRootsMap,
                                                                    TxnType.NORMAL, new LinkedList());
    Collection completedTransactionIDs = new HashSet();
    for (int i = 0; i < 10; i++) {
      completedTransactionIDs.add(new GlobalTransactionID(i));
    }
    batch.acknowledged.addAll(completedTransactionIDs);
    batch.transactions.add(serverTransaction);

    batchReader.set(batch);

    // make sure our context queues are empty...
    assertTrue(transactionBatchManager.defineBatchContexts.isEmpty());
    assertTrue(objectManager.lookupObjectForCreateIfNecessaryContexts.isEmpty());
    // HANDLE EVENT
    objectManager.makePending = true;
    handler.handleEvent(null);
    // make sure defineBatch was called on the transaction manager.
    Object[] args = (Object[]) transactionBatchManager.defineBatchContexts.take();
    assertEquals(batch.channelID, args[0]);
    assertEquals(batch.batchID, args[1]);
    assertEquals(new Integer(1), args[2]);
    // there shouldn't be any more calls in the queue
    assertTrue(transactionBatchManager.defineBatchContexts.isEmpty());

    // make sure that a Transaction Queue is created and put into the Batch Transaction Lookup Stage
    MockSink batchTxnSink = (MockSink) ((Stage) stageMap.get(ServerConfigurationContext.BATCH_TRANSACTION_LOOKUP_STAGE))
        .getSink();
    assertFalse(batchTxnSink.queue.isEmpty());
    
    EventContext context = (EventContext) batchTxnSink.queue.remove(0);
    assertNotNull(context);
    
    // Look up shouldnt have happened yet
    args = (Object[]) objectManager.lookupObjectForCreateIfNecessaryContexts.poll(100);
    assertNull(args);
    
    // send another txn and see that Transaction Queue is NOT added again to the stage
    batch = new TestTransactionBatchReader();
    batch.channelID = new ChannelID(1);
    batch.batchID = new TxnBatchID(2);

    serverTransaction = new ServerTransactionImpl(batch.batchID, new TransactionID(2),
                                                                    new SequenceID(2), new LockID[0], batch.channelID,
                                                                    dnaList, new ObjectStringSerializer(), newRootsMap,
                                                                    TxnType.NORMAL, new LinkedList());
    completedTransactionIDs = new HashSet();
    for (int i = 11; i < 20; i++) {
      completedTransactionIDs.add(new GlobalTransactionID(i));
    }
    batch.acknowledged.addAll(completedTransactionIDs);
    batch.transactions.add(serverTransaction);

    batchReader.set(batch);

    // make sure our context queues are empty...
    assertTrue(transactionBatchManager.defineBatchContexts.isEmpty());
    assertTrue(objectManager.lookupObjectForCreateIfNecessaryContexts.isEmpty());
    // HANDLE EVENT
    objectManager.makePending = true;
    handler.handleEvent(null);
    assertTrue(batchTxnSink.queue.isEmpty());
    
  }

  private final class TestTransactionBatchReader implements TransactionBatchReader {

    public final Collection acknowledged = new HashSet();
    public TxnBatchID       batchID;
    public ChannelID        channelID;
    public final List       transactions = new LinkedList();
    int                     current      = 0;

    public ServerTransaction getNextTransaction() {
      return transactions.size() > current ? (ServerTransaction) transactions.get(current++) : null;
    }

    public void reset() {
      current = 0;
    }

    public TxnBatchID getBatchID() {
      return batchID;
    }

    public int getNumTxns() {
      return transactions.size();
    }

    public ChannelID getChannelID() {
      return channelID;
    }

    public Collection addAcknowledgedTransactionIDsTo(Collection c) {
      c.addAll(acknowledged);
      return c;
    }

  }

  private final class TestTransactionBatchReaderFactory implements TransactionBatchReaderFactory {

    public TransactionBatchReader newTransactionBatchReader(EventContext ctxt) {
      return (TransactionBatchReader) batchReader.get();
    }

  }

  private final class TestServerConfigurationContext implements ServerConfigurationContext {

    public ObjectManager getObjectManager() {
      return objectManager;
    }

    public LockManager getLockManager() {
      return null;
    }

    public DSOChannelManager getChannelManager() {
      return null;
    }

    public ClientStateManager getClientStateManager() {
      return null;
    }

    public ServerTransactionManager getTransactionManager() {
      return null;
    }

    public ManagedObjectStore getObjectStore() {
      return null;
    }

    public ServerClientHandshakeManager getClientHandshakeManager() {
      return null;
    }

    public ChannelStats getChannelStats() {
      return null;
    }

    public Stage getStage(String name) {
      return (Stage) stageMap.get(name);
    }

    public TCLogger getLogger(Class clazz) {
      return null;
    }

    public TransactionBatchReaderFactory getTransactionBatchReaderFactory() {
      return transactionBatchReaderFactory;
    }

    public ObjectRequestManager getObjectRequestManager() {
      throw new ImplementMe();
    }

    public BatchedTransactionProcessor getBatchedTransactionProcessor() {
      return batchTxnProcessor;
    }

  }
}
