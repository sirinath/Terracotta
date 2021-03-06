/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.ReconnectionRejectedException;

public class ReconnectionRejectedDefaultHandler implements ReconnectionRejectedHandler {

  public void reconnectionRejected(ReconnectionRejectedCleanupAction cleanup) throws ReconnectionRejectedException {
    throw new ReconnectionRejectedException("Reconnection rejected due to stack not found. Default Behaviour.");
  }

}
