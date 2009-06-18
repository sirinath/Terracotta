/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.msg.RespondToKeyValueMappingRequestMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.EntryForKeyResponseContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.managedobject.ConcurrentStringMapManagedObjectState;

public class RespondToPartialKeysRequestHandler extends AbstractEventHandler implements EventHandler {

  private final static TCLogger logger = TCLogging.getLogger(RespondToPartialKeysRequestHandler.class);
  private DSOChannelManager     channelManager;
  private ObjectManager         objectManager;

  @Override
  public void handleEvent(final EventContext context) {
    EntryForKeyResponseContext responseContext = (EntryForKeyResponseContext) context;

    ObjectID mapID = responseContext.getMapID();
    Object portableKey = responseContext.getPortableKey();

    ManagedObject mo = responseContext.getManagedObject();
    ManagedObjectState state = mo.getManagedObjectState();

    if (!(state instanceof ConcurrentStringMapManagedObjectState)) { throw new AssertionError(
                                                                                              " Map "
                                                                                                  + mapID
                                                                                                  + " is not a ConcurrentStringMap ManagedObjectState."); }

    ConcurrentStringMapManagedObjectState csmState = (ConcurrentStringMapManagedObjectState) state;

    Object portableValue = csmState.getValueForKey(portableKey);
    System.err.println("Server : Send response for partial key lookup : " + responseContext + " value : "
                       + portableValue);

    this.objectManager.releaseReadOnly(mo);

    MessageChannel channel;
    try {
      channel = this.channelManager.getActiveChannel(responseContext.getClientID());
    } catch (NoSuchChannelException e) {
      logger.warn("Client " + responseContext.getClientID() + " disconnect before sending Entry for mapID : " + mapID
                  + " key : " + portableKey);
      return;
    }

    RespondToKeyValueMappingRequestMessage responseMessage = (RespondToKeyValueMappingRequestMessage) channel
        .createMessage(TCMessageType.KEY_VALUE_MAPPING_RESPONSE_MESSAGE);
    responseMessage.initialize(mapID, portableKey, portableValue);
    responseMessage.send();

  }

  @Override
  protected void initialize(final ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    this.objectManager = scc.getObjectManager();
  }

}
