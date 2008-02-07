/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedShort;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.util.Assert;

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
  private final SynchronizedShort   state                        = new SynchronizedShort((short) 0);
  private TCConnection              conn                         = null;

  private static final short        STATE_ASYNC_CONNECT_PREINIT  = 0x01;
  private static final short        STATE_ASYNC_CONNECT_INITED   = 0x02;
  private static final short        STATE_ASYNC_CONNECT_FAILED   = 0x03;
  private static final short        STATE_ASYNC_CONNECT_PASSED   = 0x04;

  public static final short         SOCKET_CONNECT_PASS          = 0x11;
  public static final short         SOCKET_CONNECT_FAIL          = 0x12;
  public static final short         SOCKET_CONNECT_RETRY         = 0x13;

  public static final int           MAX_ASYNC_CONNECT_WAIT_CYCLE = 2;
  private short                     asyncConnectWaitCycleCount   = 0;

  public HealthCheckerSocketConnect(TCSocketAddress peerNode, TCConnectionManager connMgr) {
    this.connectionManager = connMgr;
    this.peerNodeAddr = peerNode;
    reset();
    this.asyncConnectWaitCycleCount = 0;
  }

  private void changeState(short changeState) {
    this.state.set(changeState);
  }

  private boolean isState(short checkState) {
    return (this.state.compareTo(checkState) == 0);
  }

  public short detect() {

    /*
     * Heuristic 1 : Try connecting to some listener port on the peer machine. Even for a LongGC App, new connection
     * should go thru. Note: we are doing an async connect
     */

    if (isState(STATE_ASYNC_CONNECT_PREINIT)) {
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
      changeState(STATE_ASYNC_CONNECT_INITED);
    } else {
      // prev async connect is still in progress
      if (isState(STATE_ASYNC_CONNECT_FAILED)) {
        reset();
        return SOCKET_CONNECT_FAIL;
      } else if (isState(STATE_ASYNC_CONNECT_PASSED)) {
        reset();
        return SOCKET_CONNECT_PASS;
      }
    }
    
    // asyncConnect didnt return us any answer... increment the cycle and return true
    Assert.eval(isState(STATE_ASYNC_CONNECT_INITED));
    asyncConnectWaitCycleCount++;
    if (asyncConnectWaitCycleCount >= MAX_ASYNC_CONNECT_WAIT_CYCLE) {
      reset();
      return SOCKET_CONNECT_FAIL;
    }
    return SOCKET_CONNECT_RETRY;
  }

  private void reset() {
    asyncConnectWaitCycleCount = 0;
    changeState(STATE_ASYNC_CONNECT_PREINIT);
  }

  public void closeEvent(TCConnectionEvent event) {
    //
  }

  public void connectEvent(TCConnectionEvent event) {
    changeState(STATE_ASYNC_CONNECT_PASSED);
    closeConn();
  }

  public void endOfFileEvent(TCConnectionEvent event) {
    changeState(STATE_ASYNC_CONNECT_FAILED);
  }

  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    changeState(STATE_ASYNC_CONNECT_FAILED);
  }

  private void closeConn() {
    conn.close(500);
    conn.removeListener(this);
  }
}
