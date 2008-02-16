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
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Test case for DEV-1283
 */
public class HashMapValueSharedTest extends GCTestBase {

  private static final int NODE_COUNT = 4;

  protected Class getApplicationClass() {
    return HashMapValueShareApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  public static class HashMapValueShareApp extends AbstractTransparentApp {

    private static final int    NUM_OF_OBJS      = 1000000;
    private final HashMap       hashmapRoot      = new HashMap();
    private final HashMap       sharedObjectRoot = new HashMap();
    private final CyclicBarrier barrier;

    public HashMapValueShareApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

      String testClass = HashMapValueShareApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      config.addIncludePattern(testClass + "$*", false, false, true);

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("hashmapRoot", "hashmapRoot");
      spec.addRoot("sharedObjectRoot", "sharedObjectRoot");
      spec.addRoot("barrier", "barrier");
    }

    public void run() {

      // barrier for shared value object
      int index = 0;
      try {
        index = barrier.await();

      } catch (InterruptedException e) {
        throw new AssertionError(e);
      } catch (BrokenBarrierException e) {
        throw new AssertionError(e);
      }

      // have one thread create the value object to share among all object in the hashMap root
      if (index == 0) {
        synchronized( sharedObjectRoot ) {
          sharedObjectRoot.put("sharedValueObject", new Object());
        }
      }

      // barrier for populating root
      try {

        index = barrier.await();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      } catch (BrokenBarrierException e) {
        throw new AssertionError(e);
      }

      // populate with shared objects
      if (index == 1) {
        synchronized (hashmapRoot) {
          synchronized (sharedObjectRoot) {
          Object sObject = sharedObjectRoot.get("sharedValueObject");
          assertNotNull(sObject);
          for (int i = 0; i < NUM_OF_OBJS; i++) {
            hashmapRoot.put(String.valueOf(i), new ValueObject(sObject));
          }
          }
        }
      }

      // barrier reading hashmapRoot
      try {
        barrier.await();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      } catch (BrokenBarrierException e) {
        throw new AssertionError(e);
      }

      // assert if the shared object values are the same
      for (int i = 0; i < NUM_OF_OBJS; i++) {
        synchronized (hashmapRoot) {
          synchronized( sharedObjectRoot ) {
            Object sObject = sharedObjectRoot.get("sharedValueObject");
            assertNotNull(sObject);
            assertEquals(sObject, ((ValueObject) hashmapRoot.get(String.valueOf(i))).obj);
          }
        }
      }

    }

    public static class ValueObject {

      public ValueObject(Object obj) {
        this.obj = obj;
      }

      public Object obj;

    }

  }

}
