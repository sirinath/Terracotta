/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;

import java.io.IOException;

import javax.management.remote.JMXConnector;

public class JMXConnectStateMachine {

  private static final TCLogger LOGGER = TCLogging.getLogger(JMXConnectStateMachine.class);

  private enum State {
    INITIAL, CONNECTED, DISCONNECTED;
  }

  private State                      state;
  private TunnelingMessageConnection tmc;
  private JMXConnector               jmxConnector;
  private ChannelID                  channelId;

  public JMXConnectStateMachine() {
    this.state = JMXConnectStateMachine.State.INITIAL;
  }

  public synchronized boolean connect(ChannelID cid, TunnelingMessageConnection connection, JMXConnector connector) {
    if (state != State.INITIAL) { return false; }

    state = State.CONNECTED;

    close();

    this.channelId = cid;
    this.tmc = connection;
    this.jmxConnector = connector;

    return true;
  }

  public synchronized boolean disconnect() {
    close();

    boolean rv = state == State.DISCONNECTED;
    state = State.DISCONNECTED;
    return rv;
  }

  private void close() {
    if (tmc != null) {
      try {
        tmc.close();
      } catch (Throwable t) {
        LOGGER.error("unhandled exception closing TunnelingMessageConnection for " + channelId, t);
      } finally {
        tmc = null;
      }
    }

    if (jmxConnector != null) {
      try {
        jmxConnector.close();
      } catch (IOException ioe) {
        LOGGER.debug("Unable to close JMX connector to " + channelId, ioe);
      } catch (Throwable t) {
        LOGGER.error("unhandled exception closing JMX connector for " + channelId, t);
      } finally {
        jmxConnector = null;
      }
    }

    channelId = null;
  }

  public synchronized TunnelingMessageConnection getTunnelingMessageConnection() {
    return tmc;
  }

}
