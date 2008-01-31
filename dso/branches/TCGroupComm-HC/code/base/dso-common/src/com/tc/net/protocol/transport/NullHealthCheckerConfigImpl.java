/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class NullHealthCheckerConfigImpl implements HealthCheckerConfig {

  public boolean isHealthCheckerEnabled() {
    return false;
  }
  
  public int getKeepAliveIdleTime() {
    return -1;
  }

  public int getKeepAliveInterval() {
    return -1;
  }

  public int getKeepAliveProbes() {
    return -1;
  }

  public String getHealthCheckerName() {
    return null;
  }

  public boolean isDummy() {
    return true;
  }

  public boolean doExtraChecks() {
    return false;
  }


}
