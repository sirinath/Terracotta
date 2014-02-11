/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.RelayedServerEventRegistrationMessage;
import com.tc.l2.objectserver.L2ObjectState;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.object.msg.RegisterServerEventListenerMessage;
import com.tc.object.msg.UnregisterServerEventListenerMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Iterator;

public class ServerEventRegistrationRelayHandler extends AbstractEventHandler {
  private static final TCLogger                logger = TCLogging.getLogger(ServerEventRegistrationRelayHandler.class);

  private final L2ObjectStateManager           l2ObjectStateMgr;
  private GroupManager                         groupManager;



  public ServerEventRegistrationRelayHandler(final L2ObjectStateManager objectStateManager) {
    this.l2ObjectStateMgr = objectStateManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final RegisterServerEventListenerMessage message = (RegisterServerEventListenerMessage) context;
    final Collection states = this.l2ObjectStateMgr.getL2ObjectStates();
    for (final Iterator i = states.iterator(); i.hasNext();) {
      final L2ObjectState state = (L2ObjectState) i.next();
      final NodeID nodeID = state.getNodeID();
      sendMessage(nodeID, message);
    }
  }

  private void sendMessage(final NodeID passiveNodeId, final EventContext context) {
    GroupMessage groupMessage = null;
    if(context instanceof RegisterServerEventListenerMessage) {
      final RegisterServerEventListenerMessage message = (RegisterServerEventListenerMessage) context;
      groupMessage = new RelayedServerEventRegistrationMessage(RelayedServerEventRegistrationMessage.REGISTER,
                                                               message.getSourceNodeID(),
                                                         message.getDestination(), message.getEventTypes());
    } else if (context instanceof UnregisterServerEventListenerMessage) {
      final UnregisterServerEventListenerMessage message = (UnregisterServerEventListenerMessage) context;
      groupMessage = new RelayedServerEventRegistrationMessage(RelayedServerEventRegistrationMessage.UNREGISTER,
                                                               message.getSourceNodeID(),
                                                         message.getDestination(), message.getEventTypes());
    } else {
      Assert.fail("Unknown event type " + context.getClass().getName());
    }
    
    try {
      this.groupManager.sendTo(passiveNodeId, groupMessage);
    } catch (final Exception e) {
      logger.error("Removing " + passiveNodeId + " from group because of Exception :", e);
      this.groupManager.zapNode(passiveNodeId, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error relaying server event registration message"
                                    + L2HAZapNodeRequestProcessor.getErrorString(e));
    }
  }


  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.groupManager = oscc.getL2Coordinator().getGroupManager();
  }
}
