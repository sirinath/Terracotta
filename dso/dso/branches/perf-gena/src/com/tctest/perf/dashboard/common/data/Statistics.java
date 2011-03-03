/**
 * 
 */
package com.tctest.perf.dashboard.common.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tctest.perf.dashboard.common.util.Tuple2;

/**
 * This class represents the bundled statistics that are returned back from the data provider 
 * to the caller. 
 * 
 */
public class Statistics {
	
	/**
	 * 
	 */
	private final String eventName;
	/**
	 * 
	 */
	private final String[] statNames;
	
	/**
	 * 
	 */
	private final Map<String,List<Tuple2<Date,Number>>> data;
	
	/**
	 * Sorted data
	 */
	private final Map<String,Map<String, Tuple2<Date, Number>>> sortedData;
	
	private static final String MAX = "max";
	private static final String AVG = "avg";
	private static final String CUR = "cur";
	
	/**
	 * 
	 * @param eventName
	 */
	public Statistics(String eventName,String[] statNames,Map<String,List<Tuple2<Date,Number>>> data){
		this.eventName = eventName;
		this.statNames = statNames;
		this.data = data;
		sortedData = new HashMap<String,Map<String, Tuple2<Date, Number>>>();
	}
	

	/**
	 * @return the eventName
	 */
	public String getEventName() {
		return eventName;
	}


	/**
	 * @return the statNames
	 */
	public String[] getStatNames() {
		return statNames;
	}

	/**
	 * 
	 * @param statName
	 * @return
	 */
	public List<Tuple2<Date,Number>> getStats(String statName){
		return data.get(statName);
	}
	
	private void calculateStatistics(){
		String[] statsNames = getStatNames();
		for (String statsName : statsNames) {
			sortedData.put(statsName, createStatisticsMap(getStats(statsName)));
		}
	}
	/**
	 * Creates the map which contains the Max, Avg, Curr.
	 */
	private Map<String,Tuple2<Date,Number>> createStatisticsMap(List<Tuple2<Date,Number>> list) {
		Map<String,Tuple2<Date,Number>> map = new HashMap<String,Tuple2<Date,Number>>();
		// Find the Max
		Collections.sort(list, new TupleValueComparator());
		map.put(MAX, list.get(0));
		//TODO
		// Find the avg and put
		
		map.put(AVG, list.get(0));
		// Find the cur 
		map.put(CUR, list.get(0));
		
		return map;
	}
	/**
	 * Get the maximum value within the range
	 */
	public Tuple2<Date,Number> getMax(String statName){
		if (sortedData.isEmpty() ) {
			calculateStatistics();
		}
		if (! sortedData.containsKey(statName)) {
			return sortedData.get(statName).get(MAX);
			
		} else {
			return null;
		}
	}
	
	/**
	 * Get the min value within the range
	 */
	public Tuple2<Date,Number> getAvg(String statName){
		if (sortedData.isEmpty() ) {
			calculateStatistics();
		}
		if (! sortedData.containsKey(statName)) {
			return sortedData.get(statName).get(AVG);
			
		} else {
			return null;
		}
	}
	
	class TupleValueComparator implements Comparator<Tuple2<Date, Number>>  {

		public int compare(Tuple2<Date, Number> t1, Tuple2<Date, Number> t2) {
			int result = 0;
			if (t2.get_2().doubleValue() > t1.get_2().doubleValue()) {
				result = -1;
			} else if (t2.get_2().doubleValue() > t1.get_2().doubleValue()) {
				result = 1;
			} else {
				result = 0;
			}
			return result;
			
		}
		
	}
	
}