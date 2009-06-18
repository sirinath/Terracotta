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
import com.tc.objectserver.context.RequestEntryForKeyContext;
import com.tc.objectserver.context.RespondToObjectRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.managedobject.ConcurrentStringMapManagedObjectState;

import java.util.Collection;

public class RespondToPartialKeysRequestHandler extends AbstractEventHandler implements EventHandler {

  private final static TCLogger logger = TCLogging.getLogger(RespondToPartialKeysRequestHandler.class);
  private DSOChannelManager     channelManager;
  private ObjectManager         objectManager;

  @Override
  public void handleEvent(EventContext context) {
    RespondToObjectRequestContext respondContext = (RespondToObjectRequestContext) context;
    RequestEntryForKeyContext requestContext = (RequestEntryForKeyContext) respondContext.getRequestContext();

    ObjectID mapID = requestContext.getPartialKeyMapID();
    Object portableKey = requestContext.getPortableKey();

    if (!respondContext.getMissingObjectIDs().isEmpty()) {
      logger.error("Ignoring Missing ObjectIDs : " + respondContext.getMissingObjectIDs() + " Map ID : " + mapID
                   + " portable key : " + portableKey);
      // TODO:: Fix this
      return;
    }

    Collection objects = respondContext.getObjs();
    ManagedObject mo = (ManagedObject) objects.iterator().next();

    if (!mo.getID().equals(mapID) || objects.size() != 0) { throw new AssertionError("Partial Keys Map (mapID " + mapID
                                                                                     + " ) is not looked up or "); }

    ManagedObjectState state = mo.getManagedObjectState();

    if (!(state instanceof ConcurrentStringMapManagedObjectState)) { throw new AssertionError(
                                                                                              " Map "
                                                                                                  + mapID
                                                                                                  + " is not a ConcurrentStringMap ManagedObjectState."); }

    ConcurrentStringMapManagedObjectState csmState = (ConcurrentStringMapManagedObjectState) state;

    Object portableValue = csmState.getValueForKey(portableKey);

    this.objectManager.releaseReadOnly(mo);

    MessageChannel channel;
    try {
      channel = this.channelManager.getActiveChannel(respondContext.getRequestedNodeID());
    } catch (NoSuchChannelException e) {
      logger.warn("Client " + respondContext.getRequestedNodeID() + " disconnect before sending Entry for mapID : "
                  + mapID + " key : " + portableKey);
      return;
    }

    RespondToKeyValueMappingRequestMessage responseMessage = (RespondToKeyValueMappingRequestMessage) channel
        .createMessage(TCMessageType.KEY_VALUE_MAPPING_RESPONSE_MESSAGE);
    responseMessage.initialize(mapID, portableKey, portableValue);
    responseMessage.send();

  }

  @Override
  protected void initialize(ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    this.objectManager = scc.getObjectManager();
  }

}
