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
import com.tc.simulator.listener.MutationCompletionListener;

public class ApplicationControlImpl implements Control, MutationCompletionListener {
  private static final boolean DEBUG = false;

  private final int            completeParties;
  private final CyclicBarrier  startBarrier;

  private final CountDown      validationStartCount;
  private final CountDown      countdown;
  private final Control        globalControl;

  private CountDown            completeCount;
  private long                 executionTimeout;

  public ApplicationControlImpl(int clientCount, Control globalControl) {
    if (clientCount < 0) { throw new AssertionError("clientCount must be non-negative numbers!  clientCount=["
                                                    + clientCount + "]"); }
    if (globalControl == null) { throw new AssertionError("globalControl is null!"); }

    executionTimeout = -1;
    this.globalControl = globalControl;
    completeParties = clientCount;

    debugPrintln("####### completeParties=[" + completeParties + "]");

    startBarrier = new CyclicBarrier(completeParties);
    completeCount = new CountDown(completeParties);
    countdown = new CountDown(completeParties);

    validationStartCount = new CountDown(completeParties);
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
      boolean rv = completeCount.attempt(timeout);
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

  /*
   * MutationCompletionListener interface method -- called by applications
   */
  public void waitForMutationCompleteTestWide() throws Exception {
    try {
      checkExecutionTimeout(executionTimeout);
      boolean rv = globalControl.waitForMutationComplete(executionTimeout);
      if (!rv) { throw new RuntimeException("Wait on MutationCompletionCount did not pass:  executionTimeout=["
                                            + executionTimeout + "] "); }
    } catch (InterruptedException e) {
      throw e;
    }
  }

  /*
   * MutationCompletionListener interface method -- called by applications
   */
  public void waitForValidationStartTestWide() throws Exception {
    try {
      checkExecutionTimeout(executionTimeout);
      boolean rv = globalControl.waitForValidationStart(executionTimeout);
      if (!rv) { throw new RuntimeException("Wait on ValidationStartCount did not pass:  executionTimeout=["
                                            + executionTimeout + "] "); }
    } catch (InterruptedException e) {
      throw e;
    }
  }

  /*
   * MutationCompletionListener and Control interface method
   */
  public void notifyValidationStart() {
    debugPrintln("********  validation.release() called:  init=[" + validationStartCount.initialCount() + "] before=["
                 + validationStartCount.currentCount() + "]");
    validationStartCount.release();
    debugPrintln("******* validation.release() called:  after=[" + validationStartCount.currentCount() + "]");
  }

  /*
   * MutationCompletionListener and Control interface method
   */
  public void notifyMutationComplete() {
    completeCount.release();
  }

  public void setExecutionTimeout(long timeout) {
    checkExecutionTimeout(timeout);
    executionTimeout = timeout;
  }

  private void checkExecutionTimeout(long timeout) {
    if (timeout < 0) { throw new AssertionError("Execution timeout should be a non-negative number:  timeout=["
                                                + timeout + "]"); }
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  public String toString() {
    return getClass().getName() + "[ completeParties=" + completeParties + ", startBarrier=" + startBarrier
           + ", countdown=" + countdown + ", completeCount=" + completeCount + " ]";
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String classname = ApplicationControlImpl.class.getName();
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
    String classname = ApplicationControlImpl.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");

    new CyclicBarrierSpec().visit(visitor, config);
    new CountDownSpec().visit(visitor, config);

  }

}