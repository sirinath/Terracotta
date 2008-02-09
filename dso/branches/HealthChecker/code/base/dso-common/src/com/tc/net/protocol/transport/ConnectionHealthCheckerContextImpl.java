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
import com.tc.util.State;

/**
 * A HealthChecker Context takes care of sending and receiving probe signals, book-keeping, sending additional probes
 * and all the logic to monitor peers health. One Context per Transport is assigned as soon as a TC Connection is
 * Established.
 * 
 * @author Manoj
 */

class ConnectionHealthCheckerContextImpl implements ConnectionHealthCheckerContext {

  // state
  private static final State                     START                      = new State("START");
  private static final State                     ALIVE                      = new State("ALIVE");
  private static final State                     AWAIT_PINGREPLY            = new State("AWAIT_PINGREPLY");
  private static final State                     DEAD                       = new State("DEAD");

  // Basic Ping probes
  private State                                  currentState;
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
    logger.setLevel(LogLevel.DEBUG); // XXX only for testing
    currentState = START;
    logger.info("Health monitoring agent started");
  }

  /* all callers of this method are already synchronized */
  private void changeState(State newState) {
    if (logger.isDebugEnabled() && currentState != newState) {
      logger.debug("Context State Change: " + currentState.toString() + " ===> " + newState.toString());
    }
    currentState = newState;
  }

  private boolean canProbeAgain() {
    if (logger.isDebugEnabled()) {
      logger.debug("PING_REPLY not received for " + this.probeReplyNotRecievedCount + "(max allowed:"
                   + this.maxProbeCountWithoutReply + ").");
    }
    return (this.probeReplyNotRecievedCount.get() < this.maxProbeCountWithoutReply);
  }
  
  public synchronized void refresh() {
    initProbeCycle();
    initSocketConnectCycle();
  }

  // return false if the connection is dead
  public synchronized boolean sendProbe() {
    if (currentState.equals(ALIVE) || ((currentState.equals(AWAIT_PINGREPLY) && canProbeAgain()))) {
      if (logger.isDebugEnabled()) {
        logger.debug("Sending PING Probe to this IDLE connection");
      }
      sendProbeMessage(this.messageFactory.createPing(transport.getConnectionId(), transport.getConnection()));
      pingProbeSentCount.increment();
      probeReplyNotRecievedCount.increment();
      changeState(AWAIT_PINGREPLY);
      return true;
    } else {
      Assert.eval(currentState.equals(DEAD) || currentState.equals(AWAIT_PINGREPLY));
      if (logger.isDebugEnabled() && currentState.equals(AWAIT_PINGREPLY)) {
        logger.debug("Connection seems to be IDLE for long time. Probably DEAD.");
      }
      changeState(DEAD);
      return false;
    }
  }

  public synchronized boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (message.isPing()) {
      // Echo back but no change in this health checker state
      sendProbeMessage(this.messageFactory.createPingReply(transport.getConnectionId(), transport.getConnection()));
    } else if (message.isPingReply()) {
      // The peer is alive
      probeReplyNotRecievedCount.decrement();
      Assert.eval(probeReplyNotRecievedCount.compareTo(0) >= 0);

      if (probeReplyNotRecievedCount.compareTo(0) == 0) {
        changeState(ALIVE);
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


  private void initProbeCycle() {
    probeReplyNotRecievedCount.set(0);
    changeState(ALIVE);
  }

  private void initSocketConnectCycle() {
    socketConnectSuccessCount = 0;
  }

  private boolean wasInLongGC() {
    return (socketConnectSuccessCount > 0);
  }

  public synchronized boolean doSocketConnect() {
    if (healthCheckerSocketConnect == null) {
      Assert.eval(currentState.equals(DEAD));
      initSocketConnectCycle();
      healthCheckerSocketConnect = new HealthCheckerSocketConnect(transport.getRemoteAddress(), connectionManager);
      if (logger.isDebugEnabled()) {
        logger.debug("Extra Cheks: detecting Long GC thru socket connect");
      }
    }

    State socketConnectResult = healthCheckerSocketConnect.detect(); // May not give true result always as it is asynch
    if (socketConnectResult.equals(HealthCheckerSocketConnect.SOCKET_CONNECT_PASS)) {
      socketConnectSuccessCount++;
      if (socketConnectSuccessCount < maxSocketConnectCountOnProbeFail) {
        logger.info("Peer might be in Long GC. Retrying with PING Probe cycle.");
        initProbeCycle();
        return true;
      }
      logger.info("Peer might be in Long GC. But its too long. No more retries");
      return false;
    } else if (socketConnectResult.equals(HealthCheckerSocketConnect.SOCKET_CONNECT_FAIL)) {
      logger.info("Socket Connect to peer listener port failed. Probably DEAD");
      healthCheckerSocketConnect = null;
      return false;
    } else if (socketConnectResult.equals(HealthCheckerSocketConnect.SOCKET_CONNECT_RETRY)) {
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
