/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.remote.connect;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.JMXConnectStateMachine;
import com.tc.management.remote.protocol.terracotta.L1ConnectionMessage;
import com.tc.management.remote.protocol.terracotta.TunneledDomainsChanged;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.statistics.StatisticsGateway;

public class ClientConnectEventHandler extends AbstractEventHandler {
  private static final TCLogger LOGGER = TCLogging.getLogger(ClientConnectEventHandler.class);

  private final StatisticsGateway statisticsGateway;

  public ClientConnectEventHandler(final StatisticsGateway statisticsGateway) {
    this.statisticsGateway = statisticsGateway;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof L1ConnectionMessage) {
      L1ConnectionMessage msg = (L1ConnectionMessage) context;
      if (msg.isConnectingMsg()) {
        addJmxConnection(msg);
      } else {
        removeJmxConnection(msg);
      }
    } else if (context instanceof TunneledDomainsChanged) {
      TunneledDomainsChanged msg = (TunneledDomainsChanged) context;
      JMXConnectStateMachine state = (JMXConnectStateMachine) msg.getChannel()
          .getAttachment(ClientTunnelingEventHandler.STATE_ATTACHMENT);
      state.tunneledDomainsChanged(msg.getTunneledDomains());
    } else {
      LOGGER.error("Unknown event context : " + context + " (" + context.getClass() + ")");
    }
  }

  private void addJmxConnection(final L1ConnectionMessage msg) {
    final long start = System.currentTimeMillis();

    LOGGER.info("addJmxConnection(" + msg.getChannel().getChannelID() + ")");

    JMXConnectStateMachine state = (JMXConnectStateMachine) msg.getChannel()
        .getAttachment(ClientTunnelingEventHandler.STATE_ATTACHMENT);

    state.initClientBeanBag(msg, statisticsGateway);

    LOGGER.info("addJmxConnection(" + msg.getChannel().getChannelID() + ") completed in "
                + (System.currentTimeMillis() - start) + " millis");
  }

  private void removeJmxConnection(final L1ConnectionMessage msg) {
    final MessageChannel channel = msg.getChannel();

    JMXConnectStateMachine state = (JMXConnectStateMachine) channel
        .getAttachment(ClientTunnelingEventHandler.STATE_ATTACHMENT);

    if (state != null) {
      state.disconnect();
    }
    statisticsGateway.removeStatisticsAgent(channel.getChannelID());
  }

}
