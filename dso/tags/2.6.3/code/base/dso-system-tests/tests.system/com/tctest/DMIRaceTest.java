/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DistributedMethodSpec;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;
import com.terracottatech.config.PersistenceMode;

import java.util.ArrayList;
import java.util.List;

public class DMIRaceTest extends TransparentTestBase {

  private static final int NODE_COUNT = 6;
  private static final int PRODUCERS  = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected void setupConfig(TestTVSConfigurationSetupManagerFactory configFactory) {
    configFactory.setPersistenceMode(PersistenceMode.PERMANENT_STORE);
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private static final int    DURATION = 4 * 60 * 1000;
    private static final long   END      = System.currentTimeMillis() + DURATION;

    private final CyclicBarrier barrier;
    private final List          root     = new ArrayList();
    private DmiTarget           dmiTarget;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    protected void runTest() throws Throwable {
      final int index = barrier.barrier();
      final boolean dmiSource = index == 0;

      if (dmiSource) {
        dmiTarget = new DmiTarget();
      }

      barrier.barrier();

      if (index < PRODUCERS) {
        producer(dmiSource);
      } else {
        // fault the dmi target in the non-producer nodes
        DmiTarget faulted = dmiTarget;

        waitUntilEnd();

        if (faulted == null) {
          // silence compiler warning
          throw new AssertionError();
        }
      }
    }

    private void producer(boolean dmi) {
      while (!isEnd()) {
        for (int i = 0; i < 100; i++) {
          if (dmi) {
            Object obj = new Object();
            synchronized (root) {
              root.add(obj);
            }

            dmiTarget.dmi(obj);
          } else {
            synchronized (getApplicationId()) {
              root.clear();
            }
          }
        }
      }
    }

    private void waitUntilEnd() {
      while (!isEnd()) {
        ThreadUtil.reallySleep(2000);
      }
    }

    private boolean isEnd() {
      return System.currentTimeMillis() > END;
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      new CyclicBarrierSpec().visit(visitor, config);

      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
      spec.addRoot("dmiTarget", "dmiTarget");
      spec.addRoot("barrier", "barrier");

      config.addWriteAutolock("* " + testClassName + ".*(..)");

      config.addIncludePattern(DmiTarget.class.getName());
      config
          .addDistributedMethodCall(new DistributedMethodSpec("void " + DmiTarget.class.getName() + ".dmi(..)", false));
    }

    private static class DmiTarget {
      void dmi(Object arg) {
        //
      }
    }

  }

}
