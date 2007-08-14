/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;

public class CacheEvictorTestApp extends AbstractErrorCatchingTransparentApp {

  private CyclicBarrier barrier;
  private CacheManager  cacheManager;

  public CacheEvictorTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    cacheManager = CacheManager.create(getClass().getResource("cache-evictor-test.xml"));
  }

  protected void runTest() throws Throwable {
    System.out.println(Arrays.asList(cacheManager.getCacheNames()));

    try {
      testIsElementInMemory();
      testIsKeyInCache();
      

      // DEV-886
      // testIsValueInCache();

      // DEV-887
      // testEntryExpired();
      
      barrier.await();
    } finally {
      cacheManager.shutdown();
    }

  }

  private void testIsElementInMemory() throws Exception {
    Cache cache = cacheManager.getCache("sampleCache1");
    populateCache(cache);
    barrier.await();

    try {
      Assert.assertTrue(cache.isElementInMemory("k1"));
      Assert.assertTrue(cache.isElementInMemory("k2"));
      Assert.assertTrue(cache.isElementInMemory("k3"));
    } finally {
      clearCache(cache);
    }
  }

  private void populateCache(Cache cache) throws Exception {
    if (barrier.await() == 0) {
      cache.put(new Element("k1", "v1"));
      cache.put(new Element("k2", "v2"));
      cache.put(new Element("k3", "v3"));
    }
  }

  private void clearCache(Cache cache) throws Exception {
    if (barrier.await() == 0) {
      cache.removeAll();
    }
  }

  private void testIsKeyInCache() throws Exception {
    Cache cache = cacheManager.getCache("sampleCache1");
    populateCache(cache);
    barrier.await();

    try {
      Assert.assertTrue(cache.isKeyInCache("k1"));
      Assert.assertTrue(cache.isKeyInCache("k2"));
      Assert.assertTrue(cache.isKeyInCache("k3"));
    } finally {
      clearCache(cache);
    }
  }

  private void testIsValueInCache() throws Exception {
    Cache cache = cacheManager.getCache("sampleCache1");
    populateCache(cache);
    barrier.await();

    try {
      Assert.assertTrue(cache.isValueInCache("v1"));
      Assert.assertTrue(cache.isValueInCache("v2"));
      Assert.assertTrue(cache.isValueInCache("v3"));
    } finally {
      clearCache(cache);
    }
  }

  private void testEntryExpired() throws Exception {
    Cache cache = cacheManager.getCache("sampleCache1");
    populateCache(cache);
    barrier.await();

    Element e3 = cache.get("k3");
    long timeout = System.currentTimeMillis() + (60 * 1000);
    while (System.currentTimeMillis() < timeout) {
      cache.get("k1");
      cache.get("k2");
    }

    try {
      // k3,v3 should be timed out by now
      System.out.println(cache);
      System.out.println(cache.get("k1"));
      System.out.println(cache.get("k2"));
      System.out.println(cache.get("k3"));
      Assert.assertFalse(cache.isElementInMemory("k3"));
      Assert.assertTrue(cache.isExpired(e3));            
    } finally {
      clearCache(cache);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addNewModule("clustered-ehcache-1.3", "1.0.0");

    String testClass = CacheEvictorTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("cacheManager", "cacheManager");
  }
}
