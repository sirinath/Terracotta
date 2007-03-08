/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public abstract class AbstractMutateValidateTransparentApp extends ServerCrashingAppBase {
  public static final String  VALIDATOR_COUNT = "validator-count";
  public static final String  MUTATOR_COUNT   = "mutator-count";

  // roots
  private final CyclicBarrier mutatorBarrier;
  private final CyclicBarrier allBarrier;

  protected final int         validatorCount;
  protected final int         mutatorCount;
  private final Class         type;

  public AbstractMutateValidateTransparentApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider,
                                              Class type) {
    super(appId, cfg, listenerProvider);
    this.type = type;

    validatorCount = Integer.valueOf(cfg.getAttribute(VALIDATOR_COUNT)).intValue();
    mutatorCount = Integer.valueOf(cfg.getAttribute(MUTATOR_COUNT)).intValue();

    mutatorBarrier = new CyclicBarrier(mutatorCount);
    allBarrier = new CyclicBarrier(mutatorCount + validatorCount);
  }

  // used by validators
  public AbstractMutateValidateTransparentApp() {
    super();

    mutatorBarrier = new CyclicBarrier(0);
    allBarrier = new CyclicBarrier(0);

    mutatorCount = mutatorBarrier.parties();

    if (mutatorCount == 0) { throw new AssertionError("Mutator count is zero!"); }

    int allCount = allBarrier.parties();

    if (allCount == 0) { throw new AssertionError("All-count is zero!"); }

    validatorCount = allCount - mutatorCount;
    if (validatorCount < 0) { throw new AssertionError("Validation count is a neg num!"); }

    this.type = null;
  }

  protected void runTest() throws Throwable {

    doMutate();
    int num = mutatorBarrier.barrier();

    if (num == 0) {
      startValidators();
    }

    doValidate();
    allBarrier.barrier();

  }

  private void startValidators() throws Exception {
    // write out config
    for (int i = 0; i < validatorCount; i++) {
      spawnNewClient("" + i, type);
    }
  }

  private void doMutate() throws Throwable {
    mutate();
  }

  protected void doValidate() throws Throwable {
    validate();
    allBarrier.barrier();
  }

  protected abstract void mutate() throws Throwable;

  protected abstract void validate() throws Throwable;

  // public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
  // String testClass = AbstractMutateValidateTransparentApp.class.getName();
  // TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
  //
  // spec.addRoot("mutatorBarrier", "mutatorBarrier");
  // spec.addRoot("allBarrier", "allBarrier");
  //
  // String methodExpression = "* " + testClass + "*.*(..)";
  // config.addWriteAutolock(methodExpression);
  //
  // new CyclicBarrierSpec().visit(visitor, config);
  // }

}
