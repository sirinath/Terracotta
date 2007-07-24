/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tcclient.ehcache.TimeExpiryMap;
import com.tctest.runner.AbstractTransparentApp;

public class TimeExpiryMapTestApp extends AbstractTransparentApp {
  private final CyclicBarrier barrier;
  private DataRoot            dataRoot = new DataRoot(new MockTimeExpiryMap(1, 5));

  public TimeExpiryMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      //basicMapTest(index);
      expiredItemsTest(index);

    } catch (Throwable t) {
      notifyError(t);
    }
  }
  
  private void basicMapTest(int index) throws Exception {
    if (index == 0) {
      dataRoot.put("key1", "val1");
      dataRoot.put("key2", "val2");
      dataRoot.put("key3", "val3");
    }
    
    barrier.barrier();
    
    Assert.assertEquals(3, dataRoot.size());
    Assert.assertEquals("val1", dataRoot.get("key1"));
    Assert.assertEquals("val2", dataRoot.get("key2"));
    Assert.assertEquals("val3", dataRoot.get("key3"));
    
    barrier.barrier();
    
    Thread.sleep(7000);
    
    Assert.assertEquals(0, dataRoot.size());
    
    barrier.barrier();
    
    Assert.assertEquals(3, dataRoot.getNumOfExpired());
  }
  
  private void expiredItemsTest(int index) throws Exception {
    if (index == 0) {
      dataRoot.setMap(new MockTimeExpiryMap(1, 5));
    }
    
    barrier.barrier();
    
    if (index == 1) {
      dataRoot.put("key1", "val1");
      dataRoot.put("key2", "val2");
      dataRoot.put("key3", "val3");
    }
    
    barrier.barrier();
    
    Assert.assertEquals(3, dataRoot.size());
    Assert.assertEquals("val1", dataRoot.get("key1"));
    Assert.assertEquals("val2", dataRoot.get("key2"));
    Assert.assertEquals("val3", dataRoot.get("key3"));
    
    barrier.barrier();
    
    Thread.sleep(3000);
    
    if (index == 0) {
      Assert.assertEquals("val3", dataRoot.get("key3"));
      
      Thread.sleep(3000);
      
      //Assert.assertEquals(1, dataRoot.size());
    }
    
    barrier.barrier();
    
    if (index != 0) {
//    Assert.assertEquals(0, dataRoot.getNumOfExpired());
      Assert.assertEquals("val1", dataRoot.get("key1"));
      Assert.assertEquals("val2", dataRoot.get("key2"));
    }
    barrier.barrier();
    
    if (index == 0) {
      Assert.assertEquals(2, dataRoot.getNumOfExpired());
      Assert.assertEquals(null, dataRoot.get("key1"));
      Assert.assertEquals(null, dataRoot.get("key12"));
    }
    
    barrier.barrier();
    System.err.println("index: " + index + ", size: " + dataRoot.size());
    
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = TimeExpiryMapTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$DataRoot");
    config.addIncludePattern(testClass + "$MockTimeExpiryMap");
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("dataRoot", "dataRoot");
  }

  private static class DataRoot {
    private MockTimeExpiryMap map;

    public DataRoot(MockTimeExpiryMap map) {
      super();
      this.map = map;
    }
    
    public synchronized void put(Object key, Object val) {
      map.put(key, val);
    }
    
    public synchronized Object get(Object key) {
      return map.get(key);
    }
    
    public synchronized int size() {
      return map.size();
    }
    
    public synchronized int getNumOfExpired() {
      return map.getNumOfExpired();
    }
    
    public synchronized void setMap(MockTimeExpiryMap map) {
      this.map = map;;
    }
  }
  
  private static class MockTimeExpiryMap extends TimeExpiryMap {
    private int numOfExpired = 0;
    
    public MockTimeExpiryMap(int invalidatorSleepSeconds, int maxIdleTimeoutSeconds) {
      super(invalidatorSleepSeconds, maxIdleTimeoutSeconds);
    }
    
    protected final void processExpired(Object key, Object value) {
      System.err.println("Expiring ... key: " + key + ", value: " + value);
      numOfExpired++;
    }
    
    public int getNumOfExpired() {
      return this.numOfExpired;
    }
  }

}
