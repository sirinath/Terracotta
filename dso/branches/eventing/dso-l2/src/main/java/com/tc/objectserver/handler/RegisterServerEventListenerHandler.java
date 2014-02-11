package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.api.L2Coordinator;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.msg.RegisterServerEventListenerMessage;
import com.tc.object.msg.UnregisterServerEventListenerMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.event.ServerEventRegistry;
import com.tc.util.Assert;

/**
 * A stage handler for {@link ServerConfigurationContext#REGISTER_SERVER_EVENT_LISTENER_STAGE}.
 *
 * @author Eugene Shelestovich
 */
public class RegisterServerEventListenerHandler extends AbstractEventHandler {

  private static final TCLogger LOG = TCLogging.getLogger(RegisterServerEventListenerHandler.class);

  private final ServerEventRegistry serverEventRegistry;

  private L2Coordinator                      l2Coordinator;

  public RegisterServerEventListenerHandler(final ServerEventRegistry serverEventRegistry) {
    this.serverEventRegistry = serverEventRegistry;
  }

  @Override
  public synchronized void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    l2Coordinator = scc.getL2Coordinator();
  }

  @Override
  public void handleEvent(final EventContext context) {
    final NodeID nodeId = ((TCMessage)context).getSourceNodeID();
    if (nodeId.getNodeType() == NodeID.CLIENT_NODE_TYPE) {
      final ClientID clientId = (ClientID)nodeId;
      if (context instanceof RegisterServerEventListenerMessage) {
        final RegisterServerEventListenerMessage msg = (RegisterServerEventListenerMessage)context;
        serverEventRegistry.register(clientId, msg.getDestination(), msg.getEventTypes());
        LOG.debug("Server event listener registration message from client [" + nodeId + "] has been received: " + context);
        LOG.debug("Destination: " + msg.getDestination() + ", event types: " + msg.getEventTypes());
        l2Coordinator.relayServerEventRegistrationToPassive(msg);
      } else if (context instanceof UnregisterServerEventListenerMessage) {
        final UnregisterServerEventListenerMessage msg = (UnregisterServerEventListenerMessage)context;
        serverEventRegistry.unregister(clientId, msg.getDestination(), msg.getEventTypes());
        LOG.debug("Server event listener unregistration message from client [" + nodeId + "] has been received: " + context);
        LOG.debug("Destination: " + msg.getDestination() + ", event types: " + msg.getEventTypes());
        l2Coordinator.relayServerEventDeregistrationToPassive(msg);
      } else {
        Assert.fail("Unknown event type " + context.getClass().getName());
      }
    }


  }

}
