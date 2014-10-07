/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionIDAlreadySetException;
import com.tc.object.locks.LockID;
import com.tc.object.locks.TestLockManager;
import com.tc.object.net.ChannelStats;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.impl.TestObjectManager;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeAction;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.metadata.NullMetaDataManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ServerTransactionManagerImplTest extends TestCase {

  private ServerTransactionManagerImpl       transactionManager;
  private TestTransactionAcknowledgeAction   transactionAcknowledgeAction;
  private TestClientStateManager             clientStateManager;
  private TestLockManager                    lockManager;
  private TestObjectManager                  objectManager;
  private Counter                            transactionRateCounter;
  private TestChannelStats                   channelStats;
  private TestGlobalTransactionManager       gtxm;
  private ObjectInstanceMonitor              imo;
  private ResentTransactionSequencer         resentTransactionSequencer;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.transactionAcknowledgeAction = new TestTransactionAcknowledgeAction();
    this.clientStateManager = new TestClientStateManager();
    this.lockManager = new TestLockManager();
    this.objectManager = new TestObjectManager();
    this.transactionRateCounter = new CounterImpl();
    this.channelStats = new TestChannelStats();
    this.gtxm = new TestGlobalTransactionManager();
    this.imo = new ObjectInstanceMonitorImpl();
    this.resentTransactionSequencer = mock(ResentTransactionSequencer.class);
    newTransactionManager();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private void newTransactionManager() {
    this.transactionManager = new ServerTransactionManagerImpl(this.gtxm, this.lockManager,
                                                               this.clientStateManager, this.objectManager,
                                                               new TestTransactionalObjectManager(), this.transactionAcknowledgeAction,
                                                               this.transactionRateCounter, this.channelStats,
                                                               new ServerTransactionManagerConfig(),
                                                               new ObjectStatsRecorder(), new NullMetaDataManager(),
                                                               resentTransactionSequencer);
    this.transactionManager.goToActiveMode();
    this.transactionManager.start(Collections.EMPTY_SET);
  }

  public void testCallbackOnResentTxnComplete() throws Exception {
    TxnsInSystemCompletionListener listener = mock(TxnsInSystemCompletionListener.class);
    transactionManager.callBackOnResentTxnsInSystemCompletion(listener);
    verify(resentTransactionSequencer).callBackOnResentTxnsInSystemCompletion(listener);
  }

  public void testRootCreatedEvent() {
    Map<String, ObjectID> roots = new HashMap<String, ObjectID>();
    roots.put("root", new ObjectID(1));

    // first test w/o any listeners attached
    this.transactionManager.commit(Collections.EMPTY_SET, roots, Collections.EMPTY_LIST);

    // add a listener
    Listener listener = new Listener();
    this.transactionManager.addRootListener(listener);
    roots.clear();
    roots.put("root2", new ObjectID(2));

    this.transactionManager.commit(Collections.EMPTY_SET, roots, Collections.EMPTY_LIST);
    assertEquals(1, listener.rootsCreated.size());
    Root root = (Root) listener.rootsCreated.remove(0);
    assertEquals("root2", root.name);
    assertEquals(new ObjectID(2), root.id);

    // add another listener
    Listener listener2 = new Listener();
    this.transactionManager.addRootListener(listener2);
    roots.clear();
    roots.put("root3", new ObjectID(3));

    this.transactionManager.commit(Collections.EMPTY_SET, roots, Collections.EMPTY_LIST);
    assertEquals(1, listener.rootsCreated.size());
    root = (Root) listener.rootsCreated.remove(0);
    assertEquals("root3", root.name);
    assertEquals(new ObjectID(3), root.id);
    root = (Root) listener2.rootsCreated.remove(0);
    assertEquals("root3", root.name);
    assertEquals(new ObjectID(3), root.id);

    // add a listener that throws an exception
    this.transactionManager.addRootListener(new ServerTransactionManagerEventListener() {
      @Override
      public void rootCreated(String name, ObjectID id) {
        throw new RuntimeException("This exception is supposed to be here");
      }
    });
    this.transactionManager.commit(Collections.EMPTY_SET, roots, Collections.EMPTY_LIST);
  }

  public void testAddAndRemoveTransactionListeners() throws Exception {
    TestServerTransactionListener l1 = new TestServerTransactionListener();
    TestServerTransactionListener l2 = new TestServerTransactionListener();
    this.transactionManager.addTransactionListener(l1);
    this.transactionManager.addTransactionListener(l2);

    Map<ServerTransactionID, ServerTransaction> txns = new HashMap<ServerTransactionID, ServerTransaction>();

    ClientID cid1 = new ClientID(1);
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;

    for (int i = 0; i < 10; i++) {
      TransactionID tid1 = new TransactionID(i);
      SequenceID sequenceID = new SequenceID(i);
      LockID[] lockIDs = new LockID[0];
      ServerTransaction tx = newServerTransactionImpl(new TxnBatchID(1), tid1, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList(),
                                                      DmiDescriptor.EMPTY_ARRAY, 1);
      txns.put(tx.getServerTransactionID(), tx);
    }
    doStages(cid1, txns, false);

    // check for events
    Object o[] = (Object[]) l1.incomingContext.take();
    assertNotNull(o);
    o = (Object[]) l2.incomingContext.take();
    assertNotNull(o);

    for (int i = 0; i < 10; i++) {
      ServerTransactionID tid1 = (ServerTransactionID) l1.appliedContext.take();
      ServerTransactionID tid2 = (ServerTransactionID) l2.appliedContext.take();
      assertEquals(tid1, tid2);
      // System.err.println("tid1 = " + tid1 + " tid2 = " + tid2 + " tids = " + tids);
      assertTrue(txns.containsKey(tid1));
      tid1 = (ServerTransactionID) l1.completedContext.take();
      tid2 = (ServerTransactionID) l2.completedContext.take();
      assertEquals(tid1, tid2);
      assertTrue(txns.containsKey(tid1));
    }

    // No more events
    o = (Object[]) l1.incomingContext.poll(2000);
    assertNull(o);
    o = (Object[]) l2.incomingContext.poll(2000);
    assertNull(o);
    ServerTransactionID tid = (ServerTransactionID) l1.appliedContext.poll(2000);
    assertNull(tid);
    tid = (ServerTransactionID) l2.appliedContext.poll(2000);
    assertNull(tid);
    tid = (ServerTransactionID) l1.completedContext.poll(2000);
    assertNull(tid);
    tid = (ServerTransactionID) l2.completedContext.poll(2000);
    assertNull(tid);

    // unregister one
    this.transactionManager.removeTransactionListener(l2);

    // more txn
    txns.clear();
    for (int i = 10; i < 20; i++) {
      TransactionID tid1 = new TransactionID(i);
      SequenceID sequenceID = new SequenceID(i);
      LockID[] lockIDs = new LockID[0];
      ServerTransaction tx = newServerTransactionImpl(new TxnBatchID(2), tid1, sequenceID, lockIDs, cid1, dnas,
                                                      serializer, newRoots, txnType, new LinkedList(),
                                                      DmiDescriptor.EMPTY_ARRAY, 1);
      txns.put(tx.getServerTransactionID(), tx);
    }
    doStages(cid1, txns, false);

    // Events to only l1
    o = (Object[]) l1.incomingContext.take();
    assertNotNull(o);
    o = (Object[]) l2.incomingContext.poll(2000);
    assertNull(o);

    for (int i = 0; i < 10; i++) {
      ServerTransactionID tid1 = (ServerTransactionID) l1.appliedContext.take();
      ServerTransactionID tid2 = (ServerTransactionID) l2.appliedContext.poll(1000);
      assertNotNull(tid1);
      assertNull(tid2);
      assertTrue(txns.containsKey(tid1));
      tid1 = (ServerTransactionID) l1.completedContext.take();
      tid2 = (ServerTransactionID) l2.completedContext.poll(1000);
      assertNotNull(tid1);
      assertNull(tid2);
      assertTrue(txns.containsKey(tid1));
    }
  }

  /**
   * A transaction is broadcasted to another client, the orginating client disconnects and then the broadcasted client
   * disconnects. This test was written to illustrate a scenario where when multiple clients were disconnecting, were
   * acks are being waited for, a concurrent modification exception was thrown.
   */
  public void test2ClientsDisconnectAtTheSameTime() throws Exception {
    ClientID cid1 = new ClientID(1);
    TransactionID tid1 = new TransactionID(1);
    TransactionID tid2 = new TransactionID(2);
    TransactionID tid3 = new TransactionID(3);
    ClientID cid2 = new ClientID(2);
    ClientID cid3 = new ClientID(3);
    ClientID cid4 = new ClientID(4);
    ClientID cid5 = new ClientID(5);

    LockID[] lockIDs = new LockID[0];
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx1 = newServerTransactionImpl(new TxnBatchID(1), tid1, sequenceID, lockIDs, cid1, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);

    Map<ServerTransactionID, ServerTransaction> txns = new HashMap<ServerTransactionID, ServerTransaction>();
    txns.put(tx1.getServerTransactionID(), tx1);
    this.transactionManager.incomingTransactions(cid1, txns);
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid2);
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid3);
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid4);
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid5);
    doStages(cid1, txns, true);

    // Adding a few more transactions to that Transaction Records are created for everybody
    txns.clear();
    ServerTransaction tx2 = newServerTransactionImpl(new TxnBatchID(2), tid2, sequenceID, lockIDs, cid2, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);
    txns.put(tx2.getServerTransactionID(), tx2);
    this.transactionManager.incomingTransactions(cid2, txns);

    this.transactionManager.acknowledgement(cid2, tid2, cid3);
    doStages(cid2, txns, true);

    txns.clear();
    ServerTransaction tx3 = newServerTransactionImpl(new TxnBatchID(2), tid3, sequenceID, lockIDs, cid3, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);
    txns.put(tx3.getServerTransactionID(), tx3);
    this.transactionManager.incomingTransactions(cid3, txns);

    this.transactionManager.acknowledgement(cid3, tid3, cid4);
    this.transactionManager.acknowledgement(cid3, tid3, cid2);
    doStages(cid2, txns, true);

    assertTrue(this.transactionManager.isWaiting(cid1, tid1));

    this.transactionManager.acknowledgement(cid1, tid1, cid3);
    assertTrue(this.transactionManager.isWaiting(cid1, tid1));
    this.transactionManager.acknowledgement(cid1, tid1, cid4);
    assertTrue(this.transactionManager.isWaiting(cid1, tid1));
    this.transactionManager.acknowledgement(cid1, tid1, cid5);
    assertTrue(this.transactionManager.isWaiting(cid1, tid1));

    // Client 1 disconnects
    this.transactionManager.shutdownNode(cid1);

    // Still waiting for tx1
    assertTrue(this.transactionManager.isWaiting(cid1, tid1));

    // Client 2 disconnects now
    // Concurrent Modification exception used to be thrown here.
    this.transactionManager.shutdownNode(cid2);

    // Not waiting for tx1 anymore
    assertFalse(this.transactionManager.isWaiting(cid1, tid1));

    // Client 3 disconnects now
    // Concurrent Modification exception used to be thrown here.
    this.transactionManager.shutdownNode(cid2);




  }

  public void test1ClientDisconnectWithWaiteeAsSameClient() throws Exception {
    ClientID cid1 = new ClientID(1);
    TransactionID tid1 = new TransactionID(1);

    LockID[] lockIDs = new LockID[0];
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx1 = newServerTransactionImpl(new TxnBatchID(1), tid1, sequenceID, lockIDs, cid1, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);

    Map<ServerTransactionID, ServerTransaction> txns = new HashMap<ServerTransactionID, ServerTransaction>();

    txns.put(tx1.getServerTransactionID(), tx1);
    this.transactionManager.incomingTransactions(cid1, txns);
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid1);
    doStages(cid1, txns, true);

    assertTrue(this.transactionManager.isWaiting(cid1, tid1));

    // Client 1 disconnects
    this.transactionManager.shutdownNode(cid1);

    // Still waiting for tx1
    assertFalse(this.transactionManager.isWaiting(cid1, tid1));
  }

  public void testPauseUnpauseTransactions() throws Exception {
    final ClientID clientID1 = new ClientID(1);
    final TransactionID tid1 = new TransactionID(1);
    LockID[] lockIDs = new LockID[0];
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx1 = newServerTransactionImpl(new TxnBatchID(1), tid1, sequenceID, lockIDs, clientID1, dnas,
        serializer, newRoots, txnType, new LinkedList(),
        DmiDescriptor.EMPTY_ARRAY, 1);

    final Map<ServerTransactionID, ServerTransaction> txns = new HashMap<ServerTransactionID, ServerTransaction>();
    txns.put(tx1.getServerTransactionID(), tx1);

    ExecutorService executorService = Executors.newFixedThreadPool(1);

    Callable addIncomingTxnRunnable = new Callable() {
      @Override
      public Object call() throws Exception {
        transactionManager.incomingTransactions(clientID1, txns);
        transactionManager.transactionsRelayed(clientID1, txns.keySet());
        return true;
      }
    };

    transactionManager.pauseTransactions();
    Future addIncomingTxnFuture =  executorService.submit(addIncomingTxnRunnable);

    assertFalse(this.transactionManager.isWaiting(clientID1, tid1));
    assertTrue(this.transactionAcknowledgeAction.clientID == null && this.transactionAcknowledgeAction.txID == null);

    transactionManager.unPauseTransactions();

    addIncomingTxnFuture.get();
    doStages(clientID1, txns);
    assertTrue(this.transactionAcknowledgeAction.clientID == clientID1 && this.transactionAcknowledgeAction.txID == tid1);

    executorService.shutdown();
    executorService.awaitTermination(2, TimeUnit.MINUTES);
  }

  public void tests() throws Exception {
    ClientID cid1 = new ClientID(1);
    TransactionID tid1 = new TransactionID(1);
    TransactionID tid2 = new TransactionID(2);
    TransactionID tid3 = new TransactionID(3);
    TransactionID tid4 = new TransactionID(4);
    TransactionID tid5 = new TransactionID(5);
    TransactionID tid6 = new TransactionID(6);

    ClientID cid2 = new ClientID(2);
    ClientID cid3 = new ClientID(3);

    LockID[] lockIDs = new LockID[0];
    List dnas = Collections.unmodifiableList(new LinkedList());
    ObjectStringSerializer serializer = null;
    Map newRoots = Collections.unmodifiableMap(new HashMap());
    TxnType txnType = TxnType.NORMAL;
    SequenceID sequenceID = new SequenceID(1);
    ServerTransaction tx1 = newServerTransactionImpl(new TxnBatchID(1), tid1, sequenceID, lockIDs, cid1, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);

    // Test with one waiter
    Map<ServerTransactionID, ServerTransaction> txns = new HashMap<ServerTransactionID, ServerTransaction>();
    txns.put(tx1.getServerTransactionID(), tx1);
    this.transactionManager.incomingTransactions(cid1, txns);
    transactionManager.transactionsRelayed(cid1, txns.keySet());
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid1, cid2);
    assertTrue(this.transactionManager.isWaiting(cid1, tid1));
    assertTrue(this.transactionAcknowledgeAction.clientID == null && this.transactionAcknowledgeAction.txID == null);
    this.transactionManager.acknowledgement(cid1, tid1, cid2);
    assertTrue(this.transactionAcknowledgeAction.clientID == null && this.transactionAcknowledgeAction.txID == null);
    doStages(cid1, txns);
    assertTrue(this.transactionAcknowledgeAction.clientID == cid1 && this.transactionAcknowledgeAction.txID == tid1);
    assertFalse(this.transactionManager.isWaiting(cid1, tid1));

    // Test with 2 waiters
    this.transactionAcknowledgeAction.clear();
    this.gtxm.clear();
    txns.clear();
    sequenceID = new SequenceID(2);
    ServerTransaction tx2 = newServerTransactionImpl(new TxnBatchID(2), tid2, sequenceID, lockIDs, cid1, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);
    txns.put(tx2.getServerTransactionID(), tx2);
    this.transactionManager.incomingTransactions(cid1, txns);
    transactionManager.transactionsRelayed(cid1, txns.keySet());
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid2, cid2);
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid2, cid3);
    assertTrue(this.transactionAcknowledgeAction.clientID == null && this.transactionAcknowledgeAction.txID == null);
    assertTrue(this.transactionManager.isWaiting(cid1, tid2));
    this.transactionManager.acknowledgement(cid1, tid2, cid2);
    assertTrue(this.transactionAcknowledgeAction.clientID == null && this.transactionAcknowledgeAction.txID == null);
    assertTrue(this.transactionManager.isWaiting(cid1, tid2));
    this.transactionManager.acknowledgement(cid1, tid2, cid3);
    assertTrue(this.transactionAcknowledgeAction.clientID == null && this.transactionAcknowledgeAction.txID == null);
    doStages(cid1, txns);
    assertTrue(this.transactionAcknowledgeAction.clientID == cid1 && this.transactionAcknowledgeAction.txID == tid2);
    assertFalse(this.transactionManager.isWaiting(cid1, tid2));

    // Test shutdown client with 2 waiters
    this.transactionAcknowledgeAction.clear();
    this.gtxm.clear();
    txns.clear();
    sequenceID = new SequenceID(3);
    ServerTransaction tx3 = newServerTransactionImpl(new TxnBatchID(3), tid3, sequenceID, lockIDs, cid1, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);
    txns.put(tx3.getServerTransactionID(), tx3);
    this.transactionManager.incomingTransactions(cid1, txns);
    transactionManager.transactionsRelayed(cid1, txns.keySet());
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid3, cid2);
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid3, cid3);
    assertTrue(this.transactionAcknowledgeAction.clientID == null && this.transactionAcknowledgeAction.txID == null);
    assertTrue(this.transactionManager.isWaiting(cid1, tid3));
    this.transactionManager.shutdownNode(cid3);
    assertEquals(cid3, this.clientStateManager.shutdownClient);
    assertTrue(this.transactionManager.isWaiting(cid1, tid3));
    this.transactionManager.acknowledgement(cid1, tid3, cid2);
    doStages(cid1, txns);
    assertTrue(this.transactionAcknowledgeAction.clientID == cid1 && this.transactionAcknowledgeAction.txID == tid3);
    assertFalse(this.transactionManager.isWaiting(cid1, tid3));

    // Test shutdown client that no one is waiting for
    this.transactionAcknowledgeAction.clear();
    this.gtxm.clear();
    txns.clear();
    this.clientStateManager.shutdownClient = null;

    sequenceID = new SequenceID(4);
    ServerTransaction tx4 = newServerTransactionImpl(new TxnBatchID(4), tid4, sequenceID, lockIDs, cid1, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);
    txns.put(tx4.getServerTransactionID(), tx4);
    this.transactionManager.incomingTransactions(cid1, txns);
    transactionManager.transactionsRelayed(cid1, txns.keySet());
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid4, cid2);
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid4, cid3);
    this.transactionManager.shutdownNode(cid1);
    assertTrue(this.transactionAcknowledgeAction.clientID == null && this.transactionAcknowledgeAction.txID == null);
    // It should still be waiting, since we only do cleans ups on completion of all transactions.
    assertNull(this.clientStateManager.shutdownClient);
    assertTrue(this.transactionManager.isWaiting(cid1, tid4));

    // adding new transactions should throw an error
    boolean failed = false;
    try {
      this.transactionManager.incomingTransactions(cid1, txns);
      failed = true;
    } catch (Throwable t) {
      // failed as expected.
    }
    if (failed) {
      //
      throw new Exception("Calling incomingTransaction after client shutdown didnt throw an error as excepted!!! ;(");
    }
    this.transactionManager.acknowledgement(cid1, tid4, cid2);
    assertTrue(this.transactionManager.isWaiting(cid1, tid4));
    this.transactionManager.acknowledgement(cid1, tid4, cid3);
    assertFalse(this.transactionManager.isWaiting(cid1, tid4));

    // shutdown is not called yet since apply commit and broadcast need to complete.
    assertNull(this.clientStateManager.shutdownClient);
    List serverTids = new ArrayList();
    serverTids.add(new ServerTransactionID(cid1, tid4));
    this.transactionManager.commit(Collections.EMPTY_SET, Collections.EMPTY_MAP, serverTids);
    assertNull(this.clientStateManager.shutdownClient);
    this.transactionManager.broadcasted(cid1, tid4);
    assertEquals(cid1, this.clientStateManager.shutdownClient);

    // Test with 2 waiters on different tx's
    this.transactionAcknowledgeAction.clear();
    this.gtxm.clear();
    txns.clear();
    sequenceID = new SequenceID(5);
    ServerTransaction tx5 = newServerTransactionImpl(new TxnBatchID(5), tid5, sequenceID, lockIDs, cid1, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);
    sequenceID = new SequenceID(6);
    ServerTransaction tx6 = newServerTransactionImpl(new TxnBatchID(5), tid6, sequenceID, lockIDs, cid1, dnas,
                                                     serializer, newRoots, txnType, new LinkedList(),
                                                     DmiDescriptor.EMPTY_ARRAY, 1);
    txns.put(tx5.getServerTransactionID(), tx5);
    txns.put(tx6.getServerTransactionID(), tx6);

    this.transactionManager.incomingTransactions(cid1, txns);
    transactionManager.transactionsRelayed(cid1, txns.keySet());
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid5, cid2);
    this.transactionManager.addWaitingForAcknowledgement(cid1, tid6, cid2);

    assertTrue(this.transactionAcknowledgeAction.clientID == null && this.transactionAcknowledgeAction.txID == null);
    assertTrue(this.transactionManager.isWaiting(cid1, tid5));
    assertTrue(this.transactionManager.isWaiting(cid1, tid6));

    this.transactionManager.acknowledgement(cid1, tid5, cid2);
    assertFalse(this.transactionManager.isWaiting(cid1, tid5));
    assertTrue(this.transactionManager.isWaiting(cid1, tid6));
    doStages(cid1, txns);
    assertTrue(this.transactionAcknowledgeAction.clientID == cid1 && this.transactionAcknowledgeAction.txID == tid5);

  }

  private ServerTransaction newServerTransactionImpl(TxnBatchID txnBatchID, TransactionID tid, SequenceID sequenceID,
                                                     LockID[] lockIDs, ClientID cid, List dnas,
                                                     ObjectStringSerializer serializer, Map newRoots, TxnType txnType,
                                                     Collection notifies, DmiDescriptor[] dmis, int numAppTxns) {
    ServerTransaction txn = new ServerTransactionImpl(txnBatchID, tid, sequenceID, lockIDs, cid, dnas, serializer,
                                                      newRoots, txnType, notifies, dmis, new MetaDataReader[0],
                                                      numAppTxns, new long[0]);
    try {
      txn.setGlobalTransactionID(this.gtxm.getOrCreateGlobalTransactionID(txn.getServerTransactionID()));
    } catch (GlobalTransactionIDAlreadySetException e) {
      throw new AssertionError(e);
    }
    return txn;
  }

  private void doStages(ClientID cid1, Map txns) {
    doStages(cid1, txns, true);
  }

  private void doStages(ClientID cid1, Map<ServerTransactionID, ServerTransaction> txns, boolean skipIncoming) {

    // process stage
    if (!skipIncoming) {
      this.transactionManager.incomingTransactions(cid1, txns);
      transactionManager.transactionsRelayed(cid1, txns.keySet());
    }

    for (ServerTransaction tx : txns.values()) {
      // apply stage
      this.transactionManager.apply(tx, Collections.EMPTY_MAP, new ApplyTransactionInfo(), this.imo);

      // commit stage
      Set committedIDs = new HashSet();
      committedIDs.add(tx.getServerTransactionID());
      this.transactionManager.commit(Collections.EMPTY_SET, Collections.EMPTY_MAP, committedIDs);

      // broadcast stage
      this.transactionManager.broadcasted(tx.getSourceID(), tx.getTransactionID());
    }
  }

  private static final class TestChannelStats implements ChannelStats {

    public LinkedQueue notifyTransactionContexts = new LinkedQueue();

    @Override
    public Counter getCounter(MessageChannel channel, String name) {
      throw new ImplementMe();
    }

    @Override
    public void notifyTransaction(NodeID nodeID, int numTxns) {
      try {
        this.notifyTransactionContexts.put(nodeID);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    @Override
    public void notifyReadOperations(MessageChannel channel, int numObjectsRequested) {
      throw new ImplementMe();
    }

    @Override
    public void notifyTransactionAckedFrom(NodeID nodeID) {
      // NOP
    }

    @Override
    public void notifyTransactionBroadcastedTo(NodeID nodeID) {
      // NOP
    }

    @Override
    public void notifyServerMapRequest(ServerMapRequestType type, MessageChannel channel, int numRequests) {
      // NOP
    }

  }

  private static class Root {
    final String   name;
    final ObjectID id;

    Root(String name, ObjectID id) {
      this.name = name;
      this.id = id;
    }
  }

  private static class Listener implements ServerTransactionManagerEventListener {
    final List rootsCreated = new ArrayList();

    @Override
    public void rootCreated(String name, ObjectID id) {
      this.rootsCreated.add(new Root(name, id));
    }
  }

  private static class TestServerTransactionListener extends AbstractServerTransactionListener {

    NoExceptionLinkedQueue incomingContext  = new NoExceptionLinkedQueue();
    NoExceptionLinkedQueue appliedContext   = new NoExceptionLinkedQueue();
    NoExceptionLinkedQueue completedContext = new NoExceptionLinkedQueue();

    @Override
    public void incomingTransactions(NodeID source, Set serverTxnIDs) {
      this.incomingContext.put(new Object[] { source, serverTxnIDs });
    }

    @Override
    public void transactionApplied(ServerTransactionID stxID, ObjectIDSet newObjectsCreated) {
      this.appliedContext.put(stxID);
    }

    @Override
    public void transactionCompleted(ServerTransactionID stxID) {
      this.completedContext.put(stxID);
    }
  }

  public static class TestTransactionAcknowledgeAction implements TransactionAcknowledgeAction {
    public NodeID        clientID;
    public TransactionID txID;

    @Override
    public void acknowledgeTransaction(ServerTransactionID stxID) {
      this.txID = stxID.getClientTransactionID();
      this.clientID = stxID.getSourceID();
    }

    public void clear() {
      this.txID = null;
      this.clientID = null;
    }

  }

  private class TestTransactionalObjectManager extends NullTransactionalObjectManager {
    @Override
    public void addTransactions(Collection<ServerTransaction> txns) {
      Set<TransactionLookupContext> txnContexts = new HashSet<TransactionLookupContext>();
      for (ServerTransaction txn : txns) {
        txnContexts.add(new TransactionLookupContext(txn, false));
        transactionManager.processMetaData(txn, new ApplyTransactionInfo());
      }
    }
  }
}
