/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public abstract class AbstractMutateValidateTransparentApp extends AbstractErrorCatchingTransparentApp {
  public static final String     VALIDATOR_COUNT = "validator-count";
  public static final String     MUTATOR_COUNT   = "mutator-count";

  protected final int            validatorCount;
  protected final int            mutatorCount;
  private final boolean          isMutator;
  private final ListenerProvider listenerProvider;
  private final String           appId;

  public AbstractMutateValidateTransparentApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appId = appId;
    this.listenerProvider = listenerProvider;

    validatorCount = cfg.getValidatorCount();
    mutatorCount = cfg.getGlobalParticipantCount();
    isMutator = Boolean.valueOf(cfg.getAttribute(appId)).booleanValue();

    // TODO: remove
    System.out.println("***** appId=[" + appId + "]:  isMutator=[" + isMutator + "]");
  }

  // mutator: mutate, wait until all mutation is done, then validate
  // validator: wait until all mutation is done, then validate
  public void runTest() throws Throwable {
    if (isMutator) {
      System.out.println("***** appId[" + appId + "]: starting mutate");
      mutate();
      System.out.println("***** appId[" + appId + "]: finished mutate");
      notifyMutationComplete();
      System.out.println("***** appId[" + appId + "]: notified mutate-listener... waiting for mutate stage to finish");
      waitForMutationComplete();
      System.out.println("***** appId[" + appId + "]: mutate stage complete");
    }

    System.out.println("***** appId[" + appId + "]:  before sleep [" + System.currentTimeMillis() + "]ms");
    Thread.sleep(10000);
    System.out.println("***** appId[" + appId + "]:  after sleep [" + System.currentTimeMillis() + "]ms");
    
    notifyValidationStart();
    System.out.println("***** appId[" + appId + "]: notified mutate-listener... waiting for validat stage to start");
    waitForValidationStart();

    System.out.println("***** appId[" + appId + "]: starting validate");
    validate();
    System.out.println("***** appId[" + appId + "]: finished validate");
  }

  private final void waitForValidationStart() throws Exception {
    listenerProvider.getMutationCompletionListener().waitForValidationStartTestWide();
  }

  private final void notifyMutationComplete() {
    listenerProvider.getMutationCompletionListener().notifyMutationComplete();
  }

  private final void notifyValidationStart() {
    listenerProvider.getMutationCompletionListener().notifyValidationStart();
  }

  private final void waitForMutationComplete() throws Exception {
    listenerProvider.getMutationCompletionListener().waitForMutationCompleteTestWide();
  }

  protected abstract void mutate() throws Throwable;

  protected abstract void validate() throws Throwable;

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = AbstractMutateValidateTransparentApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
  }

}
