/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

public abstract class ServerCrashingAppBase extends AbstractTransparentApp {

  public static final String CONFIG_FILE = "config-file";
  public static final String PORT_NUMBER = "port-number";
  public static final String HOST_NAME   = "host-name";

  private ApplicationConfig  config;

  public ServerCrashingAppBase(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  public ApplicationConfig getConfig() {
    return config;
  }
}
