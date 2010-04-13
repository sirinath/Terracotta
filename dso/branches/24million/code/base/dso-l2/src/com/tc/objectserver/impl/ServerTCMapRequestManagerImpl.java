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

public class ServerTCMapRequestManagerImpl implements ServerTCMapRequestManager {

  private final TCLogger          logger = TCLogging.getLogger(ServerTCMapRequestManagerImpl.class);
  private final ObjectManager     objectManager;
  private final DSOChannelManager channelManager;
  private final Sink              respondToServerTCMapSink;
  private final Sink              managedObjectRequestSink;

  public ServerTCMapRequestManagerImpl(final ObjectManager objectManager, final DSOChannelManager channelManager,
                                       final Sink respondToServerTCMapSink, final Sink managedObjectRequestSink) {
    this.channelManager = channelManager;
    this.objectManager = objectManager;
    this.respondToServerTCMapSink = respondToServerTCMapSink;
    this.managedObjectRequestSink = managedObjectRequestSink;
  }

  public void requestValues(ClientID clientID, ObjectID mapID, Object portableKey) {

    final RequestEntryForKeyContext requestContext = new RequestEntryForKeyContext(clientID, mapID, portableKey,
                                                                                   this.respondToServerTCMapSink);
    this.objectManager.lookupObjectsFor(clientID, requestContext);

  }

  public void sendValues(ClientID clientID, ObjectID mapID, ManagedObject managedObject, Object portableKey) {
    final ManagedObjectState state = managedObject.getManagedObjectState();

    if (!(state instanceof ConcurrentDistributedServerMapManagedObjectState)) { throw new AssertionError(
                                                                                                         "Server Map "
                                                                                                             + mapID
                                                                                                             + " is not a ConcurrentDistributedServerMapManagedObjectState, state is of class type: "
                                                                                                             + state
                                                                                                                 .getClassName()); }

    final ConcurrentDistributedServerMapManagedObjectState csmState = (ConcurrentDistributedServerMapManagedObjectState) state;

    final Object portableValue = csmState.getValueForKey(portableKey);
    // System.err.println("Server : Send response for partial key lookup : " + responseContext + " value : "
    // + portableValue);

    this.objectManager.releaseReadOnly(managedObject);

    preFetchPortableValueIfNeeded(mapID, portableValue, clientID);

    MessageChannel channel;
    try {
      channel = this.channelManager.getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      logger.warn("Client " + clientID + " disconnect before sending Entry for mapID : " + mapID + " key : "
                  + portableKey);
      return;
    }

    final ServerTCMapResponseMessage responseMessage = (ServerTCMapResponseMessage) channel
        .createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE);
    responseMessage.initialize(mapID, portableKey, portableValue);
    responseMessage.send();
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

}
