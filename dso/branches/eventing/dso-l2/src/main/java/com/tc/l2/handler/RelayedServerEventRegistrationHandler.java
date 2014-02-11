/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.l2.msg.RelayedServerEventRegistrationMessage;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.objectserver.event.ServerEventRegistry;

public class RelayedServerEventRegistrationHandler extends AbstractEventHandler {
  private static final TCLogger     LOG = TCLogging.getLogger(RelayedServerEventRegistrationHandler.class);
  private final ServerEventRegistry serverEventRegistry;

  public RelayedServerEventRegistrationHandler(ServerEventRegistry serverEventRegistry) {
    this.serverEventRegistry = serverEventRegistry;
  }

  @Override
  public void handleEvent(EventContext context) throws EventHandlerException {
    if (context instanceof RelayedServerEventRegistrationMessage) {
      RelayedServerEventRegistrationMessage msg = (RelayedServerEventRegistrationMessage) context;
      if (msg.getNodeID().getNodeType() == NodeID.CLIENT_NODE_TYPE) {
      if (msg.isRegisterationMessage()) {
        LOG.debug("ServerEvent listener registration message from client [" + msg.getNodeID() + "] has been received: " + context);
        LOG.debug("Destination: " + msg.getDestination() + ", event types: " + msg.getEventTypes());
          serverEventRegistry.register((ClientID) msg.getNodeID(), msg.getDestination(), msg.getEventTypes());
      } else {
        LOG.debug("ServerEvent listener unregistration message from client [" + msg.getNodeID() + "] has been received: " + context);
        LOG.debug("Destination: " + msg.getDestination() + ", event types: " + msg.getEventTypes());

          serverEventRegistry.unregister((ClientID) msg.getNodeID(), msg.getDestination(), msg.getEventTypes());
      }
      }
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }

  }

}
