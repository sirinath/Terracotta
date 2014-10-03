/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.util.Assert;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

public class ClientTunnelingEventHandler extends AbstractEventHandler implements DSOChannelManagerEventListener {

  public static final String    STATE_ATTACHMENT = ClientTunnelingEventHandler.class.getName() + ".STATE_ATTACHMENT";

  private static final TCLogger logger           = TCLogging.getLogger(ClientTunnelingEventHandler.class);

  private final MBeanServer     l2MBeanServer;
  private final Object          sinkLock;
  private Sink                  connectStageSink;
  private Sink                  disconnectStageSink;

  public ClientTunnelingEventHandler() {
    l2MBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
    sinkLock = new Object();
  }

  @Override
  public void handleEvent(final EventContext context) {
    setupJMXStateMachine(((TCMessage) context).getChannel());

    if (context instanceof L1JmxReady) {
      final L1JmxReady readyMessage = (L1JmxReady) context;
      connectToL1JmxServer(readyMessage);
    } else if (context instanceof TunneledDomainsChanged) {
      final TunneledDomainsChanged message = (TunneledDomainsChanged) context;
      synchronized (sinkLock) {
        if (connectStageSink == null) { throw new AssertionError("ConnectStageSink was not set."); }
        connectStageSink.add(message);
      }
    } else {
      final JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) context;
      if (messageEnvelope.getCloseConnection()) {
        channelRemoved(messageEnvelope.getChannel());
      } else if (messageEnvelope.getInitConnection()) {
        logger.warn("Received a JMX tunneled connection init from the remote"
                    + " JMX server, only the JMX client should do this");
      } else {
        routeTunneledMessage(messageEnvelope);
      }
    }
  }

  private void connectToL1JmxServer(final L1JmxReady readyMessage) {
    logger.info("L1[" + readyMessage.getChannelID() + "] notified us that their JMX server is now available");

    EventContext msg = new L1ConnectionMessage.Connecting(l2MBeanServer, readyMessage.getChannel(),
                                                          readyMessage.getUUID(), readyMessage.getTunneledDomains());
    synchronized (sinkLock) {
      if (connectStageSink == null) { throw new AssertionError("ConnectStageSink was not set."); }
      connectStageSink.add(msg);
    }
  }

  private static void setupJMXStateMachine(MessageChannel channel) {
    if (channel.getAttachment(STATE_ATTACHMENT) == null) {
      channel.addAttachment(STATE_ATTACHMENT, new JMXConnectStateMachine(), false);
    }
  }

  private void routeTunneledMessage(final JmxRemoteTunnelMessage messageEnvelope) {
    JMXConnectStateMachine state = (JMXConnectStateMachine) messageEnvelope.getChannel()
        .getAttachment(STATE_ATTACHMENT);

    if (state != null) {
      state.incomingNetworkMessage(messageEnvelope);
    } else {
      logger.warn("No jmx connect state present for channel " + messageEnvelope.getChannelID());
    }
  }

  @Override
  public void channelCreated(final MessageChannel channel) {
    // DEV-16: Instead of immediately interrogating an L1's JMX server as soon as it connects, we wait for the L1 client
    // to send us a 'L1JmxReady' network message to avoid a startup race condition
  }

  @Override
  public void channelRemoved(final MessageChannel channel) {
    EventContext msg = new L1ConnectionMessage.Disconnecting(channel);

    synchronized (sinkLock) {
      if (disconnectStageSink == null) { throw new AssertionError("DisconnectStageSink was not set."); }
      disconnectStageSink.add(msg);
    }
  }

  public void setStages(Sink connectSink, Sink disconnectSink) {
    // There are two unique sinks because disconnects might need to unblock
    // the actions taken on connect. The known example of this is that when
    // the connect event might block trying to lookup beans on the L1 if it
    // disconnects very quickly. In this case, the stage thread would hang
    // forever and the disconnect event that could un-hang it was waiting
    // in the queue. Using two stages solves this issue
    Assert.assertFalse(connectSink == disconnectSink);

    synchronized (sinkLock) {
      if ((connectStageSink != null) || (disconnectStageSink != null)) {
        //
        throw new AssertionError("attempt to set stages more than once");
      }
      connectStageSink = connectSink;
      disconnectStageSink = disconnectSink;
    }
  }

}
