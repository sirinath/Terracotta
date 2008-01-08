/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.Message;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.util.concurrent.SetOnceFlag;

public final class TunnelingMessageConnection implements MessageConnection {

  private static final boolean  DEBUG     = Boolean.getBoolean(TunnelingMessageConnection.class.getName().replace('/',
                                                                                                                  '.')
                                                               + ".DEBUG");

  private static final TCLogger logger    = TCLogging.getLogger(TunnelingMessageConnection.class);

  private final LinkedList      inbox;
  private final MessageChannel  channel;
  private final boolean         isJmxConnectionServer;
  private final SetOnceFlag     connected = new SetOnceFlag();
  private final SetOnceFlag     closed    = new SetOnceFlag();

  /**
   * @param channel outgoing network channel, calls to {@link #writeMessage(Message)} will drop messages here and send
   *        to the other side
   */
  public TunnelingMessageConnection(final MessageChannel channel, boolean isJmxConnectionServer) {
    this.isJmxConnectionServer = isJmxConnectionServer;
    this.inbox = new LinkedList();
    this.channel = channel;
  }

  public synchronized void close() {
    if (closed.attemptSet()) {
      DEBUG("closing with queue " + inbox);
      inbox.clear();
      notifyAll();
    } else {
      DEBUG("already closed");
    }
  }

  private void DEBUG(String msg) {
    if (DEBUG) logger.warn(channel.getChannelID() + " " + msg, new Throwable());
  }

  public synchronized void connect(final Map environment) {
    if (connected.attemptSet()) {
      if (!isJmxConnectionServer) {
        JmxRemoteTunnelMessage connectMessage = (JmxRemoteTunnelMessage) channel
            .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
        connectMessage.setInitConnection();
        connectMessage.send();
      }
    }
  }

  public String getConnectionId() {
    return channel.getRemoteAddress().getStringForm();
  }

  public synchronized Message readMessage() throws IOException {
    while (inbox.isEmpty()) {
      if (closed.isSet()) {
        DEBUG("connection closed while reading");
        throw new IOException("connection closed");
      }
      try {
        wait();
      } catch (InterruptedException ie) {
        throw new IOException("Interrupted while waiting for inbound message");
      }
    }

    Message rv = (Message) inbox.removeFirst();

    DEBUG("returning message " + rv.getClass());

    return rv;
  }

  public synchronized void writeMessage(final Message outboundMessage) throws IOException {
    if (closed.isSet()) {
      DEBUG("not sending message of " + outboundMessage.getClass());
      throw new IOException("connection closed");
    }

    DEBUG("sent message of " + outboundMessage.getClass());

    JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) channel
        .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
    messageEnvelope.setTunneledMessage(outboundMessage);
    messageEnvelope.send();
  }

  /**
   * This should only be invoked from the SEDA event handler that receives incoming network messages.
   */
  synchronized void incomingNetworkMessage(final Message inboundMessage) {
    if (closed.isSet()) {
      DEBUG("dropping incoming message of " + inboundMessage.getClass());
      return;
    }

    DEBUG("received message of " + inboundMessage.getClass());
    inbox.addLast(inboundMessage);
    notifyAll();
  }

}
