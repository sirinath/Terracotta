/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.properties.TCProperties;

public class HealthCheckerConfigImpl implements HealthCheckerConfig {

  private final boolean    keepaliveEnable;
  private final boolean    doExtraChecks;
  private final String     name;
  private final int        keepaliveIdleTime;
  private final int        keepaliveInterval;
  private final int        keepaliveProbes;

  // Default keepalive values in seconds
  private final static int KEEPALIVE_IDLETIME = 45;
  private final static int KEEPALIVE_INTERVAL = 15;
  private final static int KEEPALIVE_PROBECNT = 3;

  // for testing
  boolean                  dummy              = false;

  public HealthCheckerConfigImpl(TCProperties healthCheckerProperties, String hcName) {
    this.keepaliveIdleTime = healthCheckerProperties.getInt("idletime");
    this.keepaliveInterval = healthCheckerProperties.getInt("interval");
    this.keepaliveProbes = healthCheckerProperties.getInt("probes");
    this.name = hcName;
    this.doExtraChecks = healthCheckerProperties.getBoolean("extraChecks");
    this.keepaliveEnable = healthCheckerProperties.getBoolean("enabled");
  }

  public HealthCheckerConfigImpl(String name) {
    this(KEEPALIVE_IDLETIME, KEEPALIVE_INTERVAL, KEEPALIVE_PROBECNT, name, false);
  }

  public HealthCheckerConfigImpl(String name, boolean extraCheck) {
    this(KEEPALIVE_IDLETIME, KEEPALIVE_INTERVAL, KEEPALIVE_PROBECNT, name, extraCheck);
  }

  public HealthCheckerConfigImpl(int idle, int interval, int probes, String name) {
    this(idle, interval, probes, name, false);
  }

  public HealthCheckerConfigImpl(int idle, int interval, int probes, String name, boolean extraCheck) {
    this.keepaliveIdleTime = idle;
    this.keepaliveInterval = interval;
    this.keepaliveProbes = probes;
    this.name = name;
    this.doExtraChecks = extraCheck;
    this.keepaliveEnable = true;
  }

  public boolean doExtraChecks() {
    return doExtraChecks;
  }

  public boolean isHealthCheckerEnabled() {
    return keepaliveEnable;
  }

  public int getKeepAliveIdleTime() {
    return this.keepaliveIdleTime;
  }

  public int getKeepAliveInterval() {
    return this.keepaliveInterval;
  }

  public int getKeepAliveProbes() {
    return this.keepaliveProbes;
  }

  public String getHealthCheckerName() {
    return this.name;
  }

  // for testing

  public void setDummy() {
    this.dummy = true;
  }
  
  public boolean isDummy() {
    return this.dummy;
  }

}
