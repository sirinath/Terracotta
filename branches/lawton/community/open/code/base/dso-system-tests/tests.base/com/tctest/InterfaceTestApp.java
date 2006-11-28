/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

import com.tc.exception.ImplementMe;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

/**
 * TODO May 17, 2005: I, steve, am too lazy to write a single sentence describing what this class is for.
 */
public class InterfaceTestApp extends AbstractTransparentApp {

  public InterfaceTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    new InterfaceTestAppInterface() {

      public void testMethod(String s, int i, FooObject1 foo) {
        throw new ImplementMe();

      }

    };
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = InterfaceTestAppInterface.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("myArrayTestRoot", "myArrayTestRoot");

  }
}
