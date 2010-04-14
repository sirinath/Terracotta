/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.msg.ServerTCMapResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerTCMapRequestManager;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.context.RequestEntryForKeyContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerTCMapRequestManagerImpl implements ServerTCMapRequestManager {

  private final TCLogger                logger       = TCLogging.getLogger(ServerTCMapRequestManagerImpl.class);
  private final ObjectManager           objectManager;
  private final DSOChannelManager       channelManager;
  private final Sink                    respondToServerTCMapSink;
  private final Sink                    managedObjectRequestSink;
  private final ServerTCMapRequestCache requestCache = new ServerTCMapRequestCache();

  public ServerTCMapRequestManagerImpl(final ObjectManager objectManager, final DSOChannelManager channelManager,
                                       final Sink respondToServerTCMapSink, final Sink managedObjectRequestSink) {
    this.channelManager = channelManager;
    this.objectManager = objectManager;
    this.respondToServerTCMapSink = respondToServerTCMapSink;
    this.managedObjectRequestSink = managedObjectRequestSink;
  }

  public void requestValues(ServerMapRequestID requestID, ClientID clientID, ObjectID mapID, Object portableKey) {

    final RequestEntryForKeyContext requestContext = new RequestEntryForKeyContext(requestID, clientID, mapID,
                                                                                   portableKey,
                                                                                   this.respondToServerTCMapSink);
    if (requestCache.add(requestContext)) {
      this.objectManager.lookupObjectsFor(clientID, requestContext);
    }

  }

  public void sendValues(ObjectID mapID, ManagedObject managedObject) {
    final ManagedObjectState state = managedObject.getManagedObjectState();

    if (!(state instanceof ConcurrentDistributedServerMapManagedObjectState)) { throw new AssertionError(
                                                                                                         "Server Map "
                                                                                                             + mapID
                                                                                                             + " is not a ConcurrentDistributedServerMapManagedObjectState, state is of class type: "
                                                                                                             + state
                                                                                                                 .getClassName()); }

    final ConcurrentDistributedServerMapManagedObjectState csmState = (ConcurrentDistributedServerMapManagedObjectState) state;
    try {
      List<RequestEntryForKeyContext> requestList = requestCache.remove(mapID);

      if (requestList != null) {
        for (RequestEntryForKeyContext request : requestList) {

          Object portableKey = request.getPortableKey();
          ClientID clientID = request.getClientID();
          final Object portableValue = csmState.getValueForKey(portableKey);
          // System.err.println("Server : Send response for partial key lookup : " + responseContext + " value : "
          // + portableValue);

          preFetchPortableValueIfNeeded(mapID, portableValue, clientID);

          MessageChannel channel;
          try {
            channel = this.channelManager.getActiveChannel(clientID);
          } catch (final NoSuchChannelException e) {
            logger.warn("Client " + clientID + " disconnect before sending Entry for mapID : " + mapID + " key : "
                        + portableKey);
            continue;
          }

          final ServerTCMapResponseMessage responseMessage = (ServerTCMapResponseMessage) channel
              .createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE);
          responseMessage.initialize(mapID, portableKey, portableValue);
          responseMessage.send();
        }
      }
    } finally {
      this.objectManager.releaseReadOnly(managedObject);
    }
  }

  private void preFetchPortableValueIfNeeded(final ObjectID mapID, final Object portableValue, final ClientID clientID) {
    if (portableValue instanceof ObjectID) {
      final ObjectID valueID = (ObjectID) portableValue;
      if (mapID.getGroupID() != valueID.getGroupID()) {
        // TODO::FIX for AA
        // Not in this server
        return;
      }
      final ObjectIDSet lookupIDs = new ObjectIDSet();
      lookupIDs.add(valueID);
      this.managedObjectRequestSink.add(new ObjectRequestServerContextImpl(clientID, ObjectRequestID.NULL_ID,
                                                                           lookupIDs, Thread.currentThread().getName(),
                                                                           -1, true));
    }
  }

  private final static class ServerTCMapRequestCache {

    private final Map<ObjectID, List<RequestEntryForKeyContext>> serverMapRequestMap = new HashMap<ObjectID, List<RequestEntryForKeyContext>>();

    public synchronized boolean add(RequestEntryForKeyContext context) {
      boolean newEntry = false;
      ObjectID mapID = context.getServerTCMapID();
      List<RequestEntryForKeyContext> requestList = serverMapRequestMap.get(mapID);
      if (requestList == null) {
        requestList = new ArrayList<RequestEntryForKeyContext>();
        serverMapRequestMap.put(mapID, requestList);
        newEntry = true;
      }
      requestList.add(context);
      return newEntry;
    }

    public synchronized List<RequestEntryForKeyContext> remove(ObjectID mapID) {
      return serverMapRequestMap.remove(mapID);
    }
  }

}
