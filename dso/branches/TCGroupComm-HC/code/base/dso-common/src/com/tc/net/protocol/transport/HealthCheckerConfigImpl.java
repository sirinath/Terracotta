/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.properties.TCProperties;

public class HealthCheckerConfigImpl implements HealthCheckerConfig {

  private final boolean keepaliveEnable;
  private final int     keepaliveIdleTime;
  private final int     keepaliveInterval;
  private final int     keepaliveProbes;
  private final String  hcName;

  public HealthCheckerConfigImpl(TCProperties healthCheckerProperties, String hcName) {
    this.keepaliveEnable = healthCheckerProperties.getBoolean("enabled");
    this.keepaliveIdleTime = healthCheckerProperties.getInt("idletime");
    this.keepaliveInterval = healthCheckerProperties.getInt("interval");
    this.keepaliveProbes = healthCheckerProperties.getInt("probes");
    this.hcName = hcName;
  }

  public boolean isKeepAliveEnabled() {
    return this.keepaliveEnable;
  }

  public int getKeepAliveIdleTime() {
    return this.keepaliveIdleTime;
  }

  public int getKeepAiveInterval() {
    return this.keepaliveInterval;
  }

  public int getKeepAliveProbes() {
    return this.keepaliveProbes;
  }

  public String getHealthCheckerName() {
    return this.hcName;
  }
}
