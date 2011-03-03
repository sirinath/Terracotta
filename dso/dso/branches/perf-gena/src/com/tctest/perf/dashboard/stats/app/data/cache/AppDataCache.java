/**
 * 
 */
package com.tctest.perf.dashboard.stats.app.data.cache;

import java.util.Date;

import com.tctest.perf.dashboard.common.cache.Cache;
import com.tctest.perf.dashboard.common.cache.CacheException;
import com.tctest.perf.dashboard.common.data.DataException;
import com.tctest.perf.dashboard.common.data.Statistics;
import com.tctest.perf.dashboard.stats.app.AppEventStatistics;
import com.tctest.perf.dashboard.stats.app.data.AppStatsDataSource;

/**
 * 
 * Wrapper around the Cache for the application Data.
 * This class exists for the cache to be easily shared over terracotta.
 * 
 * The cache object should be set inside this by the Ingestor.
 * Consumers should create an object of this class and have the shared cache 
 * set by terracotta itself. 
 * 
 */
public class AppDataCache implements AppStatsDataSource{

	/**
	 * the Cache holding Application Event Statistics
	 * This is the tc-root
	 */
	Cache<AppEventStatistics> cache = null;
	
	
	/**
	 * 
	 */
	public AppDataCache() {}

	/**
	 * 
	 * @return application statistics Cache
	 */
	public Cache<AppEventStatistics> getCache() {
		return cache;
	}

	/**
	 * Set the application statistics Cache 
	 * @param cache
	 */
	public void setCache(Cache<AppEventStatistics> cache) {
		this.cache = cache;
	}
	
	/**
	 * Returns the statistics for the given time window
	 * for the node identified with the given fqn
	 * 
	 * @param eventName Name of the event e.g. buyProductAction
	 * @param statName Name of the statistic e.g. minTime
	 * @param from from time
	 * @param to to time
	 * @param fqn identifier for the given node e.g. "mzstore","dc1"
	 * @return A List of Tuples of Dates and numbers
	 * @throws DataException
	 */
	public Statistics getStatistics(String eventName,
			String[] statNames, Date from, Date to, String... fqn)
			throws DataException {
		
		if(cache == null){
			throw new DataException("Cache not set");
		}
		
		try{
			return cache.getStatistics(eventName, statNames, from, to, fqn);
		}catch(CacheException e){
			throw new DataException(e);
		}
	}
	
}
