/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface HealthCheckerConfig {

  // HC - HealthChecker

  /* HC enabled/disabled for this commsMgr */
  boolean isHealthCheckerEnabled();

  /* HC Name - describing what it is monitoring */
  String getHealthCheckerName();

  /* HC tests liveness of a connection when no message transaction is seen on it for more than ping_idle time */
  long getPingIdleTimeMillis();

  /* HC probes a connection once in ping_interval time after it is found idle for ping_idle time */
  long getPingIntervalMillis();

  /* HC probes a idle connection for ping_probes times before tagging it as dead */
  int getPingProbes();

  /**
   * When HC detected the peer has died by above probes, it can do additional checks to see any traces of life left out
   * <ol>
   * <li>check whether the peer is in Long GC
   * <li>more similar checks
   * </ol>
   * <br>
   * If the peer is unresponsive and not died, a grace * period is given before deciding it as dead.
   */
  boolean isSocketConnectOnPingFail();

  int getSocketConnectMaxCount();

  int getSocketConnectTimeout();

  /**
   * RMP-343: L2 SocketConnect L1
   */
  boolean isCallbackPortListenerNeeded();

  String getCallbackPortListenerBindAddress();

  int getCallbackPortListenerBindPort();

  /**
   * Checking time difference between hosts enabled/disabled.
   */
  boolean isCheckTimeEnabled();

  /**
   * If {@link #isCheckTimeEnabled()} is {@code true}, HC checks time difference
   * no more than once per this interval in milliseconds.
   */
  long getCheckTimeInterval();

  /**
   * HC logs a warning if {@link #isCheckTimeEnabled()} is {@code true}
   * and the time difference exceeds this threshold.
   */
  long getTimeDiffThreshold();

}
