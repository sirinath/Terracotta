/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Arrays;

public class CacheEvictorTestApp extends AbstractErrorCatchingTransparentApp {

  private CyclicBarrier barrier;
  private CacheManager  cacheManager;

  public CacheEvictorTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  protected void runTest() throws Throwable {
    if (barrier.barrier() == 0) {
      cacheManager = CacheManager.create(getClass().getResource("cache-evictor-test.xml"));
    }

    barrier.barrier();

    System.out.println(Arrays.asList(cacheManager.getCacheNames()));

    try {
      testIsElementInMemory();
      testIsKeyInCache();
      testIsValueInCache();
      testElementExpired();

      barrier.barrier();
    } finally {
      cacheManager.shutdown();
    }

  }

  private void testIsElementInMemory() throws Exception {
    Cache cache = cacheManager.getCache("sampleCache1");
    populateCache(cache);
    barrier.barrier();

    Assert.assertTrue(cache.isElementInMemory("k1"));
    Assert.assertTrue(cache.isElementInMemory("k2"));
    Assert.assertTrue(cache.isElementInMemory("k3"));
  }

  private void populateCache(Cache cache) throws Exception {
    if (barrier.barrier() == 0) {
      cache.removeAll();
      cache.put(new Element("k1", "v1"));
      cache.put(new Element("k2", "v2"));
      cache.put(new Element("k3", "v3"));
    }
  }

  private void testIsKeyInCache() throws Exception {
    Cache cache = cacheManager.getCache("sampleCache1");
    populateCache(cache);
    barrier.barrier();

    Assert.assertTrue(cache.isKeyInCache("k1"));
    Assert.assertTrue(cache.isKeyInCache("k2"));
    Assert.assertTrue(cache.isKeyInCache("k3"));
  }

  private void testIsValueInCache() throws Exception {
    Cache cache = cacheManager.getCache("sampleCache1");
    populateCache(cache);
    barrier.barrier();

    Assert.assertTrue(cache.isValueInCache("v1"));
    Assert.assertTrue(cache.isValueInCache("v2"));
    Assert.assertTrue(cache.isValueInCache("v3"));
  }

  private void testElementExpired() throws Exception {
    Cache cache = cacheManager.getCache("sampleCache1");
    populateCache(cache);
    barrier.barrier();

    Element e1 = cache.get("k1");
    Element e2 = cache.get("k2");
    Element e3 = cache.get("k3");
    long timeout = System.currentTimeMillis() + (30 * 1000);
    while (System.currentTimeMillis() < timeout) {
      cache.get("k1");
      cache.get("k2");
      Thread.sleep(100);
    }

    // k3,v3 should expire due to timeToIdleSeconds=15
    System.out.println(cache);
    System.out.println(cache.get("k1"));
    System.out.println(cache.get("k2"));
    System.out.println(cache.get("k3"));

    assertExpired(cache, e3);
    assertInCache(cache, e1);
    assertInCache(cache, e2);

    timeout = System.currentTimeMillis() + (70 * 1000);
    while (System.currentTimeMillis() < timeout) {
      cache.get("k1");
      Thread.sleep(100);
    }

    System.out.println(cache.get("k1"));
    System.out.println(cache.get("k2"));
    System.out.println(cache.get("k3"));

    // both (k1,v2) and (k2,v2) should expired due to timeToLiveSeconds=60
    assertExpired(cache, e1);
    assertExpired(cache, e2);
    assertExpired(cache, e3);
  }
  
  private void assertInCache(Cache cache, Element e) throws Exception {
    Assert.assertTrue("Should be in cache", cache.isKeyInCache(e.getKey()));
    Assert.assertTrue("Should be in memory", cache.isElementInMemory(e.getKey()));
    Assert.assertFalse("Should not expire", cache.isExpired(e));
  }

  private void assertExpired(Cache cache, Element e) throws Exception {
    Assert.assertFalse("Should not be in cache", cache.isKeyInCache(e.getKey()));
    Assert.assertFalse("Should not be in memory", cache.isElementInMemory(e.getKey()));
    Assert.assertTrue("Should expired", cache.isExpired(e));
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addNewModule("clustered-ehcache-1.2.4", "1.0.0");

    String testClass = CacheEvictorTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("cacheManager", "cacheManager");

    new CyclicBarrierSpec().visit(visitor, config);
  }
}
