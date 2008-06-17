/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClientAccessSameObjectTest extends TransparentTestBase {

  private static final int NODE_COUNT    = 3;
  private static final int THREADS_COUNT = 5;

  protected Class getApplicationClass() {
    return ClientAccessSameObjectTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }

  public static class ClientAccessSameObjectTestApp extends AbstractTransparentApp {

  
    final List               lockList         = new ArrayList();
    CyclicBarrier            barrier;

    public ClientAccessSameObjectTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = ClientAccessSameObjectTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      spec.addRoot("lockList", "lockList");
      spec.addRoot("barrier", "barrier");
      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);
      new SynchronizedIntSpec().visit(visitor, config);
      new CyclicBarrierSpec().visit(visitor, config);
    }

    public void run() {
      setCyclicBarrier();
      try {
        barrier.barrier();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      for (int i = 0; i < 100; i++) {
        SynchronizedInt counter = new SynchronizedInt(0);
        synchronized (lockList) {
          lockList.add(counter);
        }
        counter.increment();
      }
    }

    private void setCyclicBarrier() {
      int participationCount = getParticipantCount();
      log("Participation Count = " + participationCount);
      barrier = new CyclicBarrier(participationCount);
    }

    static DateFormat formatter = new SimpleDateFormat("hh:mm:ss,S");

    private static void log(String message) {
      System.err.println(Thread.currentThread().getName() + " :: "
                         + formatter.format(new Date(System.currentTimeMillis())) + " : " + message);
    }

  }

}
