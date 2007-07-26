package com.tctest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class EhcacheEvictionTestApp extends AbstractErrorCatchingTransparentApp {
	private static final int NUM_OF_CACHE_ITEMS = 1000;

	private static final int TIME_TO_LIVE_IN_SECONDS = 200;

	static final int EXPECTED_THREAD_COUNT = 4;

	private final CyclicBarrier barrier;

	private final CacheManager cacheManager;

	/**
	 * Test that Ehcache's CacheManger and Cache objects can be clustered.
	 * 
	 * @param appId
	 * @param cfg
	 * @param listenerProvider
	 */
	public EhcacheEvictionTestApp(final String appId,
			final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
		super(appId, cfg, listenerProvider);
		barrier = new CyclicBarrier(getParticipantCount());
		cacheManager = CacheManager.getInstance();
	}

	/**
	 * Inject Ehcache 1.2.4 configuration, and instrument this test class
	 * 
	 * @param visitor
	 * @param config
	 */
	public static void visitL1DSOConfig(final ConfigVisitor visitor,
			final DSOClientConfigHelper config) {
		config.addNewModule("clustered-ehcache-1.3", "1.0.0");
		config.addNewModule("clustered-commons-collections-3.1", "1.0.0");
		config.addAutolock("* *..*.*(..)", ConfigLockLevel.WRITE);

		final String testClass = EhcacheEvictionTestApp.class.getName();
		final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
		spec.addRoot("barrier", "barrier");
		new CyclicBarrierSpec().visit(visitor, config);
		config.addIncludePattern(DataWrapper.class.getName());
	}

	protected void runTest() throws Throwable {
		int index = barrier.barrier();

		if (index == 0) {
			addCache("CACHE");
		}

		barrier.barrier();

		// runSimplePutTimeoutGet(index);

		runSimplePutSimpleGet(index);

		if (index == 1) {
			shutdownCacheManager();
		}

		barrier.barrier();
		verifyCacheManagerShutdown();
		barrier.barrier();
	}

	private void runSimplePutSimpleGet(int index) throws Throwable {
		if (index == 1) {
			doPut();
			barrier.barrier();
		} else {
			barrier.barrier();

			long startGetTime = System.currentTimeMillis();
			doGet();
			long endGetTime = System.currentTimeMillis();

			System.err.println("Time to get " + NUM_OF_CACHE_ITEMS + " items: "
					+ (endGetTime - startGetTime) + " ms.");
		}

		barrier.barrier();
	}

	private void runSimplePutTimeoutGet(int index) throws Throwable {
		if (index == 1) {
			doPut();
		}

		barrier.barrier();

		Thread.sleep(TIME_TO_LIVE_IN_SECONDS * 1000 + 1);

		doGetNull();

		barrier.barrier();
	}

	private void doPut() throws Throwable {
		Cache cache = cacheManager.getCache("CACHE");
		for (int i = 0; i < NUM_OF_CACHE_ITEMS; i++) {
			cache.put(new Element("key" + i, new DataWrapper("value" + i)));
		}
	}

	private void doGet() throws Throwable {
		Cache cache = cacheManager.getCache("CACHE");
		for (int i = 0; i < NUM_OF_CACHE_ITEMS; i++) {
			Object o = cache.get("key" + i);
			//Assert.assertEquals(new Element("key" + i, "value" + i), o);
		}
	}

	private void doGetNull() throws Throwable {
		Cache cache = cacheManager.getCache("CACHE");
		for (int i = 0; i < NUM_OF_CACHE_ITEMS; i++) {
			Object o = cache.get("key" + i);
			Assert.assertNull(o);
		}
	}

	/**
	 * Add a cache into the CacheManager.
	 * 
	 * @param name
	 *            The name of the cache to add
	 * @throws Throwable
	 */
	private Cache addCache(final String name) throws Throwable {
		Cache cache = new Cache(name, NUM_OF_CACHE_ITEMS, false, false, TIME_TO_LIVE_IN_SECONDS,
				TIME_TO_LIVE_IN_SECONDS);
		cacheManager.addCache(cache);

		cache = cacheManager.getCache(name);
		return cache;
	}

	/**
	 * Shuts down the clustered cache manager.
	 */
	private void shutdownCacheManager() {
		cacheManager.shutdown();
	}

	/**
	 * Verify that the clustered cache manager has shut down.
	 */
	private void verifyCacheManagerShutdown() {
		Assert.assertEquals(Status.STATUS_SHUTDOWN, cacheManager.getStatus());
	}
	
	private static class DataWrapper {
		private Object val;
		
		public DataWrapper(Object val) {
			this.val = val;
		}
		
		public int hashCode() {
			return val.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (!(obj instanceof DataWrapper)) { return false; }
			return val.equals(((DataWrapper)obj).val);
		}
	}
}
