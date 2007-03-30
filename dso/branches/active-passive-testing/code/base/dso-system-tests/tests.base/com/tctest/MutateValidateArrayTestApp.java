/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.util.ArrayList;
import java.util.List;

public class MutateValidateArrayTestApp extends AbstractMutateValidateTransparentApp {

  private String[]     myArrayTestRoot;
  private List         validationArray;
  private int          iterationCount1;
  private int          iterationCount2;
  private int          iterationCount3;
  private final String appId;

  public MutateValidateArrayTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appId = appId;
    myArrayTestRoot = new String[] { "hee", "hoo", "haa" };
    iterationCount1 = 300;
    iterationCount2 = 300;
    iterationCount3 = 300;
    validationArray = new ArrayList();
  }

  protected void mutate() throws Throwable {
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount1; i++) {
        int index = (i + 1) % myArrayTestRoot.length;
        String val = myArrayTestRoot[index];
        validationArray.add(val);
        System.out.println("****** appId[" + appId + "]:   val added=[" + val + "] index=[" + index + "]");
      }
    }
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount2; i++) {
        int index = (i + 1) % myArrayTestRoot.length;
        String val = myArrayTestRoot[index];
        validationArray.add(val);
        System.out.println("****** appId[" + appId + "]:   val added=[" + val + "] index=[" + index + "]");
      }
    }
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount3; i++) {
        int index = (i + 1) % myArrayTestRoot.length;
        String val = myArrayTestRoot[index];
        validationArray.add(val);
        System.out.println("****** appId[" + appId + "]:   val added=[" + val + "] index=[" + index + "]");
      }
    }
  }

  protected void validate() throws Throwable {
    synchronized (validationArray) {
      for (int i = 0; i < (iterationCount1 + iterationCount2 + iterationCount3) * getParticipantCount(); i++) {
        System.out.println("****** appId[" + appId + "]:   index=[" + i + "]");
        System.out.println("***** " + validationArray.get(i));

        boolean val = myArrayTestRoot[(i + 1) % myArrayTestRoot.length].equals(validationArray.get(i));
        if (!val) {
          notifyError("Expecting <" + myArrayTestRoot[(i + 1) % myArrayTestRoot.length] + "> but got <"
                      + validationArray.get(i) + ">");
        }
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MutateValidateArrayTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("myArrayTestRoot", "myArrayTestRoot");
    spec.addRoot("validationArray", "validationArray");
  }

}
