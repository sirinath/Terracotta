/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;


public interface HealthCheckerConfig {

  // HC - HealthChecker

  /* HC Name - describing what it is monitoring */
  String getHealthCheckerName();

  /* HC enabled/disabled for this commsMgr */
  boolean isKeepAliveEnabled();

  /* HC tests liveness of a connection when no message transaction is seen on it for more than keepalive_idle time */
  int getKeepAliveIdleTime();

  /* HC probes a connection once in keepalive_interval time after it is found idle for keepalive_idle time */
  int getKeepAiveInterval();

  /* HC probes a idle connection for keepalive_probes times before tagging it as dead */
  int getKeepAliveProbes();
  
}
