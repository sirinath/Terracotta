package com.tctest.perf.dashboard.common.cache.ds;

import java.util.Date;
import java.util.List;

import com.tctest.perf.dashboard.common.cache.EventStatistics;
import com.tctest.perf.dashboard.common.util.Tuple2;

public interface EventChronicle<E extends EventStatistics> {

	/**
	 * 
	 * @return max value
	 */
	int getMax();

	/**
	 * 
	 * @param max
	 */
	void setMax(int max);

	/**
	 * 
	 * @return name
	 */
	String getName();

	/**
	 * Add a new Statistics object to the tree This would typically be called
	 * every x mins (frequencey with wich the source publishes the statistics)
	 * e.g. 1 min for MZApps
	 * 
	 * @param date
	 * @param statistics
	 * 
	 */
	void addStatistics(Date date, E statistics);

	/**
	 * 
	 * Returns a read-only iterator over all statistics present in the Chronicle
	 * 
	 * @return UnModifiable Iterator
	 */
	List<Tuple2<Date, E>> getStatistics();

	/**
	 * Returns a read-only iterator over all statistics after the given date
	 * (fromDate Inclusive)
	 * 
	 * @param fromDate
	 * @return statistics - a List of tuples of date and stats objects
	 */
	List<Tuple2<Date, E>> getStatistics(Date fromDate);

	/**
	 * Returns a read-only iterator over all statistics in between fromDate and
	 * toDate fromDate, inclusive, to toDate, exclusive
	 * 
	 * Get the statistics for after fromDate
	 * 
	 * @param fromDate
	 * @return statistics - a List of tuples of date and stats objects
	 */
	List<Tuple2<Date, E>> getStatistics(Date fromDate, Date toDate);

	/**
	 * Returns the statistics object for the given time. This would be used to
	 * get a reference to the statistics object for a higher-level Node in which
	 * the statistic object is updatable.
	 * 
	 * @param date
	 * @return E
	 */
	E getStatisticsForTime(Date date);

	/**
	 * 
	 * @return time for the oldest entry in this chronicle. returns null if
	 *         there is no entry in the chronicle yet.
	 */
	Date getEpoch();

}