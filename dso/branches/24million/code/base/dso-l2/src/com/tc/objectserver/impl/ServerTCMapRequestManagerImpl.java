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
import com.tc.object.ServerMapRequestType;
import com.tc.object.msg.ServerTCMapResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerTCMapRequestManager;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.context.ServerMapRequestContext;
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

  public void requestValues(final ServerMapRequestID requestID, final ClientID clientID, final ObjectID mapID,
                            final Object portableKey) {

    final ServerMapRequestContext requestContext = new ServerMapRequestContext(requestID, clientID, mapID, portableKey,
                                                                               this.respondToServerTCMapSink);
    processRequest(clientID, requestContext);
  }

  public void requestSize(final ServerMapRequestID requestID, final ClientID clientID, final ObjectID mapID) {
    final ServerMapRequestContext requestContext = new ServerMapRequestContext(requestID, clientID, mapID,
                                                                               this.respondToServerTCMapSink);
    processRequest(clientID, requestContext);
  }

  private void processRequest(final ClientID clientID, final ServerMapRequestContext requestContext) {
    if (this.requestCache.add(requestContext)) {
      this.objectManager.lookupObjectsFor(clientID, requestContext);
    }
  }

  public void sendResponseFor(final ObjectID mapID, final ManagedObject managedObject) {
    final ManagedObjectState state = managedObject.getManagedObjectState();

    if (!(state instanceof ConcurrentDistributedServerMapManagedObjectState)) {
      // Formatter
      throw new AssertionError("Server Map " + mapID
                               + " is not a ConcurrentDistributedServerMapManagedObjectState, state is of class type: "
                               + state.getClassName());
    }

    final ConcurrentDistributedServerMapManagedObjectState csmState = (ConcurrentDistributedServerMapManagedObjectState) state;
    try {
      final List<ServerMapRequestContext> requestList = this.requestCache.remove(mapID);

      if (requestList == null) { throw new AssertionError("Looked up : " + managedObject
                                                          + " But no request pending for it : " + this.requestCache); }

      for (final ServerMapRequestContext request : requestList) {

        final ServerMapRequestType requestType = request.getRequestType();
        switch (requestType) {
          case GET_SIZE:
            sendResponseForGetSize(mapID, request, csmState);
            break;
          case GET_VALUE_FOR_KEY:
            sendResponseForGetValue(mapID, request, csmState);
            break;
          default:
            throw new AssertionError("Unknown request type : " + requestType);
        }
      }
    } finally {
      // TODO::FIXME::Release as soon as possible
      this.objectManager.releaseReadOnly(managedObject);
    }
  }

  private void sendResponseForGetValue(final ObjectID mapID, final ServerMapRequestContext request,
                                       final ConcurrentDistributedServerMapManagedObjectState csmState) {
    final ServerMapRequestID requestID = request.getRequestID();
    final Object portableKey = request.getPortableKey();
    final ClientID clientID = request.getClientID();
    final Object portableValue = csmState.getValueForKey(portableKey);

    preFetchPortableValueIfNeeded(mapID, portableValue, clientID);

    final MessageChannel channel = getActiveChannel(clientID);
    if (channel == null) { return; }

    final ServerTCMapResponseMessage responseMessage = (ServerTCMapResponseMessage) channel
        .createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE);
    responseMessage.initializeGetValueResponse(mapID, requestID, portableValue);
    responseMessage.send();
  }

  private void sendResponseForGetSize(final ObjectID mapID, final ServerMapRequestContext request,
                                      final ConcurrentDistributedServerMapManagedObjectState csmState) {
    final ServerMapRequestID requestID = request.getRequestID();
    final ClientID clientID = request.getClientID();
    final Integer size = csmState.getSize();

    final MessageChannel channel = getActiveChannel(clientID);
    if (channel == null) { return; }

    final ServerTCMapResponseMessage responseMessage = (ServerTCMapResponseMessage) channel
        .createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE);
    responseMessage.initializeGetSizeResponse(mapID, requestID, size);
    responseMessage.send();
  }

  private MessageChannel getActiveChannel(final ClientID clientID) {
    try {
      return this.channelManager.getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      this.logger.warn("Client " + clientID + " disconnect before sending Response for ServeMap Request ");
      return null;
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

    private final Map<ObjectID, List<ServerMapRequestContext>> serverMapRequestMap = new HashMap<ObjectID, List<ServerMapRequestContext>>();

    public synchronized boolean add(final ServerMapRequestContext context) {
      boolean newEntry = false;
      final ObjectID mapID = context.getServerTCMapID();
      List<ServerMapRequestContext> requestList = this.serverMapRequestMap.get(mapID);
      if (requestList == null) {
        requestList = new ArrayList<ServerMapRequestContext>();
        this.serverMapRequestMap.put(mapID, requestList);
        newEntry = true;
      }
      requestList.add(context);
      return newEntry;
    }

    public synchronized List<ServerMapRequestContext> remove(final ObjectID mapID) {
      return this.serverMapRequestMap.remove(mapID);
    }
  }

}
