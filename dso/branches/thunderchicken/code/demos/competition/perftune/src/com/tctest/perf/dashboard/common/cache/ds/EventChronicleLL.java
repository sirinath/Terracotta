/**
 * 
 */
package com.tctest.perf.dashboard.common.cache.ds;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

import com.tctest.perf.dashboard.common.cache.EventStatistics;
import com.tctest.perf.dashboard.common.util.Tuple2;

/**
 * 
 * This hosts the recent 'count' statistics in past for the 'event'
 * 
 */
public class EventChronicleLL<E extends EventStatistics> implements EventChronicle<E> {

	/**
	 * 
	 */
	private final String name;

	/**
	 * The maximum number of (latest) events that can be cached non final ... we
	 * want this to be changeable
	 */
	private int max;

	/**
	 *    
	 */
	private LinkedList<Tuple2<Long, E>> chronicle = new LinkedList<Tuple2<Long, E>>();

	/**
	 * This counter maintains the current size of the chronicle
	 */
	private int chronicleSize = 0;

	/**
	 * Lock
	 */
	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * 
	 */
	public EventChronicleLL(String name, int max) {
		this.name = name;
		this.max = max;
	}

	/* (non-Javadoc)
	 * @see com.tctest.perf.dashboard.common.cache.ds.IEventChronicle#getMax()
	 */
	public int getMax() {
		return max;
	}

	/* (non-Javadoc)
	 * @see com.tctest.perf.dashboard.common.cache.ds.IEventChronicle#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.tctest.perf.dashboard.common.cache.ds.IEventChronicle#addStatistics(java.util.Date, E)
	 */
	public void addStatistics(Date date, E statistics) {
		// acquire the lock
		lock.lock();
		try {
			chronicle.add(new Tuple2(date.getTime(), statistics));
			chronicleSize++;
		} finally {
			// release the lock
			lock.unlock();
		}
		limitToMax();

	}

	private void limitToMax() {
		// acquire the lock
		lock.lock();
		try {
			while (chronicleSize >= max) {
				// Remove oldest entry
				chronicle.removeFirst();
				chronicleSize--;
			}
		} finally {
			// release the lock
			lock.unlock();
		}

	}

	/* (non-Javadoc)
	 * @see com.tctest.perf.dashboard.common.cache.ds.IEventChronicle#getStatistics()
	 */
	public List<Tuple2<Date, E>> getStatistics() {
		lock.lock();
		try {
			// create a copy of the elements first ... or else the reader and
			// writer may act on the treemap the same time
			// in which case the reader would see a
			// ConcurrentModificationException
			List<Tuple2<Date, E>> statistics = new ArrayList<Tuple2<Date, E>>();
			for (Tuple2<Long, E> tuple : chronicle) {
				statistics.add(new Tuple2<Date, E>(new Date(tuple.get_1()), tuple.get_2()));
			}
			return statistics;
		} finally {
			lock.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see com.tctest.perf.dashboard.common.cache.ds.IEventChronicle#getStatistics(java.util.Date)
	 */
	public List<Tuple2<Date, E>> getStatistics(Date fromDate) {
		lock.lock();
		try {
			// create a copy of the elements first ... or else the reader and
			// writer may act on the treemap the same time
			// in which case the reader would see a
			// ConcurrentModificationException

			// PERF: reads are O(N) - using a segmented tree would give us O(log N)
			List<Tuple2<Date, E>> statistics = new ArrayList<Tuple2<Date, E>>();
			Long from = fromDate.getTime();
			for (Tuple2<Long, E> tuple : chronicle) {
				if (tuple.get_1() >= from) {
					statistics.add(new Tuple2<Date, E>(new Date(tuple.get_1()), tuple.get_2()));
				}
			}
			return statistics;
		} finally {
			lock.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see com.tctest.perf.dashboard.common.cache.ds.IEventChronicle#getStatistics(java.util.Date, java.util.Date)
	 */
	public List<Tuple2<Date, E>> getStatistics(Date fromDate, Date toDate) {
		lock.lock();
		try {
			// create a copy of the elements first ... or else the reader and
			// writer may act on the treemap the same time
			// in which case the reader would see a
			// ConcurrentModificationException

			// PERF: reads are O(N) - using a segmented tree would give us O(log N)
			List<Tuple2<Date, E>> statistics = new ArrayList<Tuple2<Date, E>>();
			Long from = fromDate.getTime();
			Long to = toDate.getTime();
			for (Tuple2<Long, E> tuple : chronicle) {
				if (tuple.get_1() >= from) {
					if (tuple.get_1() >= to) {
						break;
					}
					statistics.add(new Tuple2<Date, E>(new Date(tuple.get_1()), tuple.get_2()));
				}
			}
			return statistics;
		} finally {
			lock.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see com.tctest.perf.dashboard.common.cache.ds.IEventChronicle#getStatisticsForTime(java.util.Date)
	 */
	public E getStatisticsForTime(Date date) {
		lock.lock();
		try {
			// PERF: reads are O(N) - using a segmented tree would give us O(log N)
			Long lDate = date.getTime();
			for (Tuple2<Long, E> tuple : chronicle) {
				if (tuple.get_1() >= lDate) {
					if (tuple.get_1() == lDate) {
						return tuple.get_2();
					} else {
						return null;
					}
				}
			}
			return null;
		} finally {
			lock.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see com.tctest.perf.dashboard.common.cache.ds.IEventChronicle#getEpoch()
	 */
	public Date getEpoch() {
		lock.lock();
		try {
			long epochLong = chronicle.getFirst().get_1();
			return new Date(epochLong);
		} catch (NoSuchElementException nsee) {
			return null;
		} finally {
			lock.unlock();
		}
	}
}
