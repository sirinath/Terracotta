/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class NullConnectionPolicy implements ConnectionPolicy {

  @Override
  public boolean connectClient(ConnectionID id) {
    return true;
  }

  @Override
  public void clientDisconnected(ConnectionID id) {
    return;
  }

  @Override
  public boolean isMaxConnectionsReached() {
    return false;
  }

  @Override
  public int getMaxConnections() {
    return -1;
  }

  @Override
  public int getNumberOfActiveConnections() {
    return 0;
  }

  @Override
  public int getConnectionHighWatermark() {
    return 0;
  }

  @Override
  public boolean isConnectAllowed(ConnectionID id) {
    return true;
  }

}
