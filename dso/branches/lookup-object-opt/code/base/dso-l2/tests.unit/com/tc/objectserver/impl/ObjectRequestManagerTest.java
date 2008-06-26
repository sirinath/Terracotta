/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.apache.commons.lang.NotImplementedException;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.NullObjectInstanceMonitor;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.RespondToObjectRequestContext;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.core.api.TestDNACursor;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.BatchAndSend;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.LookupContext;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.ObjectRequestCache;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.ResponseContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProviderImpl;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionLister;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

public class ObjectRequestManagerTest extends TestCase {

  private ObjectRequestCache cache       = null;

  private Set                objectIDSet = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ManagedObjectStateFactory.disableSingleton(true);
    SleepycatPersistor persistor = new SleepycatPersistor(TCLogging.getLogger(ObjectRequestManagerTest.class),
                                                          new DBEnvironment(true, new File(".")),
                                                          new CustomSerializationAdapterFactory());

    ManagedObjectChangeListenerProviderImpl moclp = new ManagedObjectChangeListenerProviderImpl();
    moclp.setListener(new ManagedObjectChangeListener() {

      public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
        // NOP
      }

    });

    ManagedObjectStateFactory factory = ManagedObjectStateFactory.createInstance(moclp, persistor);
    ManagedObjectStateFactory.setInstance(factory);

    TestRequestManagedObjectResponseMessage.sendSet = new TreeSet();
    TestObjectsNotFoundMessage.sendSet = new TreeSet();

  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testMultipleRequestObjects() {
    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestServerTransactionManager serverTransactionManager = new TestServerTransactionManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(requestSink, objectManager,
                                                                                 channelManager, clientStateManager,
                                                                                 serverTransactionManager, respondSink);

    Set ids = createObjectIDSet();
    objectRequestManager.transactionManagerStarted(new HashSet());

    List objectRequestThreadList = new ArrayList();
    int numberOfRequestThreads = 10;
    CyclicBarrier requestBarrier = new CyclicBarrier(numberOfRequestThreads);

    for (int i = 0; i < numberOfRequestThreads; i++) {
      ClientID clientID = new ClientID(new ChannelID(i));
      objectRequestManager.clearAllTransactionsFor(clientID);
      ObjectRequestThread objectRequestThread = new ObjectRequestThread(requestBarrier, objectRequestManager, clientID,
                                                                        new ObjectRequestID(i), ids, false);
      objectRequestThreadList.add(objectRequestThread);
    }

    // let's now start until all the request threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    // assert that there is only one request in the sink.
    assertEquals(respondSink.size(), 1);

    RespondToObjectRequestContext respondToObjectRequestContext = null;

    try {
      respondToObjectRequestContext = (RespondToObjectRequestContext) respondSink.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
    System.out.println("respond: " + respondToObjectRequestContext);
    assertNotNull(respondToObjectRequestContext);
    assertEquals(ids.size(), respondToObjectRequestContext.getRequestedObjectIDs().size());
    assertEquals(false, respondToObjectRequestContext.isServerInitiated());
    assertEquals(0, respondToObjectRequestContext.getMissingObjectIDs().size());
    assertEquals(ids.size(), respondToObjectRequestContext.getObjs().size());

  }
  
  public void testCreateAndAddManagedObjectRequestContextsTo() {
    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestServerTransactionManager serverTransactionManager = new TestServerTransactionManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(requestSink, objectManager,
                                                                                 channelManager, clientStateManager,
                                                                                 serverTransactionManager, respondSink);
    ClientID clientID = new ClientID(new ChannelID(1));
    ObjectRequestID requestID = new ObjectRequestID(1);
    Set ids = createObjectIDSet();
    objectRequestManager.transactionManagerStarted(new HashSet());
    objectRequestManager.clearAllTransactionsFor(clientID);

    objectRequestManager.createAndAddManagedObjectRequestContextsTo(clientID, requestID, ids, -1, false, Thread.currentThread().getName());
    
    assertEquals(2, requestSink.size());
    
    for(int i =0; i < 2; i++) {
      LookupContext lookupContext;
      try {
        lookupContext = (LookupContext)requestSink.take();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      assertEquals(lookupContext.getLookupIDs().size(), ids.size());
      assertEquals(-1, lookupContext.getMaxRequestDepth());
      assertEquals(clientID, lookupContext.getRequestedNodeID());
      assertEquals(requestID, lookupContext.getRequestID());
      assertEquals(true, lookupContext.isServerInitiated());
    }

  }

  public void testMultipleRequestResponseObjects() {
    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestServerTransactionManager serverTransactionManager = new TestServerTransactionManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(requestSink, objectManager,
                                                                                 channelManager, clientStateManager,
                                                                                 serverTransactionManager, respondSink);

    Set ids = createObjectIDSet();
    objectRequestManager.transactionManagerStarted(new HashSet());

    List objectRequestThreadList = new ArrayList();
    int numberOfRequestThreads = 10;
    CyclicBarrier requestBarrier = new CyclicBarrier(numberOfRequestThreads);

    for (int i = 0; i < numberOfRequestThreads; i++) {
      ClientID clientID = new ClientID(new ChannelID(i));
      objectRequestManager.clearAllTransactionsFor(clientID);
      ObjectRequestThread objectRequestThread = new ObjectRequestThread(requestBarrier, objectRequestManager, clientID,
                                                                        new ObjectRequestID(i), ids, false);
      objectRequestThreadList.add(objectRequestThread);
    }

    // let's now start until all the request threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    System.out.println("done doing requests.");
    assertEquals(respondSink.size(), 1);
    assertEquals(objectRequestManager.getObjectRequestCacheSize(), 100);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), 10);

    List objectResponseThreadList = new ArrayList();
    int numberOfResponseThreads = 1;
    CyclicBarrier responseBarrier = new CyclicBarrier(numberOfResponseThreads);

    for (int i = 0; i < numberOfResponseThreads; i++) {
      ClientID clientID = new ClientID(new ChannelID(i));
      objectRequestManager.clearAllTransactionsFor(clientID);
      ObjectResponseThread objectResponseThread = new ObjectResponseThread(responseBarrier, objectRequestManager,
                                                                           respondSink);
      objectResponseThreadList.add(objectResponseThread);
    }

    // let's now start until all the response threads
    for (Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    Set sendSet = TestRequestManagedObjectResponseMessage.sendSet;
    assertEquals(sendSet.size(), 10);

    int i = 0;
    for (Iterator iter = sendSet.iterator(); iter.hasNext(); i++) {
      TestRequestManagedObjectResponseMessage message = (TestRequestManagedObjectResponseMessage) iter.next();
      System.out.println("ChannelID: " + message.getChannelID().toLong());
      assertEquals(message.getChannelID().toLong(), i);

    }

    assertEquals(objectRequestManager.getObjectRequestCacheSize(), 0);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), 0);

  }

  public void testMissingObjects() {

    TestObjectManager objectManager = new TestObjectManager() {

      public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext,
                                                   int maxCount) {

        Set ids = responseContext.getLookupIDs();
        Map resultsMap = new HashMap();
        for (Iterator iter = ids.iterator(); iter.hasNext();) {
          ObjectID id = (ObjectID) iter.next();
          responseContext.missingObject(id);
        }

        ObjectManagerLookupResults results = new ObjectManagerLookupResultsImpl(resultsMap, new HashSet());
        responseContext.setResults(results);

        return false;
      }
    };
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestServerTransactionManager serverTransactionManager = new TestServerTransactionManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(requestSink, objectManager,
                                                                                 channelManager, clientStateManager,
                                                                                 serverTransactionManager, respondSink);

    Set ids = createObjectIDSet();
    objectRequestManager.transactionManagerStarted(new HashSet());

    List objectRequestThreadList = new ArrayList();
    int numberOfRequestThreads = 10;
    CyclicBarrier requestBarrier = new CyclicBarrier(numberOfRequestThreads);

    for (int i = 0; i < numberOfRequestThreads; i++) {
      ClientID clientID = new ClientID(new ChannelID(i));
      objectRequestManager.clearAllTransactionsFor(clientID);
      ObjectRequestThread objectRequestThread = new ObjectRequestThread(requestBarrier, objectRequestManager, clientID,
                                                                        new ObjectRequestID(i), ids, false);
      objectRequestThreadList.add(objectRequestThread);
    }

    // let's now start until all the request threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    System.out.println("done doing requests.");
    assertEquals(respondSink.size(), 1);
    assertEquals(objectRequestManager.getObjectRequestCacheSize(), 100);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), 10);

    List objectResponseThreadList = new ArrayList();
    int numberOfResponseThreads = 1;
    CyclicBarrier responseBarrier = new CyclicBarrier(numberOfResponseThreads);

    for (int i = 0; i < numberOfResponseThreads; i++) {
      ClientID clientID = new ClientID(new ChannelID(i));
      objectRequestManager.clearAllTransactionsFor(clientID);
      ObjectResponseThread objectResponseThread = new ObjectResponseThread(responseBarrier, objectRequestManager,
                                                                           respondSink);
      objectResponseThreadList.add(objectResponseThread);
    }

    // let's now start until all the response threads
    for (Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    Set sendSet = TestObjectsNotFoundMessage.sendSet;
    assertEquals(sendSet.size(), 10);

    int i = 0;
    for (Iterator iter = sendSet.iterator(); iter.hasNext(); i++) {
      TestObjectsNotFoundMessage message = (TestObjectsNotFoundMessage) iter.next();
      System.out.println("ChannelID: " + message.getChannelID().toLong());
      assertEquals(message.getChannelID().toLong(), i);

    }

    assertEquals(objectRequestManager.getObjectRequestCacheSize(), 0);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), 0);

  }

  public void testBatchAndSend() {

    TestMessageChannel messageChannel = new TestMessageChannel(new ChannelID(1));
    Sequence batchIDSequence = new SimpleSequence();
    BatchAndSend batchAndSend = new BatchAndSend(messageChannel, batchIDSequence.next());

    // let's test send objects
    for (int i = 0; i < 5000; i++) {
      ObjectID id = new ObjectID(i);
      ManagedObjectImpl mo = new ManagedObjectImpl(id);
      mo.apply(new TestDNA(new TestDNACursor()), new TransactionID(id.toLong()), new BackReferences(),
               new NullObjectInstanceMonitor(), true);
      batchAndSend.sendObject(mo, i < 5000);

    }

    // let's test missing objects

  }

  public void testRequestObjects() {

    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestServerTransactionManager serverTransactionManager = new TestServerTransactionManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(requestSink, objectManager,
                                                                                 channelManager, clientStateManager,
                                                                                 serverTransactionManager, respondSink);
    ClientID clientID = new ClientID(new ChannelID(1));
    ObjectRequestID requestID = new ObjectRequestID(1);
    Set ids = createObjectIDSet();
    objectRequestManager.transactionManagerStarted(new HashSet());
    objectRequestManager.clearAllTransactionsFor(clientID);

    objectRequestManager.requestObjects(clientID, requestID, ids, -1, false, Thread.currentThread().getName());

    RespondToObjectRequestContext respondToObjectRequestContext = null;
    try {
      respondToObjectRequestContext = (RespondToObjectRequestContext) respondSink.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    assertNotNull(respondToObjectRequestContext);
    assertEquals(clientID, respondToObjectRequestContext.getRequestedNodeID());
    assertEquals(ids.size(), respondToObjectRequestContext.getRequestedObjectIDs().size());
    assertEquals(false, respondToObjectRequestContext.isServerInitiated());
    assertEquals(0, respondToObjectRequestContext.getMissingObjectIDs().size());
    assertEquals(ids.size(), respondToObjectRequestContext.getObjs().size());
  }

  public void testResponseObjects() {

    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestServerTransactionManager serverTransactionManager = new TestServerTransactionManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(requestSink, objectManager,
                                                                                 channelManager, clientStateManager,
                                                                                 serverTransactionManager, respondSink);
    ClientID clientID = new ClientID(new ChannelID(1));
    ObjectRequestID requestID = new ObjectRequestID(1);
    Set ids = createObjectIDSet();
    objectRequestManager.transactionManagerStarted(new HashSet());
    objectRequestManager.clearAllTransactionsFor(clientID);

    objectRequestManager.requestObjects(clientID, requestID, ids, -1, false, Thread.currentThread().getName());

    RespondToObjectRequestContext respondToObjectRequestContext = null;
    try {
      respondToObjectRequestContext = (RespondToObjectRequestContext) respondSink.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    objectRequestManager.sendObjects(respondToObjectRequestContext.getRequestedNodeID(), respondToObjectRequestContext
        .getObjs(), respondToObjectRequestContext.getRequestedObjectIDs(), respondToObjectRequestContext
        .getMissingObjectIDs(), respondToObjectRequestContext.isServerInitiated());

  }

  public void testContexts() {
    ObjectRequestManager orm = new TestObjectRequestManager();
    ClientID clientID = new ClientID(new ChannelID(1));
    ObjectRequestID objectRequestID = new ObjectRequestID(1);
    Set ids = createObjectIDSet();
    Set missingIds = new HashSet();
    Sink sink = new TestSink();
    Collection objs = null;

    LookupContext lookupContext = new LookupContext(orm, clientID, objectRequestID, ids, 0, "Thread-1", false, sink);
    assertEquals(lookupContext.getLookupIDs().size(), ids.size());
    assertEquals(0, lookupContext.getMaxRequestDepth());
    assertEquals(clientID, lookupContext.getRequestedNodeID());
    assertEquals(objectRequestID, lookupContext.getRequestID());
    assertEquals("Thread-1", lookupContext.getRequestingThreadName());
    assertEquals(false, lookupContext.isServerInitiated());

    ResponseContext responseContext = new ResponseContext(clientID, objs, ids, missingIds, false);
    assertEquals(clientID, responseContext.getRequestedNodeID());
  }

  public void testObjectRequestCache() {

    cache = new ObjectRequestCache();
    objectIDSet = createObjectIDSet();
    CacheThread cacheThread1 = new CacheThread(1, cache, objectIDSet);
    CacheThread cacheThread2 = new CacheThread(2, cache, objectIDSet);
    cacheThread1.start();
    cacheThread2.start();

    try {
      cacheThread1.join();

      cacheThread2.join();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    synchronized (cache) {
      assertEquals(0, cache.ids(new ClientID(new ChannelID(1))).size());
      assertEquals(0, cache.ids(new ClientID(new ChannelID(2))).size());

    }

  }

  private Set createObjectIDSet() {
    Set set = new HashSet();
    for (int i = 0; i < 100; i++) {
      set.add(new ObjectID(i));
    }
    return set;
  }

  private static class ObjectRequestThread extends Thread {

    private ObjectRequestManager objectRequestManager;
    private ClientID             clientID;
    private ObjectRequestID      requestID;
    private Set                  ids;
    private boolean              serverInitiated;
    private CyclicBarrier        barrier;

    public ObjectRequestThread(CyclicBarrier barrier, ObjectRequestManager objectRequestManager, ClientID clientID,
                               ObjectRequestID requestID, Set ids, boolean serverInitiated) {
      this.objectRequestManager = objectRequestManager;
      this.clientID = clientID;
      this.requestID = requestID;
      this.ids = ids;
      this.serverInitiated = serverInitiated;
      this.barrier = barrier;
    }

    @Override
    public void run() {
      try {
        barrier.barrier();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
      objectRequestManager.requestObjects(clientID, requestID, ids, -1, serverInitiated, Thread.currentThread()
          .getName());
    }

  }

  private static class ObjectResponseThread extends Thread {

    private ObjectRequestManager objectRequestManager;
    private TestSink             sink;
    private CyclicBarrier        barrier;

    public ObjectResponseThread(CyclicBarrier barrier, ObjectRequestManager objectRequestManager, TestSink sink) {
      this.objectRequestManager = objectRequestManager;
      this.sink = sink;
      this.barrier = barrier;
    }

    @Override
    public void run() {
      try {
        barrier.barrier();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
      RespondToObjectRequestContext respondToObjectRequestContext = null;
      try {
        respondToObjectRequestContext = (RespondToObjectRequestContext) sink.take();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      synchronized (this) {
        System.out.println("in the reponse thread: " + respondToObjectRequestContext);
        objectRequestManager.sendObjects(respondToObjectRequestContext.getRequestedNodeID(),
                                         respondToObjectRequestContext.getObjs(), respondToObjectRequestContext
                                             .getRequestedObjectIDs(), respondToObjectRequestContext
                                             .getMissingObjectIDs(), respondToObjectRequestContext.isServerInitiated());
        if (TestRequestManagedObjectResponseMessage.sendSet.size() == 1) {
          TestRequestManagedObjectResponseMessage message = (TestRequestManagedObjectResponseMessage) TestRequestManagedObjectResponseMessage.sendSet
              .iterator().next();
          assertEquals(respondToObjectRequestContext.getObjs().size(), message.getObjects().size());
        } else {
          new AssertionError("should have one sent message in the queue");
        }
      }
    }

  }

  private static class CacheThread extends Thread {

    private ObjectRequestCache cache = null;

    private Set                set   = null;

    private long               channnelID;

    public CacheThread(long channelID, ObjectRequestCache cache, Set set) {
      this.channnelID = channelID;
      this.cache = cache;
      this.set = set;
    }

    @Override
    public void run() {
      ClientID clientID = new ClientID(new ChannelID(channnelID));
      for (int j = 0; j < 10; j++) {
        synchronized (cache) {
          for (Iterator i = set.iterator(); i.hasNext();) {
            ObjectID id = (ObjectID) i.next();
            cache.add(clientID, id);
          }

          cache.remove(set);
        }
      }
    }
  }

  private static class TestServerTransactionManager implements ServerTransactionManager {

    protected List listeners = new ArrayList();

    protected List getListeners() {
      return listeners;
    }

    public void acknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void addTransactionListener(ServerTransactionListener listener) {
      listeners.add(listener);
    }

    public void addWaitingForAcknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void apply(ServerTransaction txn, Map objects, BackReferences includeIDs,
                      ObjectInstanceMonitor instanceMonitor) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void broadcasted(NodeID waiter, TransactionID requestID) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void callBackOnTxnsInSystemCompletion(TxnsInSystemCompletionLister l) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void commit(PersistenceTransactionProvider ptxp, Collection objects, Map newRoots,
                       Collection appliedServerTransactionIDs) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public int getTotalPendingTransactionsCount() {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void goToActiveMode() {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void incomingTransactions(NodeID nodeID, Set txnIDs, Collection txns, boolean relayed) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public boolean isWaiting(NodeID waiter, TransactionID requestID) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void nodeConnected(NodeID nodeID) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void removeTransactionListener(ServerTransactionListener listener) {
      listeners.remove(listener);
    }

    public void setResentTransactionIDs(NodeID source, Collection transactionIDs) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void shutdownNode(NodeID nodeID) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void skipApplyAndCommit(ServerTransaction txn) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void start(Set cids) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void transactionsRelayed(NodeID node, Set serverTxnIDs) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public String dump() {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void dump(Writer writer) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void dumpToLogger() {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      throw new NotImplementedException(TestServerTransactionManager.class);
    }

    public void objectsSynched(NodeID node, ServerTransactionID tid) {
      throw new NotImplementedException(TestServerTransactionManager.class); 
    }

  }

  /**
   * RequestObjectManager calls: getActiveChannel(NodeID id);
   */
  private static class TestDSOChannelManager implements DSOChannelManager {

    public void addEventListener(DSOChannelManagerEventListener listener) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void closeAll(Collection channelIDs) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public MessageChannel getActiveChannel(NodeID id) {
      return new TestMessageChannel(((ClientID) id).getChannelID());
    }

    public MessageChannel[] getActiveChannels() {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public Set getAllActiveClientIDs() {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public Set getAllClientIDs() {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public String getChannelAddress(NodeID nid) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public ClientID getClientIDFor(ChannelID channelID) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public boolean isActiveID(NodeID nodeID) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void makeChannelActive(ClientID clientID, boolean persistent) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void makeChannelActiveNoAck(MessageChannel channel) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

  }

  private static class TestClientStateManager implements ClientStateManager {

    public void addAllReferencedIdsTo(Set rescueIds) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void addReference(NodeID nodeID, ObjectID objectID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public Set addReferences(NodeID nodeID, Set oids) {
      // just assume they are all new.
      return oids;
    }

    public List createPrunedChangesAndAddObjectIDTo(Collection changes, BackReferences references, NodeID clientID,
                                                    Set objectIDs) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public boolean hasReference(NodeID nodeID, ObjectID objectID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void removeReferencedFrom(NodeID nodeID, Set secondPass) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void removeReferences(NodeID nodeID, Set removed) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void shutdownNode(NodeID deadNode) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void startupNode(NodeID nodeID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void stop() {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

  }

  /**
   * RequestObjectManager calls: start(); lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext
   * responseContext,int maxCount); releaseReadOnly(ManagedObject object);
   */
  private static class TestObjectManager implements ObjectManager {

    public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void createNewObjects(Set ids) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void createRoot(String name, ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void flushAndEvict(List objects2Flush) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ObjectIDSet getAllObjectIDs() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public int getCheckedOutCount() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public GarbageCollector getGarbageCollector() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ManagedObject getObjectByIDOrNull(ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public Set getRootIDs() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public Map getRootNamesToIDsMap() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public Iterator getRoots() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    // TODO: need to implement
    public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext,
                                                 int maxCount) {

      Set ids = responseContext.getLookupIDs();
      Map resultsMap = new HashMap();
      for (Iterator iter = ids.iterator(); iter.hasNext();) {
        ObjectID id = (ObjectID) iter.next();
        ManagedObjectImpl mo = new ManagedObjectImpl(id);
        mo.apply(new TestDNA(new TestDNACursor()), new TransactionID(id.toLong()), new BackReferences(),
                 new NullObjectInstanceMonitor(), true);
        resultsMap.put(id, mo);
      }

      ObjectManagerLookupResults results = new ObjectManagerLookupResultsImpl(resultsMap, new HashSet());
      responseContext.setResults(results);

      return false;
    }

    public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ObjectID lookupRootID(String name) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void notifyGCComplete(GCResultContext resultContext) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void preFetchObjectsAndCreate(Set oids, Set newOids) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void release(PersistenceTransaction tx, ManagedObject object) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void releaseAll(PersistenceTransaction tx, Collection collection) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void releaseAllReadOnly(Collection objects) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    // TODO : need to implement
    public void releaseReadOnly(ManagedObject object) {
      // do nothing, just a test
    }

    public void setGarbageCollector(GarbageCollector gc) {
      throw new NotImplementedException(TestObjectManager.class);

    }

    public void setStatsListener(ObjectManagerStatsListener listener) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    // TODO: need to implement
    public void start() {
      // starting...
    }

    public void stop() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void waitUntilReadyToGC() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ManagedObject getObjectByID(ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

  }

  private static class TestMessageChannel implements MessageChannel {

    public List                   createMessageContexts = new ArrayList();
    public NoExceptionLinkedQueue sendQueue             = new NoExceptionLinkedQueue();
    private ChannelID             channelID;

    public TestMessageChannel(ChannelID channelID) {
      this.channelID = channelID;
    }

    public void addAttachment(String key, Object value, boolean replace) {
      //
    }

    public void addListener(ChannelEventListener listener) {
      //
    }

    public void close() {
      //
    }

    public TCMessage createMessage(TCMessageType type) {
      if (TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE.equals(type)) {
        return new TestObjectsNotFoundMessage(channelID);
      } else if (TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE.equals(type)) {
        return new TestRequestManagedObjectResponseMessage(channelID);
      } else {
        return null;
      }
    }

    public Object getAttachment(String key) {
      return null;
    }

    public ChannelID getChannelID() {
      return channelID;
    }

    public TCSocketAddress getLocalAddress() {
      return null;
    }

    public TCSocketAddress getRemoteAddress() {
      return null;
    }

    public boolean isClosed() {
      return false;
    }

    public boolean isConnected() {
      return false;
    }

    public boolean isOpen() {
      return false;
    }

    public NetworkStackID open() {
      return null;
    }

    public Object removeAttachment(String key) {
      return null;
    }

    public void send(TCNetworkMessage message) {
      sendQueue.put(message);
    }

  }

  private static class TestRequestManagedObjectResponseMessage implements RequestManagedObjectResponseMessage,
      Comparable {

    protected static Set sendSet = new TreeSet();

    private ChannelID    channelID;

    public TestRequestManagedObjectResponseMessage(ChannelID channelID) {
      this.channelID = channelID;
    }

    public ChannelID getChannelID() {
      return channelID;
    }

    public long getBatchID() {
      return 0;
    }

    public Collection getObjects() {
      return null;
    }

    public ObjectStringSerializer getSerializer() {
      return null;
    }

    public int getTotal() {
      return 0;
    }

    public void initialize(TCByteBuffer[] dnas, int count, ObjectStringSerializer serializer, long bid, int tot) {
      //
    }

    public void dehydrate() {
      //
    }

    public MessageChannel getChannel() {
      return null;
    }

    public ClientID getClientID() {
      return null;
    }

    public SessionID getLocalSessionID() {
      return null;
    }

    public TCMessageType getMessageType() {
      return null;
    }

    public int getTotalLength() {
      return 0;
    }

    public void hydrate() {
      //
    }

    public void send() {
      sendSet.add(this);
    }

    public int compareTo(Object o) {
      Long value1 = getChannelID().toLong();
      Long value2 = ((TestRequestManagedObjectResponseMessage) o).getChannelID().toLong();
      return value1.compareTo(value2);
    }

  }

  private static class TestObjectsNotFoundMessage implements ObjectsNotFoundMessage, Comparable {

    protected static Set sendSet = new TreeSet();

    private ChannelID    channelID;

    public TestObjectsNotFoundMessage(ChannelID channelID) {
      this.channelID = channelID;
    }

    public ChannelID getChannelID() {
      return channelID;
    }

    public long getBatchID() {
      return 0;
    }

    public Set getMissingObjectIDs() {
      return null;
    }

    public void initialize(Set missingObjectIDs, long batchId) {
      //
    }

    public void dehydrate() {
      //
    }

    public MessageChannel getChannel() {
      return null;
    }

    public ClientID getClientID() {
      return null;
    }

    public SessionID getLocalSessionID() {
      return null;
    }

    public TCMessageType getMessageType() {
      return null;
    }

    public int getTotalLength() {
      return 0;
    }

    public void hydrate() {
      //
    }

    public void send() {
      sendSet.add(this);
    }

    public int compareTo(Object o) {
      Long value1 = getChannelID().toLong();
      Long value2 = ((TestObjectsNotFoundMessage) o).getChannelID().toLong();
      return value1.compareTo(value2);
    }

  }

  private static class TestObjectRequestManager implements ObjectRequestManager {

    protected ClientID        cID;

    protected ObjectRequestID oID;

    protected Set             lookupIds;

    protected int             maxDepth;

    protected boolean         serverInit;

    protected String          rThreadName;

    protected Collection      objColl;

    public void requestObjects(ClientID clientID, ObjectRequestID requestID, Set ids, int maxRequestDepth,
                               boolean serverInitiated, String requestingThreadName) {
      this.cID = clientID;
      this.oID = requestID;
      this.lookupIds = ids;
      this.maxDepth = maxRequestDepth;
      this.serverInit = serverInitiated;
      this.rThreadName = requestingThreadName;

    }

    public void sendObjects(ClientID requestedNodeID, Collection objs, Set requestedObjectIDs, Set missingObjectIDs,
                            boolean isServerInitiated) {

      throw new NotImplementedException(TestObjectRequestManager.class);
    }

    public void createAndAddManagedObjectRequestContextsTo(ClientID clientID, ObjectRequestID requestID, Set ids,
                                                           int maxRequestDepth, boolean serverInitiated,
                                                           String requestingThreadName) {
      throw new NotImplementedException(TestObjectRequestManager.class);
    }

  }

}
