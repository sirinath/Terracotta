/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.runner;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

/**
 * This is a different flavor of AbstractTransparentApp that takes care of catching any exceptions and reporting them. I
 * found myself writing this pattern far too often in individual test cases
 */
public abstract class AbstractTransparentApp2 extends AbstractTransparentApp {

  public AbstractTransparentApp2(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public final void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }
  
  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addIncludePattern(AbstractTransparentApp2.class.getName());
  }


  protected abstract void runTest() throws Throwable;
}
