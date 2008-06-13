/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
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

  private final static TCLogger          logger               = TCLogging.getLogger(ObjectRequestManagerImpl.class);

  private final static State             INIT                 = new State("INITIAL");
  private final static State             STARTING             = new State("STARTING");
  private final static State             STARTED              = new State("STARTED");

  private final ObjectManager            objectManager;
  private final ServerTransactionManager transactionManager;

  private final List                     pendingRequests      = new LinkedList();
  private final Set                      resentTransactionIDs = new HashSet();
  private volatile State                 state                = INIT;
  private Sink                           respondObjectRequestSink;
  private DSOChannelManager              channelManager;
  private ClientStateManager             stateManager;
  private Sequence                       batchIDSequence      = new SimpleSequence();

  private ObjectRequestCache             objectRequestCache   = new ObjectRequestCache();

  public ObjectRequestManagerImpl(ObjectManager objectManager, DSOChannelManager channelManager,
                                  ClientStateManager stateManager, ServerTransactionManager transactionManager,
                                  Sink respondObjectRequestSink) {
    this.objectManager = objectManager;
    this.channelManager = channelManager;
    this.stateManager = stateManager;
    this.transactionManager = transactionManager;
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
        pendingRequests.add(new LookupContext(this, clientID, requestID, ids, maxRequestDepth, requestingThreadName,
                                              serverInitiated, respondObjectRequestSink));
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

    objectManager.lookupObjectsAndSubObjectsFor(clientID, new LookupContext(this, clientID, requestID, lookupIDs,
                                                                            maxRequestDepth, requestingThreadName,
                                                                            serverInitiated, respondObjectRequestSink),
                                                maxRequestDepth);

  }

  private void basicSendObjects(ClientID requestedNodeID, Collection objs, Set requestedObjectIDs,
                                Set missingObjectIDs, boolean isServerInitiated) {
    Map messageMap = new HashMap();
    Map clientObjectIDMap = new HashMap();

    LinkedList objectsInOrder = new LinkedList();
    try {
      long batchID = batchIDSequence.next();

      Set ids = new HashSet(Math.max((int) (objs.size() / .75f) + 1, 16));
      synchronized (this) {
        Set removeIDLists = new HashSet();
        for (Iterator i = objs.iterator(); i.hasNext();) {
          ManagedObject mo = (ManagedObject) i.next();
          ObjectID id = mo.getID();

          // prepare channels
          Set clients = objectRequestCache.clients(id);

          for (Iterator iter = clients.iterator(); iter.hasNext();) {
            ClientID clientID = (ClientID) iter.next();
            clientObjectIDMap.put(clientID, objectRequestCache.ids(clientID));
            MessageChannel channel = channelManager.getActiveChannel(clientID);
            messageMap.put(clientID, new MessageBatchAndSend(channel, batchID));
          }
          removeIDLists.add(id);
          ids.add(id);
          if (requestedObjectIDs.contains(mo.getID())) {
            objectsInOrder.addLast(mo);
          } else {
            objectsInOrder.addFirst(mo);
          }
        }
        if (!missingObjectIDs.isEmpty()) {
          objectRequestCache.remove(missingObjectIDs);
        }
        if(!removeIDLists.isEmpty()) {
          objectRequestCache.remove(removeIDLists);
        }

      }

      // Only send objects that are NOT already there in the client. Look at the comment below.
      Map clientNewIDsMap = new HashMap();
      for (Iterator cIter = messageMap.keySet().iterator(); cIter.hasNext();) {
        ClientID clientID = (ClientID) cIter.next();
        clientNewIDsMap.put(clientID, stateManager.addReferences(clientID, ids));
      }

      for (Iterator i = objectsInOrder.iterator(); i.hasNext();) {

        ManagedObject m = (ManagedObject) i.next();
        i.remove();
        // We dont want to send any object twice to the client even the client requested it 'coz it only means
        // that the object is on its way to the client. This is true because we process the removeObjectIDs and
        // lookups in Order. Earlier the if condition used to look like ...
        // if (ids.contains(m.getID()) || morc.getObjectIDs().contains(m.getID())) {}
        for (Iterator cIter = messageMap.keySet().iterator(); cIter.hasNext();) {
          ClientID clientID = (ClientID) cIter.next();
          Set newIds = (Set) clientNewIDsMap.get(clientID);
          if (newIds.contains(m.getID())) {
            MessageBatchAndSend mbas = (MessageBatchAndSend) messageMap.get(clientID);
            mbas.sendObject(m, i.hasNext());
          } else if (requestedObjectIDs.contains(m.getID())) {
            // logger.info("Ignoring request for look up from " + morc.getChannelID() + " for " + m.getID());
          }
        }
        objectManager.releaseReadOnly(m);
      }

      // now flush all the messages left
      for (Iterator flushIterator = messageMap.values().iterator(); flushIterator.hasNext();) {
        MessageBatchAndSend mbas = (MessageBatchAndSend) flushIterator.next();
        mbas.flushMessage();
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
            MessageBatchAndSend mbas = (MessageBatchAndSend) messageMap.get(clientID);
            mbas.sendMissingObjects(missingObjectIDs, (Set) clientObjectIDMap.get(clientID));
          }
          //
        }
      }

    } catch (NoSuchChannelException e) {
      logger.info("Not sending response because channel is disconnected: " + requestedNodeID
                  + ".  Releasing all checked-out objects...");
      for (Iterator i = objectsInOrder.iterator(); i.hasNext();) {
        objectManager.releaseReadOnly((ManagedObject) i.next());
      }
      return;
    }
  }

  protected static class ObjectRequestCache {

    private Set objectRequestSet = new HashSet();
    private Map objectRequestMap = new HashMap();

    public boolean add(ClientID clientID, ObjectID id) {
      // check already been requested.
      boolean inCache = objectRequestSet.add(id);

      Set ids = (Set) objectRequestMap.get(clientID);
      if (ids == null) {
        objectRequestMap.put(clientID, (ids = new HashSet()));
      }
      ids.add(id);

      return inCache;
    }

    public Set clients(ObjectID id) {
      Set clients = new HashSet();
      for (Iterator i = objectRequestMap.keySet().iterator(); i.hasNext();) {
        ClientID clientID = (ClientID) i.next();
        Set ids = (Set) objectRequestMap.get(clientID);
        if (ids.contains(id)) {
          clients.add(clientID);
        }
      }
      return clients;
    }

    public Set ids(ClientID id) {
      Set set = null;
      return ( ( set = (Set) objectRequestMap.get(id)) == null) ? new HashSet() : set;
    }

    public void remove(Set ids) {
      for (Iterator i = ids.iterator(); i.hasNext();) {
        remove((ObjectID) i.next());
      }
    }

    private void remove(ObjectID id) {
      
      Set removeClientList = new HashSet();
      for (Iterator i = objectRequestMap.keySet().iterator(); i.hasNext();) {
        ClientID clientID = (ClientID) i.next();
        Set ids = (Set) objectRequestMap.get(clientID);
        ids.remove(id);
        if (ids.size() < 1) {
          removeClientList.add(clientID);
        }
      }  
      
      for(Iterator c = removeClientList.iterator(); c.hasNext();) {
        ClientID clientID = (ClientID)c.next();
        objectRequestMap.remove(clientID);
      }
      
      objectRequestSet.remove(id);     
      
    }
  }

  private static class MessageBatchAndSend {

    private MessageChannel                      channel         = null;

    private RequestManagedObjectResponseMessage responseMessage = null;

    private Integer                             sendCount       = null;

    private Integer                             batches         = null;

    private ObjectStringSerializer              serializer      = null;

    private TCByteBufferOutputStream            out             = null;

    private long                                batchID         = 0;

    public MessageBatchAndSend(MessageChannel channel, long batchID) {
      this.channel = channel;
      this.batchID = batchID;
      responseMessage = (RequestManagedObjectResponseMessage) channel
          .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
      serializer = new ObjectStringSerializer();
      out = new TCByteBufferOutputStream();
      sendCount = 0;
      batches = 0;

    }

    public void sendObject(ManagedObject m, boolean hasNext) {
      synchronized (this) {
        m.toDNA(out, serializer);
        sendCount++;
        if (sendCount > 1000) {
          batches++;
          responseMessage.initialize(out.toArray(), sendCount, serializer, batchID, hasNext ? 0 : batches);
          responseMessage.send();
          reinitializeMessage();
        }
      }
    }

    public void sendMissingObjects(Set missingObjectIDs, Set ids) {
      Set missingObjectsInClient = new HashSet();
      for (Iterator iter = missingObjectIDs.iterator(); iter.hasNext();) {
        ObjectID id = (ObjectID) iter.next();
        if (ids.contains(id)) {
          missingObjectsInClient.add(id);
        }
      }

      ObjectsNotFoundMessage notFound = (ObjectsNotFoundMessage) channel
          .createMessage(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE);
      notFound.initialize(missingObjectsInClient, batchID);
      notFound.send();
    }

    public void flushMessage() {
      TCByteBuffer[] b = out.toArray();
      if (b.length > 0) {
        responseMessage.initialize(out.toArray(), sendCount, serializer, batchID, batches);
        responseMessage.send();
      }
    }

    private void reinitializeMessage() {
      sendCount = 0;
      serializer = new ObjectStringSerializer();
      out = new TCByteBufferOutputStream();
      responseMessage = (RequestManagedObjectResponseMessage) channel
          .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);

    }
  }

  protected static class LookupContext implements ObjectManagerRequestContext {

    private ClientID             clientID;
    private ObjectRequestID      requestID;
    private Set                  ids;
    private Map                  objects;
    private Set                  missingObjects = new HashSet();
    private int                  maxRequestDepth;
    private String               requestingThreadName;
    private boolean              serverInitiated;
    private Sink                 respondObjectRequestSink;
    private ObjectRequestManager objectRequestManager;

    public LookupContext(ObjectRequestManager objectRequestManager, ClientID clientID, ObjectRequestID requestID,
                         Set ids, int maxRequestDepth, String requestingThreadName, boolean serverInitiated,
                         Sink respondObjectRequestSink) {
      this.objectRequestManager = objectRequestManager;
      this.clientID = clientID;
      this.requestID = requestID;
      this.ids = ids;
      this.maxRequestDepth = maxRequestDepth;
      this.requestingThreadName = requestingThreadName;
      this.serverInitiated = serverInitiated;
      this.respondObjectRequestSink = respondObjectRequestSink;

    }

    public Set getLookupIDs() {
      return ids;
    }

    public Set getNewObjectIDs() {
      return Collections.EMPTY_SET;
    }

    public void missingObject(ObjectID oid) {
      missingObjects.add(oid);
    }

    // XXX implement checkoutMap in future
    public void setResults(ObjectManagerLookupResults results) {
      objects = results.getObjects();
      if (results.getLookupPendingObjectIDs().size() > 0) {
        objectRequestManager.requestObjects(this.clientID, this.requestID, results.getLookupPendingObjectIDs(), -1,
                                            true, this.requestingThreadName);
      }
      respondObjectRequestSink.add(new ResponseContext(this.clientID, this.objects.values(), this.ids,
                                                       this.missingObjects, this.serverInitiated));
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

  }

}
