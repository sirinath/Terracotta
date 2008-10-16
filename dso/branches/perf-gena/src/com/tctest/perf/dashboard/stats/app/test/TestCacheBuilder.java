package com.tctest.perf.dashboard.stats.app.test;

import com.tctest.perf.dashboard.stats.app.data.cache.AppDataCache;
import com.tctest.perf.dashboard.stats.app.data.cache.AppDataCacheBuilder;
import com.tctest.perf.dashboard.stats.app.metadata.AppMetaData;

/**
 * Just a utility to mash up a a Cache
 * 
 */
public class TestCacheBuilder {

	public static AppDataCache buildCache() {
		AppMetaData metaData = new AppMetaData();
		StressTestConfigurator configurator = new StressTestConfigurator();
		try {
			metaData.init(configurator);
			AppDataCacheBuilder builder = new AppDataCacheBuilder();
			return builder.build(metaData);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
