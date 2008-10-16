/**
 * 
 */
package com.tctest.perf.dashboard.common.cache;

import java.util.Date;
import java.util.Set;

/**
 * 
 * The interface that declares the functionality of a statistics bean.
 * Every component capturing statistics from different systems <b>must</b>
 * implement this bean for the  to be able to maintain it.
 * 
 */
public interface EventStatistics {

	/**
	 * Get the name of the event that is captured by this object
	 * 
	 * @return name
	 */
	public String getEventName();
	/**
	 * Get the name of the event that is captured by this object
	 * 
	 * @return name
	 */
	public Set<String> getStatNames();
	/**
	 * Get the time when the statistics for this event were captured
	 * 
	 * @return date
	 */
	public Date getCaptureTime();
	
	/**
	 * Get the value for the given statistic
	 * Return null if the name is not recognized
	 * 
	 * The reason for using the generic get method instead of get methods for each 
	 * value in the custom statistics bean is that the Cache may not want 
	 * to return back references to the event beans it holds. 
	 * In a normal scenario a caller may expect an instance of EventStatistics' subclass
	 * and then derive numbers out of it directly calling the get methods. But in that case
	 * the bean gets leaked outside the Cache and hence there is no guarantee that it would 
	 * be freed up gracefully for garbage collection. collection of an object of this type
	 * is important as it's going to be a clusterwide object in terracotta and unnecessary 
	 * reference may bring down the whole system 
	 * 
	 * @param statName
	 * @return Number
	 */
	public Number getValue(String statName);
	
	
}
