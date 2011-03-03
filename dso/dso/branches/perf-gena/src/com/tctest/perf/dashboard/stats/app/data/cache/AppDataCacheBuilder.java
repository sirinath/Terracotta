/**
 * 
 */
package com.tctest.perf.dashboard.stats.app.data.cache;

import com.tctest.perf.dashboard.common.cache.Cache;
import com.tctest.perf.dashboard.common.cache.CacheException;
import com.tctest.perf.dashboard.common.cache.CacheFactory;
import com.tctest.perf.dashboard.common.metadata.MetaData;
import com.tctest.perf.dashboard.stats.app.AppEventStatistics;
import com.tctest.perf.dashboard.stats.app.metadata.AppMetaData;

/**
 * 
 * A Builder that builds the Cache for Application Data when furnished with a
 * metadata object.
 * 
 */
public class AppDataCacheBuilder {

	/**
	 * 
	 * @return AppDataCache
	 * @throws CacheException
	 */
	public AppDataCache build(AppMetaData appMetaData) throws CacheException {

		if (appMetaData == null)
			throw new CacheException(
					"AppMetaData not set. Could not build the Cache");

		MetaData<AppEventStatistics> metaData = appMetaData.getMetaData();

		if (metaData == null)
			throw new CacheException(
					"AppMetaData not initialized. Could not build the Cache");

		Cache<AppEventStatistics> cache = CacheFactory.createCache(metaData);

		AppDataCache appDataCache = new AppDataCache();
		// if using terracotta ... the cache set here would be shared across the
		// cluster
		appDataCache.setCache(cache);

		return appDataCache;
	}

}
