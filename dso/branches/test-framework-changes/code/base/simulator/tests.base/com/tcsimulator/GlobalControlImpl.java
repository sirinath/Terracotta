/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CountDown;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CountDownSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.control.Control;
import com.tc.simulator.control.TCBrokenBarrierException;

public class GlobalControlImpl implements Control {
  private static final boolean DEBUG = false;

  private final int            mutatorCount;
  private final int            completeParties;
  private final CyclicBarrier  startBarrier;
  private final CountDown      validationStartCount;
  private final CountDown      countdown;
  private final int            validatorCount;
  private final boolean        pauseAfterMutate;

  private CountDown            mutationCompleteCount;

  public GlobalControlImpl(int mutatorCount) {
    this(mutatorCount, 0, false);
  }

  public GlobalControlImpl(int mutatorCount, int validatorCount, boolean pauseAfterMutate) {
    if (mutatorCount < 0 || validatorCount < 0) { throw new AssertionError(
                                                                           "MutatorCount and validatorCount must be non-negative numbers!  mutatorCount=["
                                                                               + mutatorCount + "] validatorCount=["
                                                                               + validatorCount + "]"); }

    this.pauseAfterMutate = pauseAfterMutate;
    this.mutatorCount = mutatorCount;
    this.validatorCount = validatorCount;
    completeParties = mutatorCount + validatorCount;

    debugPrintln("####### completeParties=[" + completeParties + "]");

    startBarrier = new CyclicBarrier(this.mutatorCount);
    mutationCompleteCount = new CountDown(this.mutatorCount);
    countdown = new CountDown(completeParties);

    // "1" indicates the server
    if (this.pauseAfterMutate) {
      validationStartCount = new CountDown(completeParties + 1);
    } else {
      validationStartCount = new CountDown(completeParties);
    }
  }

  public String toString() {
    return getClass().getName() + "[ mutatorCount=" + mutatorCount + ", completeParties=" + completeParties
           + ", startBarrier=" + startBarrier + ", countdown=" + countdown + ", mutationCompleteCount="
           + mutationCompleteCount + ", validatorCount=" + validatorCount + " ]" + ", pauseAfterMutate=["
           + pauseAfterMutate + "]";
  }

  /*
   * Control interface method
   */
  public void notifyMutationComplete() {
    mutationCompleteCount.release();
  }

  /*
   * Control interface method
   */
  public void notifyValidationStart() {
    debugPrintln("********  validation.release() called:  init=[" + validationStartCount.initialCount() + "] before=["
                 + validationStartCount.currentCount() + "]");
    validationStartCount.release();
    debugPrintln("******* validation.release() called:  after=[" + validationStartCount.currentCount() + "]");
  }

  /*
   * Control interface method
   */
  public boolean waitForMutationComplete(long timeout) throws InterruptedException {
    if (timeout < 0) {
      while (true) {
        synchronized (this) {
          wait();
        }
      }
    }
    try {
      checkExecutionTimeout(timeout);
      boolean rv = mutationCompleteCount.attempt(timeout);
      return rv;
    } catch (InterruptedException e) {
      throw e;
    }
  }

  /*
   * Control interface method
   */
  public boolean waitForValidationStart(long timeout) throws InterruptedException {
    debugPrintln("*******  waitForValidationStart:  validationStartCount=[" + validationStartCount.currentCount() + "]");
    if (timeout < 0) {
      while (true) {
        synchronized (this) {
          wait();
        }
      }
    }
    try {
      checkExecutionTimeout(timeout);
      boolean rv = validationStartCount.attempt(timeout);
      return rv;
    } catch (InterruptedException e) {
      throw e;
    }
  }

  /*
   * Control interface method
   */
  public void waitForStart() throws InterruptedException, TCBrokenBarrierException {
    try {
      try {
        this.startBarrier.barrier();
      } catch (InterruptedException e1) {
        throw e1;
      }
    } catch (BrokenBarrierException e) {
      throw new TCBrokenBarrierException(e);
    }
  }

  /*
   * Control interface method
   */
  public void notifyComplete() {
    debugPrintln("*******  countdown called:  control=[" + toString() + "]");
    this.countdown.release();
  }

  /*
   * Control interface method
   */
  public boolean waitForAllComplete(long timeout) throws InterruptedException {
    if (timeout < 0) {
      while (true) {
        synchronized (this) {
          wait();
        }
      }
    }
    try {
      checkExecutionTimeout(timeout);
      boolean rv = this.countdown.attempt(timeout);
      return rv;
    } catch (InterruptedException e) {
      throw e;
    }
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  private void checkExecutionTimeout(long timeout) {
    if (timeout < 0) { throw new AssertionError("Execution timeout should be a non-negative number:  timeout=["
                                                + timeout + "]"); }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String classname = GlobalControlImpl.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");

    String cyclicBarrierClassname = CyclicBarrier.class.getName();
    config.addIncludePattern(cyclicBarrierClassname);
    config.addWriteAutolock("* " + cyclicBarrierClassname + ".*(..)");

    String countdownClassname = CountDown.class.getName();
    config.addIncludePattern(countdownClassname);
    config.addWriteAutolock("* " + countdownClassname + ".*(..)");
  }

  public static void visitDSOApplicationConfig(com.tc.object.config.ConfigVisitor visitor,
                                               com.tc.object.config.DSOApplicationConfig config) {
    String classname = GlobalControlImpl.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");

    new CyclicBarrierSpec().visit(visitor, config);
    new CountDownSpec().visit(visitor, config);

  }
}