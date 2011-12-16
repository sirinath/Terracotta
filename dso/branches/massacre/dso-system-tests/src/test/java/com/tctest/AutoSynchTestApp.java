/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.builtin.ArrayList;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.List;

/**
 * Test for CDV-190 - auto-synchronize feature
 * 
 * @author hhuynh
 */
public class AutoSynchTestApp extends AbstractErrorCatchingTransparentApp {
  private final CyclicBarrier         barrier;
  private final AutoSynchronizedClass autoSynchronizedRoot = new AutoSynchronizedClass();
  private final NonSynchronizedClass  nonSynchronizedRoot  = new NonSynchronizedClass();

  public AutoSynchTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, com.tc.object.config.DSOClientConfigHelper config) {
    String testClass = AutoSynchTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("autoSynchronizedRoot", "autoSynchronizedRoot");
    spec.addRoot("nonSynchronizedRoot", "nonSynchronizedRoot");

    String baseClass = AutoSynchronizedClass.class.getName();
    config.addIncludePattern(baseClass);
    methodExpression = "* " + baseClass + "*.add(..)";
    config.addWriteAutoSynchronize(methodExpression);
    methodExpression = "* " + baseClass + "*.getSize(..)";
    config.addReadAutoSynchronize(methodExpression);

    baseClass = NonSynchronizedClass.class.getName();
    config.addIncludePattern(baseClass);
  }

  @Override
  protected void runTest() throws Throwable {
    autoSynchronizedRoot.add("one");
    autoSynchronizedRoot.add("two");

    barrier.await();
    Assert.assertEquals(4, autoSynchronizedRoot.getSize());

    barrier.await();

    try {
      nonSynchronizedRoot.add("one");
      throw new AssertionError("Expect to throw an UnlockedSharedObjectException.");
    } catch (UnlockedSharedObjectException e) {
      // Expected
    }
  }

  static class AutoSynchronizedClass {
    protected List list = new ArrayList();

    public void add(Object o) {
      // intentionally not using synchronize
      list.add(o);
    }

    public int getSize() {
      // intentionally not using synchronize
      return list.size();
    }

  }

  static class NonSynchronizedClass {
    protected List list = new ArrayList();

    public void add(Object o) {
      // intentionally not using synchronize
      list.add(o);
    }

    public int getSize() {
      // intentionally not using synchronize
      return list.size();
    }

  }

}
