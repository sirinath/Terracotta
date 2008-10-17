/**
 * 
 */
package com.tctest.perf.dashboard.common.cache.ds;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.tctest.perf.dashboard.common.cache.EventStatistics;
import com.tctest.perf.dashboard.common.util.Tuple2;

/**
 * 
 * This hosts the recent 'count' statistics in past for the 'event'
 * 
 */
public class EventChronicleTM<E extends EventStatistics> implements EventChronicle<E> {

	/**
	 * 
	 */
	private final String name;

	/**
	 * The maximum number of (latest) events that can be cached non final ... we
	 * want this to be changeable
	 */
	private final int max;

	/**
	 *    
	 */
	private final TreeMap<Long, E> chronicle = new TreeMap<Long, E>();

	/**
	 * This counter maintains the current size of the chronicle
	 */
	private int chronicleSize = 0;

	/**
	 * Lock
	 */
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * 
	 */
	public EventChronicleTM(String name, int max) {
		this.name = name;
		this.max = max;
	}

	/**
	 * 
	 * @return max value
	 */
	public int getMax() {
		return max;
	}

	/**
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Add a new Statistics object to the tree This would typically be called
	 * every x mins (frequencey with wich the source publishes the statistics)
	 * e.g. 1 min for MZApps
	 * 
	 * @param date
	 * @param statistics
	 * 
	 */
	public void addStatistics(Date date, E statistics) {
		// acquire the lock
		lock.writeLock().lock();
		try {
			chronicle.put(date.getTime(), statistics);
			chronicleSize++;
			limitToMax();
		} finally {
			// release the lock
			lock.writeLock().unlock();
		}

	}

	private void limitToMax() {
			while (chronicleSize >= max) {
				// Remove oldest entry
				chronicle.remove(chronicle.firstKey());
				chronicleSize--;
			}
	}

	/**
	 * 
	 * Returns a read-only iterator over all statistics present in the Chronicle
	 * 
	 * @return UnModifiable Iterator
	 */
	public List<Tuple2<Date, E>> getStatistics() {
		lock.readLock().lock();
		try {
			// create a copy of the elements first ... or else the reader and
			// writer may act on the treemap the same time
			// in which case the reader would see a
			// ConcurrentModificationException
			List<Tuple2<Date, E>> statistics = new ArrayList<Tuple2<Date, E>>();
			for (Long key : chronicle.keySet()) {
				statistics.add(new Tuple2<Date, E>(new Date(key), chronicle
						.get(key)));
			}
			return statistics;
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Returns a read-only iterator over all statistics after the given date
	 * (fromDate Inclusive)
	 * 
	 * @param fromDate
	 * @return statistics - a List of tuples of date and stats objects
	 */
	public List<Tuple2<Date, E>> getStatistics(Date fromDate) {
		lock.readLock().lock();
		try {
			// create a copy of the elements first ... or else the reader and
			// writer may act on the treemap the same time
			// in which case the reader would see a
			// ConcurrentModificationException

			SortedMap<Long, E> tailMap = chronicle.tailMap(fromDate.getTime());

			List<Tuple2<Date, E>> statistics = new ArrayList<Tuple2<Date, E>>();
			for (Long key : tailMap.keySet()) {
				statistics.add(new Tuple2<Date, E>(new Date(key), chronicle
						.get(key)));
			}
			return statistics;
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Returns a read-only iterator over all statistics in between fromDate and
	 * toDate fromDate, inclusive, to toDate, exclusive
	 * 
	 * Get the statistics for after fromDate
	 * 
	 * @param fromDate
	 * @return statistics - a List of tuples of date and stats objects
	 */
	public List<Tuple2<Date, E>> getStatistics(Date fromDate, Date toDate) {
		lock.readLock().lock();
		try {
			// create a copy of the elements first ... or else the reader and
			// writer may act on the treemap the same time
			// in which case the reader would see a
			// ConcurrentModificationException

			SortedMap<Long, E> subMap = chronicle.subMap(fromDate.getTime(),
					toDate.getTime());

			List<Tuple2<Date, E>> statistics = new ArrayList<Tuple2<Date, E>>();
			for (Long key : subMap.keySet()) {
				statistics.add(new Tuple2<Date, E>(new Date(key), chronicle
						.get(key)));
			}
			return statistics;
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Returns the statistics object for the given time. This would be used to
	 * get a reference to the statistics object for a higher-level Node in which
	 * the statistic object is updatable.
	 * 
	 * @param date
	 * @return E
	 */
	public E getStatisticsForTime(Date date) {
		lock.readLock().lock();
		try {
			return chronicle.get(date);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 
	 * @return time for the oldest entry in this chronicle. returns null if
	 *         there is no entry in the chronicle yet.
	 */
	public Date getEpoch() {
		lock.readLock().lock();
		try {
			long epochLong = chronicle.firstKey();
			return new Date(epochLong);
		} catch (NoSuchElementException nsee) {
			return null;
		} finally {
			lock.readLock().unlock();
		}
	}
}
