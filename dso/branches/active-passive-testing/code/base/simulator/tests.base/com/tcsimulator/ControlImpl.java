/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CountDown;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CountDownSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.control.Control;
import com.tc.simulator.control.TCBrokenBarrierException;
import com.tc.simulator.listener.MutationCompletionListener;
import com.tc.util.TCTimeoutException;

public class ControlImpl implements Control, MutationCompletionListener {
  private final int           mutatorCount;
  private final int           completeParties;
  private final CyclicBarrier startBarrier;
  private final CountDown     countdown;
  private final int           validatorCount;
  private CountDown           mutationCompleteCount;
  private final Control       testWideControl;
  private long                executionTimeout;

  public ControlImpl(int mutatorCount) {
    this(mutatorCount, 0);
  }

  // used to create container-wide control
  public ControlImpl(int mutatorCount, Control testWideControl) {
    this(mutatorCount, 0, testWideControl);
  }

  // used to create test-wide control
  public ControlImpl(int mutatorCount, int validatorCount) {
    this(mutatorCount, validatorCount, null);
  }

  public ControlImpl(int mutatorCount, int validatorCount, Control testWideControl) {
    if (mutatorCount < 0 || validatorCount < 0) { throw new AssertionError(
                                                                           "MutatorCount and validatorCount must be non-negative numbers!  mutatorCount=["
                                                                               + mutatorCount + "] validatorCount=["
                                                                               + validatorCount + "]"); }

    this.executionTimeout = -1;
    this.testWideControl = testWideControl;
    this.mutatorCount = mutatorCount;
    this.validatorCount = validatorCount;
    this.completeParties = mutatorCount + validatorCount;
    // TODO: remove
    System.err.println("####### completeParties=[" + completeParties + "]");

    startBarrier = new CyclicBarrier(mutatorCount);
    mutationCompleteCount = new CountDown(mutatorCount);
    countdown = new CountDown(this.completeParties);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String classname = ControlImpl.class.getName();
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
    String classname = ControlImpl.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");

    new CyclicBarrierSpec().visit(visitor, config);
    new CountDownSpec().visit(visitor, config);

  }

  public String toString() {
    return getClass().getName() + "[ mutatorCount=" + mutatorCount + ", completeParties=" + completeParties
           + ", startBarrier=" + startBarrier + ", countdown=" + countdown + ", mutationCompleteCount="
           + mutationCompleteCount + ", validatorCount=" + validatorCount + " ]";
  }

  public void waitForStart(long timeout) throws InterruptedException, TCBrokenBarrierException, TCTimeoutException {
    try {
      try {
        this.startBarrier.attemptBarrier(timeout);
      } catch (InterruptedException e1) {
        throw e1;
      }
    } catch (TimeoutException e) {
      throw new TCTimeoutException(e);
    } catch (BrokenBarrierException e) {
      throw new TCBrokenBarrierException(e);
    }
  }

  public void notifyMutationComplete() {
    mutationCompleteCount.release();
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
      boolean rv = mutationCompleteCount.attempt(timeout);
      return rv;
    } catch (InterruptedException e) {
      throw e;
    }
  }

  /*
   * MutationCompletionListener interface method -- called by applications
   */
  public void waitForMutationCompleteTestWide() throws Exception {
    try {
      if (testWideControl == null) { throw new AssertionError(
                                                              "Application should be calling this on container-wide control, not test-wide control."); }
      checkExecutionTimeout(executionTimeout);
      boolean rv = testWideControl.waitForMutationComplete(executionTimeout);
      if (!rv) { throw new RuntimeException("Wait on MutationCompletionCount did not pass:  executionTimeout=[" + executionTimeout + "] "); }
    } catch (InterruptedException e) {
      throw e;
    }
  }

  // TODO: remove debug statement
  public void notifyComplete() {
    System.err.println("*******  countdown called:  control=[" + toString() + "]");
    this.countdown.release();
  }

  public boolean waitForAllComplete(long timeout) throws InterruptedException {
    if (timeout < 0) {
      while (true) {
        synchronized (this) {
          wait();
        }
      }
    }
    try {
      boolean rv = this.countdown.attempt(timeout);
      return rv;
    } catch (InterruptedException e) {
      throw e;
    }
  }

  public void setExecutionTimeout(long timeout) {
    checkExecutionTimeout(timeout);
    executionTimeout = timeout;
  }

  private void checkExecutionTimeout(long timeout) {
    if (timeout < 0) { throw new AssertionError("Execution timeout should be a non-negative number:  timeout=["
                                                + timeout + "]"); }
  }
}