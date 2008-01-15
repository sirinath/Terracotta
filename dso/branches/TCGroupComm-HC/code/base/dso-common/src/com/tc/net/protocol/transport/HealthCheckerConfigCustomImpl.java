/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class HealthCheckerConfigCustomImpl implements HealthCheckerConfig {

  // All Time values are in seconds
  final int    KEEPALIVE_IDLETIME; // A
  final int    KEEPALIVE_INTERVAL; // B
  final int    KEEPALIVE_PROBECNT; // C
  final String hcName;

  // The Dead connection is identified earliest by A + (B * C) seconds

  public HealthCheckerConfigCustomImpl() {
    this(HealthCheckerConfigDefaultImpl.KEEPALIVE_IDLETIME, HealthCheckerConfigDefaultImpl.KEEPALIVE_INTERVAL,
         HealthCheckerConfigDefaultImpl.KEEPALIVE_PROBECNT, "DefaultHC");
  }

  public HealthCheckerConfigCustomImpl(int keepaliveIdletime, int keepaliveInterval, int keepaliveProbecnt,
                                       String hcName) {
    this.KEEPALIVE_IDLETIME = keepaliveIdletime;
    this.KEEPALIVE_INTERVAL = keepaliveInterval;
    this.KEEPALIVE_PROBECNT = keepaliveProbecnt;
    this.hcName = hcName;
  }

  public boolean isKeepAliveEnabled() {
    return true;
  }

  public int getKeepAliveIdleTime() {
    return this.KEEPALIVE_IDLETIME;
  }

  public int getKeepAiveInterval() {
    return this.KEEPALIVE_INTERVAL;
  }

  public int getKeepAliveProbes() {
    return this.KEEPALIVE_PROBECNT;
  }

  public String getHealthCheckerName() {
    return this.hcName;
  }
}
