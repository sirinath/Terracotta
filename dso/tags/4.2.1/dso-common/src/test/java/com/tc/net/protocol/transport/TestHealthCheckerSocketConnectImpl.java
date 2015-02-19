/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;

public class TestHealthCheckerSocketConnectImpl extends HealthCheckerSocketConnectImpl {

  public TestHealthCheckerSocketConnectImpl(TCSocketAddress peerNode, TCConnection conn, String remoteNodeDesc,
                                            TCLogger logger, int timeoutInterval) {
    super(peerNode, conn, remoteNodeDesc, logger, timeoutInterval);
  }

  @Override
  public synchronized void closeEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public synchronized void connectEvent(TCConnectionEvent event) {
    // ignore the connect events
  }

  @Override
  public synchronized void endOfFileEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public synchronized void errorEvent(TCConnectionErrorEvent errorEvent) {
    //
  }

}
