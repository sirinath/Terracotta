/**
 * 
 */
package com.tctest.perf.dashboard.stats.app.data;

import java.util.Date;

import com.tctest.perf.dashboard.common.data.DataException;
import com.tctest.perf.dashboard.common.data.Statistics;

/**
 * A common interface 
 * @author vipul
 *
 */
public interface AppStatsDataSource {
	
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
			throws DataException;


}
