/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.util.TCTimeoutException;

import java.io.IOException;

public class HealthCheckerExtra {

  private final int                 LONG_GC_CONNECTION_TIMEOUT = 3000; // XXX configurable ?
  private final TCSocketAddress     peerNodeAddr;
  private final TCConnectionManager connMgr;

  public HealthCheckerExtra(TCSocketAddress peerNode, TCConnectionManager connMgr) {
    this.peerNodeAddr = peerNode;
    this.connMgr = connMgr;
  }

  public boolean detect() {

    /*
     * Heuristic 1 : Try connecting to some listener port on the peer machine. Even for a LongGC App, new connection
     * should go thru.
     */
    TCConnection conn = connMgr.createConnection(new NullProtocolAdaptor());
    boolean h1 = false;
    try {
      conn.connect(this.peerNodeAddr, LONG_GC_CONNECTION_TIMEOUT);
      h1 = true;
    } catch (TCTimeoutException e) {
      h1 = false;
    } catch (IOException e) {
      h1 = false;
    } finally {
      conn.close(LONG_GC_CONNECTION_TIMEOUT);
    }

    /*
     * Other Heurisrtics ... keep adding if you find a better one h2 .. h3 .. h4
     */

    return h1;
  }
}
