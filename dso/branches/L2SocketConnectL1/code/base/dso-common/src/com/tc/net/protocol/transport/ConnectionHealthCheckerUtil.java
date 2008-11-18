/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.net.AddressChecker;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.TCProperties;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConnectionHealthCheckerUtil {

  public static NetworkListener createHealthCheckListener(CommunicationsManager communicationsManager,
                                                          TCProperties healthCheckListenerProperties) {
    InetAddress bindAddr;
    String bindAddress = healthCheckListenerProperties.getProperty("healthcheck.bindAddress");
    if (bindAddress == null || bindAddress.equals("")) {
      bindAddress = TCSocketAddress.WILDCARD_IP;
    }

    try {
      bindAddr = InetAddress.getByName(bindAddress);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + TCSocketAddress.WILDCARD_IP);
    }
    AddressChecker addressChecker = new AddressChecker();
    if (!addressChecker.isLegalBindAddress(bindAddr)) { throw new TCRuntimeException("Invalid bind address ["
                                                                                     + bindAddr
                                                                                     + "]. Local addresses are "
                                                                                     + addressChecker
                                                                                         .getAllLocalAddresses()); }

    String bindPortProperty = healthCheckListenerProperties.getProperty("healthcheck.bindPort");
    int bindPort;
    if (bindPortProperty == null || bindPortProperty.equals("")) {
      bindPort = 0;
    } else {
      bindPort = Integer.parseInt(bindPortProperty);
    }
    return communicationsManager.createListener(new NullSessionManager(), new TCSocketAddress(bindAddr, bindPort),
                                                true, new DefaultConnectionIdFactory());
  }

  public static boolean isHealthCheckListenerRechable(TCConnectionManager connectionManager,
                                                      TCSocketAddress peerHCListener, TCLogger logger) {
    TCConnection connection = connectionManager.createConnection(new NullProtocolAdaptor());
    try {
      connection.connect(peerHCListener, 2000);
    } catch (TCTimeoutException e) {
      logger.info("Timeout when connecting to HealthCheckListener at " + peerHCListener + " : " + e);
      connection.close(200);
      return false;
    } catch (IOException e) {
      logger.info("Error connecting to HealthCheckListener at " + peerHCListener + " : " + e);
      connection.close(200);
      return false;
    }
    logger.info("HealthCheckListener at " + peerHCListener + " is Rechable");
    connection.close(2000);
    return true;
  }

  public static long getMaxIdleTimeForAlive(HealthCheckerConfig config, boolean considerPeerNodeLongGC) {
    // XXX should we check for disabled hc config ?
    if (considerPeerNodeLongGC) {
      return (config.getPingIdleTimeMillis() + (config.getPingIntervalMillis() * ((config.getSocketConnectTimeout() * config
          .getPingProbes()) * config.getSocketConnectMaxCount())));
    } else {
      return (config.getPingIdleTimeMillis() + (config.getPingIntervalMillis() * config.getPingProbes()));
    }
  }
}
