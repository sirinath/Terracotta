package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

/**
 * Test Application
 */
public class CreateLotsOfGarbageGCTestApp extends AbstractTransparentApp {

  private static final long TIME_TO_RUN = 5 * 60 * 1000;
  private static final int  SIZE        = 50;

  private final Object[]    array       = new Object[SIZE];

  public CreateLotsOfGarbageGCTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = CreateLotsOfGarbageGCTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("array", "root");
  }

  public void run() {
    long start = System.currentTimeMillis();
    int i = 0;
    while (System.currentTimeMillis() - start < TIME_TO_RUN) {
      synchronized (array) {
        for (int j = 0; j < SIZE; j++) {
          array[j] = new Object();
        }
      }
      if (i != 0 && i % 50 == 0) {
        System.out.println("Loop count : " + i);
        ThreadUtil.reallySleep(1000);
      }
      i++;
    }
  }
}