/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.TCRuntimeException;
import com.tc.net.AddressChecker;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.TCProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConnectionHealthCheckerUtil {

  public static NetworkListener createHealthCheckListener(CommunicationsManager communicationsManager,
                                                          TCProperties healthCheckListenerProperties) {
    InetAddress bindAddr;
    String bindAddress = healthCheckListenerProperties.getProperty("healthcheck.bindAddress");
    if (bindAddress == null) {
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

    int bindPort = healthCheckListenerProperties.getInt("healthcheck.bindPort", 0);
    return communicationsManager.createListener(new NullSessionManager(), new TCSocketAddress(bindAddr, bindPort),
                                                true, new DefaultConnectionIdFactory());
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
