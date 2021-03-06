/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;

// test removeAll method of CopyOnWriteArrayList
public class COWALRemoveAllTestApp extends AbstractTransparentApp {

  private final CyclicBarrier        barrier;
  private final CopyOnWriteArrayList cowal = new CopyOnWriteArrayList();

  public COWALRemoveAllTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(cfg.getGlobalParticipantCount());
  }

  public void run() {
    try {
      testRemoveAll();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private void testRemoveAll() throws InterruptedException, BrokenBarrierException {
    // populate array
    int id = barrier.await();
    if (id == 0) {
      cowal.add(new Foo("string1"));
      cowal.add(new Foo("string2"));
      cowal.add(new Foo("string3"));
      cowal.add(new Foo("string4"));
    }

    // iterating though it
    barrier.await();
    Assert.assertEquals(4, cowal.size());
    for (Iterator it = cowal.iterator(); it.hasNext();) {
      System.out.println("Client " + ManagerUtil.getClientID() + " sees " + it.next());
    }

    // remove 1,3
    id = barrier.await();
    List toBeRemoved = new ArrayList(Arrays.asList(new Foo("string1"), new Foo("string3")));
    if (id == 0) {
      cowal.removeAll(toBeRemoved);
    }

    // assert what left
    barrier.await();
    Assert.assertEquals(2, cowal.size());
    Assert.assertEquals(new Foo("string2"), cowal.get(0));
    Assert.assertEquals(new Foo("string4"), cowal.get(1));

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = COWALRemoveAllTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("cowal", "cowal");
    config.addIncludePattern(testClass + "$*");
  }
  
  private static class Foo implements Comparable {
    private final String value;

    Foo(String value) {
      this.value = value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Foo) { return value.equals(((Foo) obj).value); }
      return false;
    }

    public int compareTo(Object o) {
      return value.compareTo(((Foo) o).value);
    }
    
    public String toString() {
      return "Foo " + value;
    }
  }
}
