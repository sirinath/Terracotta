/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

public class MapsSystemTestApp extends AbstractTransparentApp {
  private final ServerControl                   serverControl;
  private final CyclicBarrier                   barrier;
  private final HashMap<PartialSetNode, String> myHashMap;

  private final static int                      NUMBERS_ADDED = 5000;

  public MapsSystemTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    serverControl = cfg.getServerControl();
    barrier = new CyclicBarrier(getParticipantCount());
    myHashMap = new HashMap<PartialSetNode, String>();
  }

  public void run() {
    int index = waitOnBarrier();
    if (index != 0) {
      addElementsToMaps();
    }

    index = waitOnBarrier();
    if (index == 0) restartServer();
    waitOnBarrier();

    validate();
  }

  private void validate() {
    validateAllEntries(myHashMap);
  }

  private void validateAllEntries(Map<PartialSetNode, String> map) {
    synchronized (map) {
      Assert.assertEquals(NUMBERS_ADDED, map.size());
      for (int i = 0; i < NUMBERS_ADDED; i++) {
        String str = map.get(new PartialSetNode(i));
        Assert.assertEquals(String.valueOf(i), str);
      }
    }
  }

  private void addElementsToMaps() {
    addElementsToMap(myHashMap);
  }

  private void addElementsToMap(Map<PartialSetNode, String> map) {
    synchronized (map) {
      for (int i = 0; i < NUMBERS_ADDED; i++) {
        map.put(new PartialSetNode(i), String.valueOf(i));
      }
    }
  }

  private void restartServer() {
    try {
      System.out.println("Crashing the Server ...");
      serverControl.crash();
      ThreadUtil.reallySleep(5000);
      System.out.println("Re-starting the Server ...");
      serverControl.start();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private int waitOnBarrier() {
    int index = -1;
    try {
      index = barrier.await();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return index;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MapsSystemTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(MapsSystemTestApp.class.getName());
    config.addIncludePattern(PartialSetNode.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";

    spec.addRoot("myTreeMap", "myTreeMap");
    spec.addRoot("myHashMap", "myHashMap");
    spec.addRoot("barrier", "barrier");
    config.addWriteAutolock(methodExpression);
  }
}