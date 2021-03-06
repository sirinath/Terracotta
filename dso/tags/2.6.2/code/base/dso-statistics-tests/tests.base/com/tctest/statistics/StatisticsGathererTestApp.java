/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

public class StatisticsGathererTestApp extends AbstractTransparentApp {

  public static final int NODE_COUNT = 2;

  private CyclicBarrier barrier = new CyclicBarrier(NODE_COUNT);

  public StatisticsGathererTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    barrier();
  }

  private void barrier() {
    try {
      barrier.barrier();
    } catch (InterruptedException ie) {
      throw new AssertionError();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = StatisticsGathererTestApp.class.getName();

    config.getOrCreateSpec(testClass)
      .addRoot("barrier", "barrier");

    config.addWriteAutolock("* " + testClass + "*.*(..)");
    new CyclicBarrierSpec().visit(visitor, config);
  }
}