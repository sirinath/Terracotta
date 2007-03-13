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
import com.tc.util.TCTimeoutException;

public class ControlImpl implements Control {
  private static final int    NOT_SET = -1;
  private final int           startParties;
  private final int           completeParties;
  private final CyclicBarrier startBarrier;
  private final CountDown     countdown;
  private final boolean       isMutateValidateTest;
  private final int           validatorCount;
  private CountDown           mutationCompleteCount;

  public ControlImpl(int parties) {
    this(parties, parties);
  }

  public ControlImpl(int startParties, int completeParties) {
    this(startParties, completeParties, false, 0);
  }

  // Mutate-validate tests should use this constructor.
  public ControlImpl(int mutatorCount, boolean isMutateValidateTest, int validatorCount) {
    this(mutatorCount, mutatorCount + validatorCount, isMutateValidateTest, validatorCount);
  }

  public ControlImpl(int startParties, int completeParties, boolean isMutateValidateTest, int validatorCount) {
    if (completeParties != (startParties + validatorCount)) { throw new AssertionError(
                                                                                       "completeParties["
                                                                                           + this.completeParties
                                                                                           + "] does not equal startParties["
                                                                                           + this.startParties
                                                                                           + "] + validatorCount["
                                                                                           + this.validatorCount + "]"); }

    this.startParties = startParties;
    this.startBarrier = new CyclicBarrier(startParties);

    mutationCompleteCount = new CountDown(startParties);
    this.validatorCount = validatorCount;

    this.isMutateValidateTest = isMutateValidateTest;

    if (this.isMutateValidateTest) {

      System.err.println("******* isMutateValidateTest=[" + isMutateValidateTest + "]");
      System.err.println("####### completeParties=[" + completeParties + "]");

      if (completeParties == NOT_SET) {
        this.completeParties = startParties + validatorCount;
      } else {
        this.completeParties = completeParties;
      }
    } else if (completeParties == NOT_SET) {
      this.completeParties = startParties;
    } else {
      this.completeParties = completeParties;
    }

    System.err.println("####### this.completeParties=[" + this.completeParties + "]");
    this.countdown = new CountDown(this.completeParties);
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
    return getClass().getName() + "[ startParties=" + startParties + ", completeParties=" + completeParties
           + ", startBarrier=" + startBarrier + ", countdown=" + countdown + ", mutationCompleteCount="
           + mutationCompleteCount + ", isMutateValidateTest=" + isMutateValidateTest + ", validatorCount="
           + validatorCount + " ]";
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

  public boolean waitForMutationComplete(long timeout) throws InterruptedException {
    if (timeout < 0) {
      while (true) {
        synchronized (this) {
          wait();
        }
      }
    }
    try {
      boolean rv = this.mutationCompleteCount.attempt(timeout);
      return rv;
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

}