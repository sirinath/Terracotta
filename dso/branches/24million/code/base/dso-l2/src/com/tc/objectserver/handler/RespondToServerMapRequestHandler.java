/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
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
import com.tc.objectserver.context.EntryForKeyResponseContext;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;
import com.tc.util.ObjectIDSet;

public class RespondToServerMapRequestHandler extends AbstractEventHandler implements EventHandler {

  private final static TCLogger logger = TCLogging.getLogger(RespondToServerMapRequestHandler.class);
  private DSOChannelManager     channelManager;
  private ObjectManager         objectManager;
  private Sink                  managedObjectRequestSink;

  @Override
  public void handleEvent(final EventContext context) {
    final EntryForKeyResponseContext responseContext = (EntryForKeyResponseContext) context;

    final ObjectID mapID = responseContext.getMapID();
    final Object portableKey = responseContext.getPortableKey();

    final ManagedObject mo = responseContext.getManagedObject();
    final ManagedObjectState state = mo.getManagedObjectState();

    if (!(state instanceof ConcurrentDistributedServerMapManagedObjectState)) { throw new AssertionError(
                                                                                                         "Server Map "
                                                                                                             + mapID
                                                                                                             + " is not a ConcurrentDistributedServerMapManagedObjectState, state is of class type: " + state.getClassName()); }

    final ConcurrentDistributedServerMapManagedObjectState csmState = (ConcurrentDistributedServerMapManagedObjectState) state;

    final Object portableValue = csmState.getValueForKey(portableKey);
    // System.err.println("Server : Send response for partial key lookup : " + responseContext + " value : "
    // + portableValue);

    this.objectManager.releaseReadOnly(mo);

    final ClientID clientID = responseContext.getClientID();
    preFetchPortableValueIfNeeded(mapID, portableValue, clientID);

    MessageChannel channel;
    try {
      channel = this.channelManager.getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      logger.warn("Client " + responseContext.getClientID() + " disconnect before sending Entry for mapID : " + mapID
                  + " key : " + portableKey);
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

  @Override
  protected void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    this.objectManager = scc.getObjectManager();
    this.managedObjectRequestSink = scc.getStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE).getSink();
  }

}
