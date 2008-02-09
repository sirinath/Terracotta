/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.util.Assert;
import com.tc.util.State;

import java.io.IOException;

/**
 * When the peer node doesn't reply for the PING probes, an extra check(on demand) is made to make sure if it is really
 * dead. Today's heuristic to detect the Long GC is to connect to some of the peer listener ports. If it succeeds, we
 * will cycle again the probe sends.
 * 
 * @author Manoj
 */
public class HealthCheckerSocketConnect implements TCConnectionEventListener {

  private final TCSocketAddress     peerNodeAddr;
  private final TCConnectionManager connectionManager;
  private TCConnection              conn                         = null;
  private State                     currentState;

  private static final State        ASYNC_CONNECT_PREINIT        = new State("ASYNC_CONNECT_PREINIT");
  private static final State        ASYNC_CONNECT_INITED         = new State("ASYNC_CONNECT_INITED");
  private static final State        ASYNC_CONNECT_FAILED         = new State("ASYNC_CONNECT_FAILED");
  private static final State        ASYNC_CONNECT_PASSED         = new State("ASYNC_CONNECT_PASSED");

  public static final State         SOCKET_CONNECT_PASS          = new State("SOCKET_CONNECT_PASS");
  public static final State         SOCKET_CONNECT_FAIL          = new State("SOCKET_CONNECT_FAIL");
  public static final State         SOCKET_CONNECT_RETRY         = new State("SOCKET_CONNECT_RETRY");

  public static final int           MAX_ASYNC_CONNECT_WAIT_CYCLE = 2;
  private short                     asyncConnectWaitCycleCount   = 0;

  public HealthCheckerSocketConnect(TCSocketAddress peerNode, TCConnectionManager connMgr) {
    this.connectionManager = connMgr;
    this.peerNodeAddr = peerNode;
    reset();
    this.asyncConnectWaitCycleCount = 0;
  }

  /* the callers of this method are synchronized */
  private void changeState(State newState) {
    currentState = newState;
  }

  public synchronized State detect() {

    /*
     * Heuristic 1 : Try connecting to some listener port on the peer machine. Even for a LongGC App, new connection
     * should go thru. Note: we are doing an async connect
     */

    if (currentState.equals(ASYNC_CONNECT_PREINIT)) {
      this.conn = connectionManager.createConnection(new NullProtocolAdaptor());
      conn.addListener(this);

      try {
        if (conn.asynchConnect(this.peerNodeAddr)) {
          // success
          closeConn();
          reset();
          return SOCKET_CONNECT_PASS;
        }
      } catch (IOException e) {
        // failure
        reset();
        return SOCKET_CONNECT_FAIL;
      }
      changeState(ASYNC_CONNECT_INITED);
    } else {
      // prev async connect is still in progress
      if (currentState.equals(ASYNC_CONNECT_FAILED)) {
        reset();
        return SOCKET_CONNECT_FAIL;
      } else if (currentState.equals(ASYNC_CONNECT_PASSED)) {
        reset();
        return SOCKET_CONNECT_PASS;
      }
    }

    // asyncConnect didnt return us any answer... increment the cycle and return true
    Assert.eval(currentState.equals(ASYNC_CONNECT_INITED));
    asyncConnectWaitCycleCount++;
    if (asyncConnectWaitCycleCount >= MAX_ASYNC_CONNECT_WAIT_CYCLE) {
      reset();
      return SOCKET_CONNECT_FAIL;
    }
    return SOCKET_CONNECT_RETRY;
  }

  private void reset() {
    asyncConnectWaitCycleCount = 0;
    changeState(ASYNC_CONNECT_PREINIT);
  }

  public synchronized void closeEvent(TCConnectionEvent event) {
    //
  }

  public synchronized void connectEvent(TCConnectionEvent event) {
    changeState(ASYNC_CONNECT_PASSED);
    closeConn();
  }

  public synchronized void endOfFileEvent(TCConnectionEvent event) {
    changeState(ASYNC_CONNECT_FAILED);
  }

  public synchronized void errorEvent(TCConnectionErrorEvent errorEvent) {
    changeState(ASYNC_CONNECT_FAILED);
  }

  private void closeConn() {
    conn.asynchClose();
    conn.removeListener(this);
  }
}
