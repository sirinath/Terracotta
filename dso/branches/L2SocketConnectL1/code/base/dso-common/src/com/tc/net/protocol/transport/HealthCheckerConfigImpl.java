/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.TCSocketAddress;
import com.tc.properties.TCProperties;

/**
 * Main implementation of the Health Checker Config. Health Checker related tc.properties are read and a config data
 * structure is built which is passed on to various health checker modules.
 * 
 * @author Manoj
 */
public class HealthCheckerConfigImpl implements HealthCheckerConfig {

  private final boolean    enable;
  private final long       pingIdleTime;
  private final long       pingInterval;
  private final int        pingProbes;
  private final boolean    doSocketConnect;
  private final int        socketConnectTimeout;
  private final int        socketConnectMaxCount;
  private final String     name;

  // RMP-343:
  private final String     callbackportListenerBindAddress;
  private final int        callbackportListenerBindPort;

  // Default ping probe values in milliseconds
  private static final int DEFAULT_PING_IDLETIME          = 45000;
  private static final int DEFAULT_PING_INTERVAL          = 15000;
  private static final int DEFAULT_PING_PROBECNT          = 3;
  private static final int DEFAULT_SCOKETCONNECT_MAXCOUNT = 3;
  private static final int DEFAULT_SOCKETCONNECT_TIMEOUT  = 2;

  public HealthCheckerConfigImpl(TCProperties healthCheckerProperties, String hcName) {
    this.pingIdleTime = healthCheckerProperties.getLong("ping.idletime");
    this.pingInterval = healthCheckerProperties.getLong("ping.interval");
    this.pingProbes = healthCheckerProperties.getInt("ping.probes");
    this.name = hcName;
    this.doSocketConnect = healthCheckerProperties.getBoolean("socketConnect");
    this.enable = healthCheckerProperties.getBoolean("ping.enabled");
    this.socketConnectMaxCount = healthCheckerProperties.getInt("socketConnectCount");
    this.socketConnectTimeout = healthCheckerProperties.getInt("socketConnectTimeout");
    this.callbackportListenerBindAddress = healthCheckerProperties.getProperty("bindAddress");
    this.callbackportListenerBindPort = healthCheckerProperties.getInt("bindPort", 0);
  }

  // Default Ping-Probe cycles. No SocketConnect check
  public HealthCheckerConfigImpl(String name) {
    this(DEFAULT_PING_IDLETIME, DEFAULT_PING_INTERVAL, DEFAULT_PING_PROBECNT, name, false);
  }

  // Custom SocketConnect check. Default SocketConnect values
  public HealthCheckerConfigImpl(long idle, long interval, int probes, String name, boolean extraCheck) {
    this(idle, interval, probes, name, extraCheck, DEFAULT_SCOKETCONNECT_MAXCOUNT, DEFAULT_SOCKETCONNECT_TIMEOUT,
         TCSocketAddress.WILDCARD_IP, 0);
  }

  // All Custom values
  public HealthCheckerConfigImpl(long idle, long interval, int probes, String name, boolean extraCheck,
                                 int socketConnectMaxCount, int socketConnectTimeout, String bindAddress, int bindPort) {
    this.pingIdleTime = idle;
    this.pingInterval = interval;
    this.pingProbes = probes;
    this.name = name;
    this.doSocketConnect = extraCheck;
    this.enable = true;
    this.socketConnectMaxCount = socketConnectMaxCount;
    this.socketConnectTimeout = socketConnectTimeout;
    this.callbackportListenerBindAddress = bindAddress;
    this.callbackportListenerBindPort = bindPort;
  }

  public boolean isSocketConnectOnPingFail() {
    return doSocketConnect;
  }

  public boolean isHealthCheckerEnabled() {
    return enable;
  }

  public long getPingIdleTimeMillis() {
    return this.pingIdleTime;
  }

  public long getPingIntervalMillis() {
    return this.pingInterval;
  }

  public int getPingProbes() {
    return this.pingProbes;
  }

  public String getHealthCheckerName() {
    return this.name;
  }

  public int getSocketConnectMaxCount() {
    return this.socketConnectMaxCount;
  }

  public int getSocketConnectTimeout() {
    return this.socketConnectTimeout;
  }

  public String getCallbackPortListenerBindAddress() {
    return this.callbackportListenerBindAddress;
  }

  public int getCallbackPortListenerBindPort() {
    return this.callbackportListenerBindPort;
  }

  public boolean isCallbackPortListenerNeeded() {
    return false;
  }

}
