/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.logging.LogLevel;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;

/**
 * A HealthChecker Context takes care of sending and receiving probe signals, book-keeping, sending additional probes
 * and all the logic to monitor peers health. One Context per Transport is assigned as soon as a TC Connection is
 * Established.
 * 
 * @author Manoj
 */

class ConnectionHealthCheckerContextImpl implements ConnectionHealthCheckerContext {

  // state
  private static final short                     STATE_ALIVE                = 0x01;
  private static final short                     STATE_AWAIT_PINGREPLY      = 0x02;
  private static final short                     STATE_DEAD                 = 0x03;

  // Basic Ping probes
  private short                                  state;
  private final TCLogger                         logger;
  private final MessageTransportBase             transport;
  private final HealthCheckerProbeMessageFactory messageFactory;
  private final TCConnectionManager              connectionManager;
  private final int                              maxProbeCountWithoutReply;
  private final int                              maxSocketConnectCountOnProbeFail;
  private final SynchronizedLong                 probeReplyNotRecievedCount = new SynchronizedLong(0);

  // Extra Health Checks
  private HealthCheckerSocketConnect             healthCheckerSocketConnect = null;
  private int                                    socketConnectSuccessCount  = 0;

  // stats
  private final SynchronizedLong                 pingProbeSentCount         = new SynchronizedLong(0);

  public ConnectionHealthCheckerContextImpl(MessageTransportBase mtb, HealthCheckerConfig config,
                                            TCConnectionManager connMgr) {
    this.transport = mtb;
    this.messageFactory = new TransportMessageFactoryImpl();
    this.maxProbeCountWithoutReply = config.getPingProbes();
    this.maxSocketConnectCountOnProbeFail = config.getMaxSocketConnectCount();
    this.connectionManager = connMgr;
    this.logger = TCLogging.getLogger(ConnectionHealthCheckerImpl.class.getName() + ": "
                                      + config.getHealthCheckerName() + "(" + mtb.getConnectionId() + ")");
    logger.setLevel(LogLevel.DEBUG);
    changeState(STATE_ALIVE);
    logger.info("Health monitoring agent started");
  }

  private boolean isState(short checkState) {
    return (this.state == checkState);
  }

  private String stateName(short contextState) {
    switch (contextState) {
      case STATE_ALIVE:
        return "ALIVE";
      case STATE_AWAIT_PINGREPLY:
        return "WAITING_FOR_PINGREPLY";
      case STATE_DEAD:
        return "DEAD";
      default:
        return "UNKNOWN";
    }
  }

  private void changeState(short changeState) {
    if (this.state != changeState) {
      logger.debug("Context State Change: " + stateName(this.state) + " ===> " + stateName(changeState));
    }
    this.state = changeState;
  }

  private boolean canProbeAgain() {
    logger.debug("PING_REPLY not received for " + this.probeReplyNotRecievedCount + "(max allowed:"
                 + this.maxProbeCountWithoutReply + ").");
    return (this.probeReplyNotRecievedCount.get() < this.maxProbeCountWithoutReply);
  }

  // return false if the connection is dead
  public boolean sendProbe() {
    if (isState(STATE_ALIVE) || ((isState(STATE_AWAIT_PINGREPLY) && canProbeAgain()))) {
      logger.debug("Sending PING Probe to this IDLE connection");
      sendProbeMessage(this.messageFactory.createPing(transport.getConnectionId(), transport.getConnection()));
      pingProbeSentCount.increment();
      probeReplyNotRecievedCount.increment();
      changeState(STATE_AWAIT_PINGREPLY);
      return true;
    } else {
      Assert.eval(isState(STATE_DEAD) || isState(STATE_AWAIT_PINGREPLY));
      if (isState(STATE_AWAIT_PINGREPLY)) {
        logger.debug("Connection seems to be IDLE for long time. Probably DEAD.");
      }
      changeState(STATE_DEAD);
      return false;
    }
  }

  public boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (message.isPing()) {
      // Echo back but no change in this health checker state
      sendProbeMessage(this.messageFactory.createPingReply(transport.getConnectionId(), transport.getConnection()));
    } else if (message.isPingReply()) {
      // The peer is alive
      probeReplyNotRecievedCount.decrement();
      Assert.eval(probeReplyNotRecievedCount.compareTo(0) >= 0);

      if (probeReplyNotRecievedCount.compareTo(0) == 0) {
        changeState(STATE_ALIVE);
      }

      if (wasInLongGC()) {
        initSocketConnectCycle();
      }
    } else {
      // error thrown at transport layers
      return false;
    }
    return true;
  }

  private void sendProbeMessage(HealthCheckerProbeMessage message) {
    this.transport.send(message);
  }

  public long getTotalProbesSent() {
    return pingProbeSentCount.get();
  }

  public void refresh() {
    initProbeCycle();
    initSocketConnectCycle();
  }

  private void initProbeCycle() {
    probeReplyNotRecievedCount.set(0);
    changeState(STATE_ALIVE);
  }

  private void initSocketConnectCycle() {
    socketConnectSuccessCount = 0;
  }

  private boolean wasInLongGC() {
    return (socketConnectSuccessCount > 0);
  }

  public boolean doSocketConnect() {
    if (healthCheckerSocketConnect == null) {
      Assert.eval(isState(STATE_DEAD));
      initSocketConnectCycle();
      healthCheckerSocketConnect = new HealthCheckerSocketConnect(transport.getRemoteAddress(), connectionManager);
      logger.debug("Extra Cheks: detecting Long GC thru socket connect");
    }

    short socketConnectResult = healthCheckerSocketConnect.detect(); // May not give true result always as it is asynch
    if (socketConnectResult == HealthCheckerSocketConnect.SOCKET_CONNECT_PASS) {
      socketConnectSuccessCount++;
      if (socketConnectSuccessCount < maxSocketConnectCountOnProbeFail) {
        logger.info("Peer might be in Long GC. Retrying with PING Probe cycle.");
        initProbeCycle();
        return true;
      }
      logger.info("Peer might be in Long GC. But its too long. No more retries");
      return false;
    } else if (socketConnectResult == HealthCheckerSocketConnect.SOCKET_CONNECT_FAIL) {
      logger.info("Socket Connect to peer listener port failed. Probably DEAD");
      healthCheckerSocketConnect = null;
      return false;
    } else if (socketConnectResult == HealthCheckerSocketConnect.SOCKET_CONNECT_RETRY) {
      // same as SOCKET_CONNECT_PASS, but do the socket connect in the next immediate probe interval to check the async
      // connect result
      // Max retry mechanism is fit into the HealthCheckerSocketConnect. So we dont expect RETRY continuously.
      logger.info("Socket Connect to peer listener port in progress.");
      return true;
    } else {
      throw new AssertionError("HealthCheckerSocketConnect is acting wierd");
    }
  }

}
