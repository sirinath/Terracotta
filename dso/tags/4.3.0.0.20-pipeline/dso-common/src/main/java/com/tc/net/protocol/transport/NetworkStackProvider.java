/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.RejectReconnectionException;

/**
 * Provider/locator for a network stack.
 */
public interface NetworkStackProvider {

  /**
   * Takes a new connection and a connectionId. Returns the MessageTransport associated with that id.
   */
  public MessageTransport attachNewConnection(ConnectionID connectionId, TCConnection connection)
      throws RejectReconnectionException;

}
