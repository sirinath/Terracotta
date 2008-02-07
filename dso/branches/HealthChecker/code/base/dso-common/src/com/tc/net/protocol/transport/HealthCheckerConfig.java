/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface HealthCheckerConfig {

  // HC - HealthChecker

  /* HC enabled/disabled for this commsMgr */
  boolean isHealthCheckerEnabled();

  /* HC Name - describing what it is monitoring */
  String getHealthCheckerName();

  /* HC tests liveness of a connection when no message transaction is seen on it for more than keepalive_idle time */
  int getPingIdleTime();

  /* HC probes a connection once in keepalive_interval time after it is found idle for keepalive_idle time */
  int getPingInterval();

  /* HC probes a idle connection for keepalive_probes times before tagging it as dead */
  int getPingProbes();

  /*
   * When HC detected the peer has died by above probes, it can do additional checks to see any traces of life left out 
   *  1. chk whether the peer is in Long GC
   *  2. more similar checks
   *  
   *  If the peer is un-responsive and not died, a grace period is given before deciding it as dead.
   */
  boolean doSocketConnect();
  
  int getMaxSocketConnectCount();
}
