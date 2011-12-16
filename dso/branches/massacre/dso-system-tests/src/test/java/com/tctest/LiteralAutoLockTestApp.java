/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

public class LiteralAutoLockTestApp extends AbstractTransparentApp {
  private final Set           nodes   = new HashSet();
  private final CyclicBarrier barrier = new CyclicBarrier(2);

  public LiteralAutoLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    int size = 0;
    synchronized ("Steve") {
      nodes.add(new Object());
      size = nodes.size();
    }
    try {
      System.out.println("barrier:" + size);
      barrier.await();
      System.out.println("barrier out:" + size);
    } catch (InterruptedException ie) {
      notifyError(ie);
    } catch (BrokenBarrierException e) {
      notifyError(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LiteralAutoLockTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("nodes", "nodes");
    spec.addRoot("barrier", "barrier");
  }
}
