/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.properties.TCProperties;

public class HealthCheckerConfigClientImpl extends HealthCheckerConfigImpl {

  public HealthCheckerConfigClientImpl(TCProperties healthCheckerProperties, String hcName) {
    super(healthCheckerProperties, hcName);
  }

  public HealthCheckerConfigClientImpl(String name) {
    super(name);
  }

  public boolean isCallbackPortListenerNeeded() {
    return true;
  }

}
