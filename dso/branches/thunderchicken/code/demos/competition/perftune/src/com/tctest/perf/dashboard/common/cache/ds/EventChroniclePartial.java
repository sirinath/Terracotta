/**
 * 
 */
package com.tctest.perf.dashboard.common.cache.ds;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
public class EventChroniclePartial<E extends EventStatistics> implements EventChronicle<E> {

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
	private final HashMap<Long, E> chronicle = new HashMap<Long, E>();
	

	/**
	 * This counter maintains the current size of the chronicle
	 */
	private int chronicleSize = 0;

	/**
	 * Lock
	 */
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	//=====================================================
	//sorting optimization
	
	private SortedMap<Long, E> lastSorted = null;
	private final int cacheSize = 18; //10% of max, TODO make property
	
	//=====================================================

	/**
	 * 
	 */
	public EventChroniclePartial(String name, int max) {
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
				removeFirst();
				chronicleSize--;
			}
	}

  private void removeFirst() {
    final SortedMap<Long, E> sortedMap = cachedMapTail();
    final Long firstKey = sortedMap.firstKey();
    chronicle.remove(firstKey);
    sortedMap.remove(firstKey);
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

			SortedMap<Long, E> tailMap = sortedMap().tailMap(fromDate.getTime());

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

			SortedMap<Long, E> subMap = sortedMap().subMap(fromDate.getTime(),
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
			long epochLong = cachedMapTail().firstKey();
			return new Date(epochLong);
		} catch (NoSuchElementException nsee) {
			return null;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	private SortedMap<Long, E> sortedMap(){
	  return new TreeMap<Long, E>(this.chronicle);
	}
	
	/* return cached tail of sorted map, create new one if necessary */
	private SortedMap<Long, E> cachedMapTail(){
      if (lastSorted == null || lastSorted.size() == 0){

        SortedMap<Long, E> newSort = sortedMap();
        this.lastSorted = new TreeMap<Long, E>();
        Iterator<Map.Entry<Long, E>> entrySet = newSort.entrySet().iterator();
        for(int i=0; i<cacheSize && entrySet.hasNext(); i++){
          Map.Entry<Long, E> entry = entrySet.next();
          this.lastSorted.put(entry.getKey(), entry.getValue());
        }
      } 
      
      return this.lastSorted;	  
	}
}
