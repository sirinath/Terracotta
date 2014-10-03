/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Enforces max connections (licenses) based on using one license per unique JVM.
 */
public class ConnectionPolicyImpl implements ConnectionPolicy {

  private final HashMap<String, HashSet<ConnectionID>> clientsByJvm = new HashMap<String, HashSet<ConnectionID>>();
  private final TCLogger                               logger       = TCLogging.getLogger(ConnectionPolicyImpl.class);
  private final int                                    maxConnections;
  private int                                          maxReached;

  public ConnectionPolicyImpl(int maxConnections) {
    Assert.assertTrue("negative maxConnections", maxConnections >= 0);
    this.maxConnections = maxConnections;
  }

  @Override
  public synchronized boolean isConnectAllowed(ConnectionID connID) {
    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (jvmClients == null && isMaxConnectionsReached()) {
      return false;
    }

    return true;
  }

  @Override
  public synchronized String toString() {
    return "ConnectionPolicy[maxConnections=" + maxConnections + ", connectedJvmCount=" + clientsByJvm.size() + "]";
  }

  @Override
  public synchronized boolean connectClient(ConnectionID connID) {
    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (isMaxConnectionsReached() && jvmClients == null) {
      logger.info("Rejecting " + connID + "; " + toString());
      return false;
    }

    if (jvmClients == null) {
      jvmClients = new HashSet<ConnectionID>();
      clientsByJvm.put(connID.getJvmID(), jvmClients);
      maxReached = clientsByJvm.size();
      logger.info("Allocated connection license for jvm " + connID.getJvmID() + "; " + toString());
    }

    if (!jvmClients.contains(connID)) {
      logger.info("New connection [" + connID.getChannelID() + "] from jvm " + connID.getJvmID());
      jvmClients.add(connID);
    }

    return true;
  }

  @Override
  public synchronized void clientDisconnected(ConnectionID connID) {
    // not all times clientSet has connID client disconnect removes the connID. after reconnect timeout, for close event
    // we get here again.

    HashSet<ConnectionID> jvmClients = clientsByJvm.get(connID.getJvmID());

    if (jvmClients == null) return; // must have already received the event for this client

    if (jvmClients.remove(connID)) {
      logger.info("Removed connection [" + connID.getChannelID() + "] from jvm " + connID.getJvmID());
    }

    if (jvmClients.size() == 0) {
      clientsByJvm.remove(connID.getJvmID());
      logger.info("De-allocated connection license for jvm " + connID.getJvmID() + "; " + toString());
    }
  }

  @Override
  public synchronized boolean isMaxConnectionsReached() {
    return (clientsByJvm.size() >= maxConnections);
  }

  @Override
  public synchronized int getMaxConnections() {
    return maxConnections;
  }

  @Override
  public synchronized int getNumberOfActiveConnections() {
    return clientsByJvm.size();
  }

  @Override
  public synchronized int getConnectionHighWatermark() {
    return maxReached;
  }
}
