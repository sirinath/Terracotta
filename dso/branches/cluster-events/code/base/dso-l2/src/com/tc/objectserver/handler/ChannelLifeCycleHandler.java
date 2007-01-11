/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.util.concurrent.ThreadUtil;

/**
 * @author steve
 */
public class ChannelLifeCycleHandler extends AbstractEventHandler {
  private final ServerTransactionManager transactionManager;
  private final TransactionBatchManager  transactionBatchManager;
  private TCLogger                       logger;
  private final CommunicationsManager    commsManager;
  private final DSOChannelManager        channelMgr;

  public ChannelLifeCycleHandler(CommunicationsManager commsManager, ServerTransactionManager transactionManager,
                                 TransactionBatchManager transactionBatchManager, DSOChannelManager channelManager) {
    this.commsManager = commsManager;
    this.transactionManager = transactionManager;
    this.transactionBatchManager = transactionBatchManager;
    this.channelMgr = channelManager;
  }

  public void handleEvent(EventContext context) {
    ChannelEvent event = (ChannelEvent) context;
    ChannelID channelID = event.getChannelID();
    if (ChannelEventType.TRANSPORT_DISCONNECTED_EVENT.matches(event)) {
      broadcastClusterMemebershipMessage(ClusterMembershipMessage.EventType.NODE_DISCONNECTED, channelID);
      if (commsManager.isInShutdown()) {
        logger.info("Ignoring transport disconnect for " + channelID + " while shutting down.");
      } else {
        // Giving 0.5 sec for the server to catch up with any pending transactions. Not a fool prove mechanism.
        ThreadUtil.reallySleep(500);
        logger.info("Received transport disconnect.  Killing client " + channelID);
        transactionManager.shutdownClient(channelID);
        transactionBatchManager.shutdownClient(channelID);
      }
    } else if (ChannelEventType.TRANSPORT_CONNECTED_EVENT.matches(event)) {
      broadcastClusterMemebershipMessage(ClusterMembershipMessage.EventType.NODE_CONNECTED, channelID);
    }
  }

  private void broadcastClusterMemebershipMessage(int eventType, ChannelID channelID) {
    MessageChannel[] channels = channelMgr.getChannels();
    for (int i = 0; i < channels.length; i++) {
      MessageChannel channel = channels[i];
      ClusterMembershipMessage cmm = (ClusterMembershipMessage) channel
          .createMessage(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE);
      cmm.initialize(eventType, channelID, channels);
      cmm.send();
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.logger = scc.getLogger(ChannelLifeCycleHandler.class);
  }

}
