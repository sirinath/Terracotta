/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

public class LowWaterMarkPerfGCTestApp extends AbstractErrorCatchingTransparentApp {
  private static boolean        DEBUG       = true;

  private static final long     DURATION    = 1 * 60 * 1000;                           // 1 minutes
  private static final long     END         = System.currentTimeMillis() + DURATION;
  private static final String   KEY_PREFIX  = "key";
  private static final int      TOTAL_COUNT = 10;
  private static final int      BATCH_SIZE = 100;

  // roots
  private final CyclicBarrier   barrier     = new CyclicBarrier(getParticipantCount());
  private final Map             root        = new HashMap();

  private final Random          random;
  private final String          appId;
  private static AssertionError error;

  public LowWaterMarkPerfGCTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appId = appId;

    long seed = new Random().nextLong();
    System.err.println("seed for " + getApplicationId() + " is " + seed);
    random = new Random(seed);
  }

  protected void runTest() throws Throwable {
    final int index = barrier.barrier();
    if (index == 0) {
      populate();
    } else {
      // make one transaction only.
      ThreadUtil.reallySleep(100);
      int n = BATCH_SIZE * 3;
      synchronized(root) {
        root.put(KEY_PREFIX + n, newTreeSet(getRandomNum(), getRandomNum(), n));
      }
      read();
    }

    barrier.barrier();

    if (error != null) {
      throw error;
    }
  }

  private void populate() {
    int loopCount = 0;
    debugPrintln(wrapMessage("populate..."));
    synchronized (root) {
      for (int i = 0; i < BATCH_SIZE; i++) {
        root.put(KEY_PREFIX + i, newTreeSet(getRandomNum(), getRandomNum(), i));
      }
      debugPrintln(wrapMessage("root size=[" + root.size() + "]"));
    }
    ThreadUtil.reallySleep(100);
    
    while(true) {
      int ndx = random.nextInt(10000) + BATCH_SIZE;
      int start = ndx;
      int end = ndx + BATCH_SIZE;
      for (int i = start; i < end; i++) {
        synchronized (root) {
          root.put(KEY_PREFIX + i, newTreeSet(getRandomNum(), getRandomNum(), i));
        }
      }
      // ThreadUtil.reallySleep(1);
      for (int i = start; i < end; i++) {
        synchronized (root) {
          root.remove(KEY_PREFIX + i);
        }
      }
      ++loopCount;
      if (loopCount % 10 == 0) debugPrintln(wrapMessage("LoopCount = " + loopCount));
    }
  }

  private void read() throws InterruptedException {
    int loopCount = 0;
    debugPrintln(wrapMessage("Reading..."));
    while(true) {
      synchronized (root) {
        int n = random.nextInt(root.size());
        root.get(KEY_PREFIX + n);
        ++loopCount;
        if (loopCount % 100 == 0) debugPrintln(wrapMessage("Read " + n + " total="+loopCount));
      }
      ThreadUtil.reallySleep(50);
    }
  }


  private String wrapMessage(String s) {
    return "\n  ##### appId[" + appId + "] " + s + "\n";
  }

  private static void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println("\n " + s);
    }
  }
  
  private int getRandomNum() {
    int num = random.nextInt(TOTAL_COUNT);
    if (num == 0) {
      num = 1;
    }
    return num;
  }

  private TreeSet newTreeSet(int treeSetCount, int treeSetSize, int id) {
    TreeSet newTS = new TreeSet(new NullTolerantComparator());
    for (int i = 0, n = treeSetCount; i < n; i++) { 
      newTS.add(newTreeSetWrapper(treeSetCount, treeSetSize, id, i));
    }
    return newTS;
  }

  private TreeSetWrapper newTreeSetWrapper(int treeSetCount, int treeSetSize, int outer_id, int id) {
    //debugPrintln("creating new TreeSet=[" + outer_id + "][" + id + "]:  treeSetCount=[" + treeSetCount
    //             + "] treeSetSize=[" + treeSetSize + "]");
    TreeSetWrapper newTS = new TreeSetWrapper(new TreeSet(new NullTolerantComparator()), id);
    for (int i = 0, n = treeSetSize; i < n; i++) {
      newTS.getTreeSet().add(new FooObject(i));
    }
    return newTS;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    new CyclicBarrierSpec().visit(visitor, config);

    // config.getOrCreateSpec(FooObject.class.getName());
    // config.getOrCreateSpec(TreeSetWrapper.class.getName());
    // config.getOrCreateSpec(NullTolerantComparator.class.getName());

    config.addIncludePattern(FooObject.class.getName());
    config.addIncludePattern(TreeSetWrapper.class.getName());
    config.addIncludePattern(NullTolerantComparator.class.getName());

    String testClassName = LowWaterMarkPerfGCTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
    String methodExpression = "* " + testClassName + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");

    String FooObjectExpression = "* " + FooObject.class.getName() + "*.*(..)";
    config.addWriteAutolock(FooObjectExpression);
    String TreeSetWrapperExpression = "* " + TreeSetWrapper.class.getName() + "*.*(..)";
    config.addWriteAutolock(TreeSetWrapperExpression);
    String NullTolerantComparatorExpression = "* " + NullTolerantComparator.class.getName() + "*.*(..)";
    config.addWriteAutolock(NullTolerantComparatorExpression);

  }

  private static final class TreeSetWrapper implements Comparable {
    private final int     id;
    private final TreeSet ts;

    public TreeSetWrapper(TreeSet ts, int id) {
      this.id = id;
      this.ts = ts;
    }

    public TreeSet getTreeSet() {
      return ts;
    }

    public int getId() {
      return id;
    }

    public int compareTo(Object o) {
      int othersId = ((TreeSetWrapper) o).getId();
      if (id < othersId) {
        return -1;
      } else if (id == othersId) {
        return 0;
      } else {
        return 1;
      }
    }
  }

  private static final class FooObject implements Comparable {
    private final int id;

    public FooObject(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }

    public boolean equals(Object foo) {
      if (foo == null) { return false; }
      return ((FooObject) foo).getId() == id;
    }

    public int hashCode() {
      return id;
    }

    public int compareTo(Object o) {
      int othersId = ((FooObject) o).getId();
      if (id < othersId) {
        return -1;
      } else if (id == othersId) {
        return 0;
      } else {
        return 1;
      }
    }
  }

}
