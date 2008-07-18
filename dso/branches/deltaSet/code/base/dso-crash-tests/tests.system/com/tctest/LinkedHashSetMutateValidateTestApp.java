/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CyclicBarrier;

public class LinkedHashSetMutateValidateTestApp extends AbstractTransparentApp {
  private ServerControl                 serverControl;
  private CyclicBarrier                 barrier;
  private TreeSet<PartialSetNode>       myTreeSet;
  private LinkedHashSet<PartialSetNode> myLinkedHashSet;
  private final static int              NUMBERS_ADDED = 5000;

  public LinkedHashSetMutateValidateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    serverControl = cfg.getServerControl();
    barrier = new CyclicBarrier(getParticipantCount());
    myTreeSet = new TreeSet();
    myLinkedHashSet = new LinkedHashSet();
  }

  public void run() {
    int index = waitOnBarrier();
    if (index != 0) {
      addElementsToTreeSet();
      mutate();
    }

    index = waitOnBarrier();
    if (index != 0) restartServer();
    waitOnBarrier();
    
    validate();
  }

  private void validate() {
    validateSet(myTreeSet);
    validateSet(myLinkedHashSet);
  }

  private void validateSet(Set<PartialSetNode> set) {
    int count = 0;
    synchronized (set) {
      Assert.assertEquals(NUMBERS_ADDED, set.size());
      
      for (Iterator<PartialSetNode> iter = set.iterator(); iter.hasNext();) {
        Assert.assertEquals(count, iter.next().getNumber());
        count++;
      }
    }
  }

  private void addElementsToTreeSet() {
    synchronized (myTreeSet) {
      for (int i = 0; i < NUMBERS_ADDED; i++) {
        Assert.assertTrue(myTreeSet.add(new PartialSetNode(i)));
      }
    }
  }

  private void mutate() {
    synchronized (myLinkedHashSet) {
      Iterator<PartialSetNode> iter = myTreeSet.iterator();
      while (iter.hasNext()) {
        Assert.assertTrue(myLinkedHashSet.add(iter.next()));
      }
    }
  }

  private void restartServer() {
    try {
      System.out.println("Crashing the Server ...");
      serverControl.crash();
      ThreadUtil.reallySleep(5000);
      System.out.println("Re-starting the Server ...");
      serverControl.start();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private int waitOnBarrier() {
    int index = -1;
    try {
      index = barrier.await();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return index;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LinkedHashSetMutateValidateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(LinkedHashSetMutateValidateTestApp.class.getName());
    config.addIncludePattern(PartialSetNode.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";

    spec.addRoot("myTreeSet", "myTreeSet");
    spec.addRoot("myLinkedHashSet", "myLinkedHashSet");
    spec.addRoot("barrier", "barrier");
    config.addWriteAutolock(methodExpression);
  }
}