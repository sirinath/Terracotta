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

  private String[] myArrayTestRoot;
  private List validationArray;
  private int      iterationCount;
  private final String appId;

  public MutateValidateArrayTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appId = appId;
    myArrayTestRoot = new String[] { "hee", "hoo", "haa" };
    iterationCount = 3;
    validationArray = new ArrayList();
  }

  // used by validators
//  public MutateValidateArrayTestApp() {
//    super();
//    myArrayTestRoot = new String[] {};
//
//    if (myArrayTestRoot.length != 3) { throw new AssertionError("myArrayTestRoot is not as expected: "
//                                                                + myArrayTestRoot); }
//
//    validationArray = new String[] {};
//
//    iterationCount = validationArray.length;
//    if (iterationCount != 10) { throw new AssertionError("validationArray is not as expected: " + validationArray); }
//  }

  protected void mutate() throws Throwable {
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount; i++) {
        int index = (i + 1) % myArrayTestRoot.length;
        String val = myArrayTestRoot[index];
        validationArray.add(val);
        System.out.println("****** appId[" + appId + "]:   val added=[" + val + "] index=[" + index + "]");
      }
    }
  }

  protected void validate() throws Throwable {
    synchronized (validationArray) {
      for (int i = 0; i < iterationCount * getParticipantCount() ; i++) {
        System.out.println("****** appId[" + appId + "]:   index=[" + i + "]");
        System.out.println("***** " + validationArray.get(i));
        
        boolean val = myArrayTestRoot[(i + 1) % myArrayTestRoot.length].equals(validationArray.get(i));
        if (!val) { throw new AssertionError("Expecting <" + myArrayTestRoot[(i + 1) % myArrayTestRoot.length]
                                             + "> but got <" + validationArray.get(i) + ">"); }
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

//  public static void main(String[] args) throws Throwable {
//    MutateValidateArrayTestApp app = new MutateValidateArrayTestApp();
//    app.doValidate();
//  }
}
