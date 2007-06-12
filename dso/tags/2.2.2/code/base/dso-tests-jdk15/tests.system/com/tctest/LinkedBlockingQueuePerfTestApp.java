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
import com.tctest.runner.AbstractTransparentApp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LinkedBlockingQueuePerfTestApp extends AbstractTransparentApp {
  private static final int              NO_OF_OBJECTS = 2000;
  private static final int              NO_OF_LEVEL   = 20;

  private static final SimpleDateFormat dateFormat    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

  private LinkedBlockingQueue           queue         = new LinkedBlockingQueue();
  private final CyclicBarrier           barrier;

  public LinkedBlockingQueuePerfTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      runTest(index);

      barrier.await();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest(int index) throws Exception {
    if (index == 0) {
      populateQueue(index);
    } else {
      waitForPopulation();
      startReaders(index);
    }
  }

  private void waitForPopulation() {
    synchronized (queue) {
      while (queue.size() == 0) { // Explicitly not doing spin lock for a reason
        try {
          queue.wait(1000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  private void startReaders(int index) throws Exception {
    log("Starting Readers", index);
    int count = 5;
    CyclicBarrier localBarrier = new CyclicBarrier(count + 1);
    while (count-- > 0) {
      Thread t = new Thread(new PollRunnable(index, localBarrier), "Reader-" + count);
      t.start();
    }
    localBarrier.await();
  }

  private void populateQueue(int index) {
    if (index == 0) {
      log("Populating queue", index);
      int count = NO_OF_OBJECTS;
      while (count-- > 0) {
        queue.add(new DeepLargeObject(NO_OF_LEVEL));
      }
      log("Population done", index);
      notifyPopulationComplete();
    } else {
      waitForPopulation();
    }
  }

  private void notifyPopulationComplete() {
    synchronized (queue) {
      queue.notifyAll();
    }
  }

  private void log(String message, int index) {
    System.err.println("Node ID : " + index + " Thread : " + Thread.currentThread().getName() + " : "
                       + dateFormat.format(new Date()) + " : " + message);
  }

  private class PollRunnable implements Runnable {
    private int           index;
    private CyclicBarrier barrier;

    public PollRunnable(int index, CyclicBarrier barrier) {
      this.index = index;
      this.barrier = barrier;
    }

    public void run() {
      Object o = null;
      long timeTaken = 0;
      int count = 0;
      while (true) {
        long t1 = System.currentTimeMillis();
        try {
          o = queue.poll(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        long t2 = System.currentTimeMillis();
        if (o == null) {
          break;
        }
        timeTaken += (t2 - t1);
        count++;
      }
      log("time taken = " + timeTaken + " ms count = " + count + " Avg = "
          + (count != 0 ? (timeTaken * 1.0 / count) + " ms" : "NA"), index);
      log("TPS = " + (count * 1000.0 / timeTaken), index);

      try {
        barrier.await();
      } catch (Throwable t) {
        throw new AssertionError(t);
      }
    }
  }

  public static class DeepLargeObject {
    private int             numOfLevel;

    private DeepLargeObject childObject;

    public DeepLargeObject(int numOfLevel) {
      this.numOfLevel = numOfLevel;
      if (numOfLevel > 0) {
        this.childObject = new DeepLargeObject(numOfLevel - 1);
      } else {
        this.childObject = null;
      }
    }

    public DeepLargeObject getChildObject() {
      return childObject;
    }

    public int getNumOfLevel() {
      return numOfLevel;
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LinkedBlockingQueuePerfTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("queue", "queue");
    spec.addRoot("barrier", "barrier");
  }

}
