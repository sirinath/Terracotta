/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Arrays;

public class DistributedMethodCallTestApp extends AbstractTransparentApp {

  private SharedModel         model          = new SharedModel();
  private final CyclicBarrier sharedBarrier  = new CyclicBarrier(getParticipantCount());
  private final CyclicBarrier sharedBarrier2 = new CyclicBarrier(getParticipantCount());

  public DistributedMethodCallTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    try {
      runTest();
    } catch (Throwable e) {
      notifyError(e);
    }
  }

  private void runTest() throws Throwable {
    final boolean callInitiator = sharedBarrier.barrier() == 0;

    if (callInitiator) {
      synchronized (model) {
        FooObject[][] foos = new FooObject[2][3];
        for (int i = 0; i < foos.length; i++) {
          Arrays.fill(foos[i], new FooObject());
        }

        int count = 0;
        int[][][] ints = new int[6][8][9];
        for (int i = 0; i < ints.length; i++) {
          int[][] array1 = ints[i];
          for (int j = 0; j < array1.length; j++) {
            int[] array2 = array1[j];
            for (int k = 0; k < array2.length; k++) {
              array2[k] = count++;
            }
          }
        }
        model.addObject(new FooObject(), 1, 2, foos, ints, true);
      }
    }
    sharedBarrier.barrier();
    final int actual = model.callCount.get();
    if (actual != getParticipantCount()) {
      notifyError("Unexpected call count: expected=" + getParticipantCount() + ", actual=" + actual);
    }
  }

  public class SharedModel {
    public final SynchronizedInt callCount = new SynchronizedInt(0);

    public void addObject(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b) throws Throwable {
      callCount.increment();
      // Everything in the "foos" array should be non-null
      for (int index = 0; index < foos.length; index++) {
        FooObject[] array = foos[index];
        for (int j = 0; j < array.length; j++) {
          FooObject foo = array[j];
          if (foo == null) notifyError("foo == null");
        }
      }

      // access all the "ints"
      int count = 0;
      for (int index = 0; index < ints.length; index++) {
        int[][] array1 = ints[index];
        for (int j = 0; j < array1.length; j++) {
          int[] array2 = array1[j];
          for (int k = 0; k < array2.length; k++) {
            int val = array2[k];
            if (count++ != val) notifyError("count ++ != val");
          }
        }
      }

      if (obj == null || i != 1 || d != 2 || !b) {
        System.out.println("Invalid parameters:" + obj + " i:" + i + " d:" + d + " b:" + b);
        notifyError("Invalid parameters:" + obj + " i:" + i + " d:" + d + " b:" + b);
      }
      sharedBarrier2.barrier();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    try {
      new CyclicBarrierSpec().visit(visitor, config);
      new SynchronizedIntSpec().visit(visitor, config);

      TransparencyClassSpec spec = config.getOrCreateSpec(FooObject.class.getName());
      String testClassName = DistributedMethodCallTestApp.class.getName();
      spec = config.getOrCreateSpec(testClassName);
      spec.addTransient("callInitiator");
      spec.addRoot("model", "model");
      spec.addRoot("sharedStuff", "sharedStuff");
      spec.addRoot("sharedBarrier", "sharedBarrier");
      spec.addRoot("sharedBarrier2", "sharedBarrier2");
      String methodExpression = "* " + testClassName + "*.*(..)";
      System.err.println("Adding autolock for: " + methodExpression);
      config.addWriteAutolock(methodExpression);

      spec = config.getOrCreateSpec(SharedModel.class.getName());
      spec.addDistributedMethodCall("addObject", "(Ljava/lang/Object;ID[[Lcom/tctest/FooObject;[[[IZ)V");
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
