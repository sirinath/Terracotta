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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ObjectRequestManagerImpl implements ObjectRequestManager, ServerTransactionListener {

  // TODO read from l2 property
  public static final int                MAX_OBJECTS_TO_LOOKUP = 5;

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
                          boolean isServerInitiated, int maxRequestDepth) {

    basicSendObjects(requestedNodeID, objs, requestedObjectIDs, missingObjectIDs, isServerInitiated, maxRequestDepth);
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
    TreeSet<ObjectID> sortedIDs = new TreeSet<ObjectID>();
    TreeSet<ObjectID> split = new TreeSet<ObjectID>();

    synchronized (this) {
      int splitIndex = 1;
      for (Iterator iter = ids.iterator(); iter.hasNext();) {
        sortedIDs.add((ObjectID) iter.next());
      }

      for (Iterator iter = sortedIDs.iterator(); iter.hasNext();) {
        split.add((ObjectID) iter.next());
        if (splitIndex == MAX_OBJECTS_TO_LOOKUP) {
          RequestedObject reqObj = new RequestedObject(split, maxRequestDepth);
          if (objectRequestCache.add(reqObj, clientID)) {
            LookupContext lookupContext = new LookupContext(this, clientID, requestID, split, maxRequestDepth,
                                                            requestingThreadName, serverInitiated,
                                                            respondObjectRequestSink);

            objectManager.lookupObjectsAndSubObjectsFor(clientID, lookupContext, maxRequestDepth);
          }
          splitIndex = 0;
          split = new TreeSet<ObjectID>();
        }
        splitIndex++;
      }
      if (split.size() > 0) {
        RequestedObject reqObj = new RequestedObject(split, maxRequestDepth);
        if (objectRequestCache.add(reqObj, clientID)) {
          LookupContext lookupContext = new LookupContext(this, clientID, requestID, split, maxRequestDepth,
                                                          requestingThreadName, serverInitiated,
                                                          respondObjectRequestSink);

          objectManager.lookupObjectsAndSubObjectsFor(clientID, lookupContext, maxRequestDepth);
        }
      }
    }
  }

  private void basicSendObjects(ClientID requestedNodeID, Collection objs, Set requestedObjectIDs,
                                Set missingObjectIDs, boolean isServerInitiated, int maxRequestDepth) {

    Map messageMap = new HashMap();

    Map clientNewIDsMap = new HashMap(); // will contain the object which are not present in the client out of the
    // returned ones

    LinkedList objectsInOrder = new LinkedList();
    try {

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

        long batchID = batchIDSequence.next();

        // sort the requested Objects
        TreeSet<ObjectID> sortedRequestedIDs = new TreeSet<ObjectID>();
        for (Iterator iter = requestedObjectIDs.iterator(); iter.hasNext();) {
          sortedRequestedIDs.add((ObjectID) iter.next());
        }

        RequestedObject reqObj = new RequestedObject(sortedRequestedIDs, maxRequestDepth);
        LinkedHashSet<ClientID> clientList = this.objectRequestCache.getClientsForRequest(reqObj);

        // prepare clients
        for (Iterator iter = clientList.iterator(); iter.hasNext();) {
          ClientID clientID = (ClientID) iter.next();

          Set newIds = stateManager.addReferences(clientID, ids);
          clientNewIDsMap.put(clientID, newIds);

          // make batch and send object for each client.
          MessageChannel channel = channelManager.getActiveChannel(clientID);
          messageMap.put(clientID, new BatchAndSend(channel, batchID));
        }

        for (Iterator iter = objectsInOrder.iterator(); iter.hasNext();) {
          ManagedObject mo = (ManagedObject) iter.next();

          for (Iterator i = clientList.iterator(); i.hasNext();) {
            ClientID clientID = (ClientID) i.next();
            Set newIDs = (Set) clientNewIDsMap.get(clientID);
            if (newIDs.contains(mo.getID())) {
              BatchAndSend batchAndSend = (BatchAndSend) messageMap.get(clientID);

              batchAndSend.sendObject(mo, iter.hasNext());
              // remove this id from the new ID
              newIDs.remove(mo.getID());
            } else {
              logger.info("Client " + clientID + " already contains " + mo.getID() + ". So not sending it.");
            }
          }
          objectManager.releaseReadOnly(mo);
        }
        this.objectRequestCache.remove(reqObj);

        if (!missingObjectIDs.isEmpty()) {
          if (isServerInitiated) {
            // This is a possible case where changes are flying in and server is initiating some lookups and the lookups
            // go pending and in the meantime the changes made those looked up objects garbage and DGC removes those
            // objects. Now we dont want to send those missing objects to clients. Its not really an issue as the
            // clients should never lookup those objects, but still why send them ?
            logger.warn("Server Initiated lookup. Ignoring Missing Objects : " + missingObjectIDs);
          } else {
            for (Iterator missingIterator = messageMap.keySet().iterator(); missingIterator.hasNext();) {
              ClientID clientID = (ClientID) missingIterator.next();
              BatchAndSend batchAndSend = (BatchAndSend) messageMap.get(clientID);
              Set requestedIdsForClient = reqObj.getOIdSet();
              logger.info("sending missing ids: + " + requestedIdsForClient.size() + " , to client: " + clientID);
              batchAndSend.sendMissingObjects(missingObjectIDs, requestedIdsForClient);
            }
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

  protected synchronized int getTotalRequestedObjects() {
    return objectRequestCache.numberOfRequestedObjects();
  }

  protected synchronized int getObjectRequestCacheClientSize() {
    return objectRequestCache.clientSize();
  }

  protected static class RequestedObject {

    private final TreeSet<ObjectID> oIdSet;

    private final int               depth;

    public RequestedObject(TreeSet<ObjectID> oIdSet, int depth) {
      this.oIdSet = oIdSet;
      this.depth = depth;
    }

    public TreeSet<ObjectID> getOIdSet() {
      return oIdSet;
    }

    public int getDepth() {
      return depth;
    }

    public boolean equals(Object obj) {
      RequestedObject reqObj = (RequestedObject) obj;
      if (this.oIdSet.equals(reqObj.getOIdSet()) && this.depth == reqObj.getDepth()) { return true; }
      return false;
    }

    public int hashCode() {
      return this.oIdSet.hashCode();
    }

    public String toString() {
      String msg = "";
      for (Iterator iter = this.oIdSet.iterator(); iter.hasNext();) {
        msg += " Object: " + iter.next();
      }
      return msg;
    }
  }

  protected static class ObjectRequestCache {

    private Map<RequestedObject, LinkedHashSet<ClientID>> objectRequestMap = new HashMap();

    protected int numberOfRequestedObjects() {
      int val = 0;
      for (Iterator iter = this.objectRequestMap.keySet().iterator(); iter.hasNext();) {
        val += ((RequestedObject) iter.next()).getOIdSet().size();
      }
      return val;
    }

    protected int cacheSize() {
      return objectRequestMap.size();
    }

    protected int clientSize() {
      return clients().size();
    }

    public boolean add(RequestedObject reqObjects, ClientID clientID) {
      // check already been requested.

      LinkedHashSet<ClientID> clientList = this.objectRequestMap.get(reqObjects);
      if (clientList == null) {
        clientList = new LinkedHashSet<ClientID>();
        clientList.add(clientID);
        this.objectRequestMap.put(reqObjects, clientList);
        return true;
      } else {
        clientList.add(clientID);
        return false;
      }
    }

    public boolean contains(RequestedObject reqObj) {
      return this.objectRequestMap.containsKey(reqObj);
    }

    public Set clients() {
      Set clients = new LinkedHashSet();
      for (Iterator i = objectRequestMap.keySet().iterator(); i.hasNext();) {
        LinkedHashSet<ClientID> clientList = this.objectRequestMap.get(i.next());
        clients.addAll(clientList);
      }
      return clients;
    }

    public LinkedHashSet<ClientID> getClientsForRequest(RequestedObject reqObj) {
      return this.objectRequestMap.get(reqObj);
    }

    public void remove(RequestedObject reqObj) {
      this.objectRequestMap.remove(reqObj);
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
                                                            this.missingObjects, this.serverInitiated,
                                                            this.maxRequestDepth);
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

    private final int  maxRequestDepth;

    public ResponseContext(ClientID requestedNodeID, Collection objs, Set requestedObjectIDs, Set missingObjectIDs,
                           boolean serverInitiated, int maxDepth) {
      this.requestedNodeID = requestedNodeID;
      this.objs = objs;
      this.requestedObjectIDs = requestedObjectIDs;
      this.missingObjectIDs = missingObjectIDs;
      this.serverInitiated = serverInitiated;
      this.maxRequestDepth = maxDepth;
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

    public int getRequestDepth() {
      return maxRequestDepth;
    }

    @Override
    public String toString() {

      return "ResponseContext [ requestNodeID = " + requestedNodeID + " , objs.size = " + objs.size()
             + " , requestedObjectIDs.size = " + requestedObjectIDs + " , missingObjectIDs.size = " + missingObjectIDs
             + " , serverInitiated = " + serverInitiated + " ] ";
    }

  }

}
