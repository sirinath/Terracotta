/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.context.ObjectManagerRequestContext;
import com.tc.objectserver.context.RespondToObjectRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.tx.ServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.State;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectRequestManagerImpl implements ObjectRequestManager, ServerTransactionListener {

  //TODO read from l2 property
  private static final int               MAX_OBJECTS_TO_LOOKUP = 50;

  private final static TCLogger          logger                = TCLogging.getLogger(ObjectRequestManagerImpl.class);

  private final static State             INIT                  = new State("INITIAL");
  private final static State             STARTING              = new State("STARTING");
  private final static State             STARTED               = new State("STARTED");

  private final ObjectManager            objectManager;
  private final ServerTransactionManager transactionManager;

  private final List                     pendingRequests       = new LinkedList();
  private final Set                      resentTransactionIDs  = new HashSet();
  private final Sink                     managedObjectRequestSink;
  private final Sink                     respondObjectRequestSink;
  private volatile State                 state                 = INIT;
  private DSOChannelManager              channelManager;
  private ClientStateManager             stateManager;
  private Sequence                       batchIDSequence       = new SimpleSequence();

  private ObjectRequestCache             objectRequestCache    = new ObjectRequestCache();

  public ObjectRequestManagerImpl(Sink managedObjectRequestSink, ObjectManager objectManager,
                                  DSOChannelManager channelManager, ClientStateManager stateManager,
                                  ServerTransactionManager transactionManager, Sink respondObjectRequestSink) {
    this.objectManager = objectManager;
    this.channelManager = channelManager;
    this.stateManager = stateManager;
    this.transactionManager = transactionManager;
    this.managedObjectRequestSink = managedObjectRequestSink;
    this.respondObjectRequestSink = respondObjectRequestSink;
    transactionManager.addTransactionListener(this);

  }

  public synchronized void transactionManagerStarted(Set cids) {
    state = STARTING;
    objectManager.start();
    moveToStartedIfPossible();
  }

  private void moveToStartedIfPossible() {
    if (state == STARTING && resentTransactionIDs.isEmpty()) {
      state = STARTED;
      transactionManager.removeTransactionListener(this);
      processPending();
    }
  }

  public void requestObjects(ClientID clientID, ObjectRequestID requestID, Set ids, int maxRequestDepth,
                             boolean serverInitiated, String requestingThreadName) {
    synchronized (this) {
      if (state != STARTED) {
        LookupContext lookupContext = new LookupContext(this, clientID, requestID, ids, maxRequestDepth,
                                                        requestingThreadName, serverInitiated, respondObjectRequestSink);
        pendingRequests.add(lookupContext);
        if (logger.isDebugEnabled()) {
          logger.debug("RequestObjectManager is not started, lookup has been added to pending request: "
                       + lookupContext);
        }
        return;
      }
    }
    basicRequestObjects(clientID, requestID, ids, maxRequestDepth, serverInitiated, requestingThreadName);
  }

  public void sendObjects(ClientID requestedNodeID, Collection objs, Set requestedObjectIDs, Set missingObjectIDs,
                          boolean isServerInitiated) {

    basicSendObjects(requestedNodeID, objs, requestedObjectIDs, missingObjectIDs, isServerInitiated);
  }

  public synchronized void addResentServerTransactionIDs(Collection sTxIDs) {
    if (state != INIT) { throw new AssertionError("Cant add Resent transactions after start up ! " + sTxIDs.size()
                                                  + "Txns : " + state); }
    resentTransactionIDs.addAll(sTxIDs);
    logger.info("resentTransactions = " + resentTransactionIDs.size());
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    return;
  }

  public void incomingTransactions(NodeID source, Set serverTxnIDs) {
    return;
  }

  public synchronized void clearAllTransactionsFor(NodeID client) {
    if (state == STARTED) return;
    for (Iterator iter = resentTransactionIDs.iterator(); iter.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) iter.next();
      if (stxID.getSourceID().equals(client)) {
        iter.remove();
      }
    }
    moveToStartedIfPossible();
  }

  private void processPending() {
    logger.info("Processing Pending Lookups = " + pendingRequests.size());
    for (Iterator iter = pendingRequests.iterator(); iter.hasNext();) {
      LookupContext lookupContext = (LookupContext) iter.next();
      logger.info("Processing pending Looking up : " + lookupContext);
      basicRequestObjects(lookupContext.getRequestedNodeID(), lookupContext.getRequestID(), lookupContext
          .getLookupIDs(), lookupContext.getMaxRequestDepth(), lookupContext.isServerInitiated(), lookupContext
          .getRequestingThreadName());
    }
  }

  public synchronized void transactionApplied(ServerTransactionID stxID) {
    resentTransactionIDs.remove(stxID);
    moveToStartedIfPossible();
  }

  // Utility method to create 1 or more server initiated requests.
  public void createAndAddManagedObjectRequestContextsTo(ClientID clientID, ObjectRequestID requestID, Set ids,
                                                         int maxRequestDepth, boolean serverInitiated,
                                                         String requestingThreadName) {
    if (ids.size() <= MAX_OBJECTS_TO_LOOKUP) {
      managedObjectRequestSink.add(new LookupContext(this, clientID, requestID, ids, -1, requestingThreadName, true,
                                                     respondObjectRequestSink));
    } else {
      String threadName = Thread.currentThread().getName();
      // split into multiple request
      Set split = new HashSet(MAX_OBJECTS_TO_LOOKUP);
      for (Iterator i = ids.iterator(); i.hasNext();) {
        split.add(i.next());
        if (split.size() >= MAX_OBJECTS_TO_LOOKUP || !i.hasNext()) {
          managedObjectRequestSink.add(new LookupContext(this, clientID, requestID, split, -1, threadName, true,
                                                         respondObjectRequestSink));
          if (i.hasNext()) split = new HashSet(MAX_OBJECTS_TO_LOOKUP);
        }
      }
    }
  }

  private void basicRequestObjects(ClientID clientID, ObjectRequestID requestID, Set ids, int maxRequestDepth,
                                   boolean serverInitiated, String requestingThreadName) {
    Set lookupIDs = new HashSet();
 
    synchronized (this) {
      for (Iterator iter = ids.iterator(); iter.hasNext();) {
        ObjectID id = (ObjectID) iter.next();
        if (objectRequestCache.add(clientID, id)) {
     
          lookupIDs.add(id);
        }
      }
    }
    if (lookupIDs.size() > 0) {
      LookupContext lookupContext = new LookupContext(this, clientID, requestID, lookupIDs, maxRequestDepth,
                                                      requestingThreadName, serverInitiated, respondObjectRequestSink);
      
      objectManager.lookupObjectsAndSubObjectsFor(clientID, lookupContext, maxRequestDepth);
    }
  }

  private void basicSendObjects(ClientID requestedNodeID, Collection objs, Set requestedObjectIDs,
                                Set missingObjectIDs, boolean isServerInitiated) {
    
    Map messageMap = new HashMap();
    Map clientObjectIDMap = new HashMap();
    Map clientNewIDsMap = new HashMap();

    LinkedList objectsInOrder = new LinkedList();
    try {

      long batchID = batchIDSequence.next();

      Set ids = new HashSet(Math.max((int) (objs.size() / .75f) + 1, 16));
      synchronized (this) {
        for (Iterator i = objs.iterator(); i.hasNext();) {
          ManagedObject mo = (ManagedObject) i.next();
          ids.add(mo.getID());
          if (requestedObjectIDs.contains(mo.getID())) {
            objectsInOrder.addLast(mo);
          } else {
            objectsInOrder.addFirst(mo);
          }
        }

        // prepare clients
        for (Iterator iter = objectRequestCache.clients().iterator(); iter.hasNext();) {
          ClientID clientID = (ClientID) iter.next();

          // clientID -> ids in request
          clientObjectIDMap.put(clientID, objectRequestCache.ids(clientID));

          Set newIds = stateManager.addReferences(clientID, ids);
          clientNewIDsMap.put(clientID, newIds);
          
          // make batch and send object for each client.
          MessageChannel channel = channelManager.getActiveChannel(clientID);
          messageMap.put(clientID, new BatchAndSend(channel, batchID));
        }

        // remove missing objects from cache
        if (!missingObjectIDs.isEmpty()) {
          objectRequestCache.remove(missingObjectIDs);
        }

        // remove found objects from cache, since we are going to send them
        if (!ids.isEmpty()) {
          objectRequestCache.remove(ids);
        }

      }

      // Only send objects that are NOT already there in the client. Look at the comment below.

      for (Iterator i = objectsInOrder.iterator(); i.hasNext();) {

        ManagedObject m = (ManagedObject) i.next();
        i.remove();
        // We dont want to send any object twice to the client even the client requested it 'coz it only means
        // that the object is on its way to the client. This is true because we process the removeObjectIDs and
        // lookups in Order. Earlier the if condition used to look like ...
        // if (ids.contains(m.getID()) || morc.getObjectIDs().contains(m.getID())) {}
        Set currentRequestedIDs = (Set) clientObjectIDMap.get(requestedNodeID);
        for (Iterator cIter = messageMap.keySet().iterator(); cIter.hasNext();) {
          ClientID clientID = (ClientID) cIter.next();
          Set requestedIDs = (Set) clientObjectIDMap.get(clientID);
          Set newIds = (Set)clientNewIDsMap.get(clientID);
          // does this id need to be sent to the client, and is it also an id that has been requested by the client
          // 
          if (newIds.contains(m.getID()) && (requestedNodeID.equals(clientID) || (requestedIDs.contains(m.getID()) || !currentRequestedIDs.contains(m.getID())))) {
            
            BatchAndSend batchAndSend = (BatchAndSend) messageMap.get(clientID);
            
            batchAndSend.sendObject(m, i.hasNext());
            requestedIDs.remove(m.getID());
            newIds.remove(m.getID());
          } else if (requestedObjectIDs.contains(m.getID())) {
           //
          }
        }
        objectManager.releaseReadOnly(m);
      }

      if (!missingObjectIDs.isEmpty()) {
        if (isServerInitiated) {
          // This is a possible case where changes are flying in and server is initiating some lookups and the lookups
          // go pending and in the meantime the changes made those looked up objects garbage and DGC removes those
          // objects. Now we dont want to send those missing objects to clients. Its not really an issue as the clients
          // should never lookup those objects, but still why send them ?
          logger.warn("Server Initiated lookup. Ignoring Missing Objects : " + missingObjectIDs);
        } else {
          for (Iterator missingIterator = messageMap.keySet().iterator(); missingIterator.hasNext();) {
            ClientID clientID = (ClientID) missingIterator.next();
            BatchAndSend batchAndSend = (BatchAndSend) messageMap.get(clientID);
            Set missingIDsForClient = (Set) clientObjectIDMap.get(clientID);
            batchAndSend.sendMissingObjects(missingObjectIDs, missingIDsForClient);
            logger.info("sending missing ids: + " + missingIDsForClient.size() + " , to client: " + clientID);
          }
        }
      }

    } catch (NoSuchChannelException e) {
      for (Iterator i = objectsInOrder.iterator(); i.hasNext();) {
        objectManager.releaseReadOnly((ManagedObject) i.next());
      }
      return;
    } finally {
      for (Iterator iter = messageMap.values().iterator(); iter.hasNext();) {
        BatchAndSend batchAndSend = (BatchAndSend) iter.next();
        batchAndSend.flush();
      }
    }
  }

  protected synchronized int getObjectRequestCacheSize() {
    return objectRequestCache.cacheSize();
  }

  protected synchronized int getObjectRequestCacheClientSize() {
    return objectRequestCache.clientSize();
  }

  protected static class ObjectRequestCache {

    private Set objectRequestSet = new HashSet();
    private Map objectRequestMap = new HashMap();

    protected int cacheSize() {
      return objectRequestSet.size();
    }

    protected int clientSize() {
      return objectRequestMap.size();
    }

    public boolean add(ClientID clientID, ObjectID id) {
      // check already been requested.
      
      boolean notInCache = objectRequestSet.add(id);

      Set ids = (Set) objectRequestMap.get(clientID);
      if (ids == null) {
        objectRequestMap.put(clientID, (ids = new HashSet()));
      }
      ids.add(id);

      return notInCache;
    }

    public Set clients() {
      Set clients = new HashSet();
      for (Iterator i = objectRequestMap.keySet().iterator(); i.hasNext();) {
        ClientID clientID = (ClientID) i.next();
        clients.add(clientID);
      }
      return clients;
    }

    public Set ids(ClientID id) {
      Set set = new HashSet();

      if (objectRequestMap.containsKey(id)) {
        Iterator iter = ((Set) objectRequestMap.get(id)).iterator();
        for (; iter.hasNext();) {
          ObjectID oid = (ObjectID)iter.next();
          set.add(oid);
        }
      }
      return set;
    }

    public void remove(Set ids) {
      for (Iterator i = ids.iterator(); i.hasNext();) {
        remove((ObjectID)i.next());
      }
    }

    private void remove(ObjectID oid) {

      for (Iterator i = objectRequestMap.keySet().iterator(); i.hasNext();) {
        ClientID clientID = (ClientID) i.next();
        Set ids = (Set) objectRequestMap.get(clientID);
        ids.remove(oid);
        if (ids.size() < 1) {
          i.remove();
        }
      }
      objectRequestSet.remove(oid);

    }
  }
  

  protected static class BatchAndSend {

    private final MessageChannel     channel;

    private Integer                  sendCount  = 0;

    private Integer                  batches    = 0;

    private ObjectStringSerializer   serializer = new ObjectStringSerializer();

    private TCByteBufferOutputStream out        = new TCByteBufferOutputStream();

    private long                     batchID    = 0;

    public BatchAndSend(MessageChannel channel, long batchID) {
      this.channel = channel;
      this.batchID = batchID;
    }

    public synchronized void sendObject(ManagedObject m, boolean hasNext) {
      m.toDNA(out, serializer);
      sendCount++;
      if (sendCount > 1000 || (sendCount > 0 && !hasNext)) {
        batches++;
        RequestManagedObjectResponseMessage responseMessage = (RequestManagedObjectResponseMessage) channel
            .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
        responseMessage.initialize(out.toArray(), sendCount, serializer, batchID, hasNext ? 0 : batches);
        responseMessage.send();
        sendCount = 0;
        serializer = new ObjectStringSerializer();
        out = new TCByteBufferOutputStream();
      }
    }

    public synchronized void flush() {
      if (sendCount > 0) {
        RequestManagedObjectResponseMessage responseMessage = (RequestManagedObjectResponseMessage) channel
            .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
        responseMessage.initialize(out.toArray(), sendCount, serializer, batchID, 0);
        responseMessage.send();
        sendCount = 0;
        serializer = new ObjectStringSerializer();
        out = new TCByteBufferOutputStream();
      }
    }

    public synchronized void sendMissingObjects(Set missingObjectIDs, Set ids) {
      Set missingObjectsInClient = new HashSet();
      for (Iterator iter = ids.iterator(); iter.hasNext();) {
        ObjectID id = (ObjectID) iter.next();
        if (missingObjectIDs.contains(id)) {
          missingObjectsInClient.add(id);
        }
      }
      if (missingObjectsInClient.size() > 0) {
        ObjectsNotFoundMessage notFound = (ObjectsNotFoundMessage) channel
            .createMessage(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE);
        notFound.initialize(missingObjectsInClient, batchID);
        notFound.send();
      }
    }

    protected int getSendCount() {
      return sendCount;
    }

    protected int getBatches() {
      return batches;
    }

    protected ObjectStringSerializer getSerializer() {
      return serializer;
    }

    protected TCByteBufferOutputStream getOut() {
      return out;
    }

  }

  protected static class LookupContext implements ObjectManagerRequestContext {

    private ClientID             clientID       = null;
    private ObjectRequestID      requestID      = null;
    private Set                  lookupIDs      = new HashSet();
    private Map                  objects        = new HashMap();
    private Set                  missingObjects = new HashSet();
    private int                  maxRequestDepth;
    private String               requestingThreadName;
    private boolean              serverInitiated;
    private Sink                 respondObjectRequestSink;
    private ObjectRequestManager objectRequestManager;

    public LookupContext(ObjectRequestManager objectRequestManager, ClientID clientID, ObjectRequestID requestID,
                         Set lookupIDs, int maxRequestDepth, String requestingThreadName, boolean serverInitiated,
                         Sink respondObjectRequestSink) {
      this.objectRequestManager = objectRequestManager;
      this.clientID = clientID;
      this.requestID = requestID;
      this.lookupIDs = lookupIDs;
      this.maxRequestDepth = maxRequestDepth;
      this.requestingThreadName = requestingThreadName;
      this.serverInitiated = serverInitiated;
      this.respondObjectRequestSink = respondObjectRequestSink;

    }

    public Set getLookupIDs() {
      return lookupIDs;
    }

    public Set getNewObjectIDs() {
      return Collections.EMPTY_SET;
    }

    public void missingObject(ObjectID oid) {
      missingObjects.add(oid);
    }

    // TODO implement checkoutMap in future
    public void setResults(ObjectManagerLookupResults results) {
      objects = results.getObjects();
      if (results.getLookupPendingObjectIDs().size() > 0) {
        if (logger.isDebugEnabled()) {
          logger.debug("lookupPendingObjectIDs.size = " + results.getLookupPendingObjectIDs() + " , clientID = "
                       + clientID + " , requestID = " + requestID);
        }
        objectRequestManager.createAndAddManagedObjectRequestContextsTo(this.clientID, this.requestID, results
            .getLookupPendingObjectIDs(), -1, true, this.requestingThreadName);
      }
      ResponseContext responseContext = new ResponseContext(this.clientID, this.objects.values(), this.lookupIDs,
                                                            this.missingObjects, this.serverInitiated);
      respondObjectRequestSink.add(responseContext);
      if (logger.isDebugEnabled()) {
        logger.debug("adding to respondSink , clientID = " + clientID + " , requestID = " + requestID + " "
                     + responseContext);
      }
    }

    public ObjectRequestID getRequestID() {
      return requestID;
    }

    public int getMaxRequestDepth() {
      return maxRequestDepth;
    }

    public boolean updateStats() {
      return false;
    }

    public boolean isServerInitiated() {
      return serverInitiated;
    }

    public ClientID getRequestedNodeID() {
      return clientID;
    }

    public String getRequestingThreadName() {
      return requestingThreadName;
    }

    @Override
    public String toString() {
      return "Lookup Context [ clientID = " + clientID + " , requestID = " + requestID + " , ids.size = " + lookupIDs
             + " , objects.size = " + objects.size() + " , missingObjects.size  = " + missingObjects
             + " , maxRequestDepth = " + maxRequestDepth + " , requestingThreadName = " + requestingThreadName
             + " , serverInitiated = " + serverInitiated + " , respondObjectRequestSink = " + respondObjectRequestSink
             + " ] ";
    }

  }

  protected static class ResponseContext implements RespondToObjectRequestContext {

    private ClientID   requestedNodeID;

    private Collection objs;

    private Set        requestedObjectIDs;

    private Set        missingObjectIDs;

    private boolean    serverInitiated;

    public ResponseContext(ClientID requestedNodeID, Collection objs, Set requestedObjectIDs, Set missingObjectIDs,
                           boolean serverInitiated) {
      this.requestedNodeID = requestedNodeID;
      this.objs = objs;
      this.requestedObjectIDs = requestedObjectIDs;
      this.missingObjectIDs = missingObjectIDs;
      this.serverInitiated = serverInitiated;
    }

    public ClientID getRequestedNodeID() {
      return requestedNodeID;
    }

    public Collection getObjs() {
      return objs;
    }

    public Set getRequestedObjectIDs() {
      return requestedObjectIDs;
    }

    public Set getMissingObjectIDs() {
      return missingObjectIDs;
    }

    public boolean isServerInitiated() {
      return serverInitiated;
    }

    @Override
    public String toString() {

      return "ResponseContext [ requestNodeID = " + requestedNodeID + " , objs.size = " + objs.size()
             + " , requestedObjectIDs.size = " + requestedObjectIDs + " , missingObjectIDs.size = " + missingObjectIDs
             + " , serverInitiated = " + serverInitiated + " ] ";
    }

  }

}
