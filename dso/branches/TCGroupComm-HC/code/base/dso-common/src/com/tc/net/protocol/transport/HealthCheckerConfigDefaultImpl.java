/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class HealthCheckerConfigDefaultImpl implements HealthCheckerConfig {

  // All Time values are in seconds
  public static final int KEEPALIVE_IDLETIME = 60;
  public static final int KEEPALIVE_INTERVAL = 10;
  public static final int KEEPALIVE_PROBECNT = 03;
  public String           hcName             = "DafaultHC";

  /*
   * Say, a commsMgr is created with this default healthCheckerConfig. Then, if a connection is idle for more than 60
   * seconds, HC starts probing in intervals of 10 seconds and for 3 times. So, with this config a dead connection is
   * identified the earliest by 60 + (3 * 10) = 90 seconds. Too long ... but just an example.
   */
  public HealthCheckerConfigDefaultImpl(String hcName) {
    this.hcName = hcName;
  }

  public boolean isKeepAliveEnabled() {
    return true;
  }

  public int getKeepAliveIdleTime() {
    return KEEPALIVE_IDLETIME;
  }

  public int getKeepAiveInterval() {
    return KEEPALIVE_INTERVAL;
  }

  public int getKeepAliveProbes() {
    return KEEPALIVE_PROBECNT;
  }

  public String getHealthCheckerName() {
    return hcName;
  }
}
