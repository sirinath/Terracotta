/**
 * 
 */
package com.tctest.perf.dashboard.stats.app;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.tctest.perf.dashboard.common.cache.EventStatistics;

/**
 * 
 * 
 * EventStatistics for the Applications
 * An instance of this class would represent the statistics for a given event (e.g. buyProductAction)
 * for a given time duration (time in between the captureTime of the last event and captureTime of this event)
 * The statistics it contains are 
 * - MinTime : Minimum time take to execute this call (in seconds)  
 * - MaxTime : Maximum time take to execute this call (in seconds)  
 * - AvgTime : Average time take to execute this call (in seconds)  
 * - Count : Number of calls made of this kind  
 * 
 */
public class AppEventStatistics implements EventStatistics {
	
	/**
	 * Time when the statistics were captured
	 */
	private Date captureTime;
	/**
	 * Event for which this statistics belong
	 */
	private String eventName;
	/**
	 * Count of this kind of events since the last time this data was collection
	 */
	private int count;
	/**
	 * Minimum time taken (in seconds) for this event since the last collection
	 */
	private float minTimeInSeconds;
	/**
	 * Average time taken (in seconds) for this event since the last collection
	 */
	private float avgTimeInSeconds;
	/**
	 * Maximum time taken (in seconds) for this event since the last collection
	 */
	private float maxTimeInSeconds;
	
	
	/**
	 * 
	 */
	public AppEventStatistics(String eventName,Date captureTime) {
		this.eventName = eventName;
		this.captureTime = captureTime;
	}

	/**
	 * returns the time this matrix was collected
	 * @return capture Time
	 */
	public Date getCaptureTime() {
		return captureTime;
	}

	/**
	 * returns the event name 
	 * @return event Name
	 */
	public String getEventName() {
		return eventName;
	}
	

	/**
	 * Gets the value for the given statistic
	 * valid names are 
	 * <p> - count
	 * <p> - MinTime
	 * <p> - MaxTime
	 * <p> - AvgTime
	 * 
	 * @param count 
	 * @return value
	 */
	public Number getValue(String statName) {
		
		if(statName == null) return null;
		
		if(statName.equalsIgnoreCase(Stats.COUNT.getName()))
			return count;
		if(statName.equalsIgnoreCase(Stats.MIN_TIME.getName()))
			return minTimeInSeconds;
		if(statName.equalsIgnoreCase(Stats.MAX_TIME.getName()))
			return maxTimeInSeconds;
		if(statName.equalsIgnoreCase(Stats.AVG_TIME.getName()))
			return avgTimeInSeconds;
		return null;
	}

	/**
	 * Returns the count of the number of times this call was made since the last collection
	 * @return count
	 */
	public int getCount() {
		return count;
	}
	/**
	 * 
	 * @param count
	 */
	public void setCount(int count) {
		this.count = count;
	}
	/**
	 * 
	 * @return min Time In Seconds
	 */
	public float getMinTimeInSeconds() {
		return minTimeInSeconds;
	}
	/**
	 * 
	 * @param minTimeInSeconds
	 */
	public void setMinTimeInSeconds(float minTimeInSeconds) {
		this.minTimeInSeconds = minTimeInSeconds;
	}
	/**
	 * returns the average time taken (in seconds) for the current time duration  
	 * 
	 * @return avg Time In Seconds
	 */
	public float getAvgTimeInSeconds() {
		return avgTimeInSeconds;
	}
	/**
	 * 
	 * @param avgTimeInSeconds
	 */
	public void setAvgTimeInSeconds(float avgTimeInSeconds) {
		this.avgTimeInSeconds = avgTimeInSeconds;
	}
	/**
	 * returns the maximum time taken (in seconds) for the current time duration  
	 * @return maxTime In Seconds
	 */
	public float getMaxTimeInSeconds() {
		return maxTimeInSeconds;
	}
	/**
	 * 
	 * @param maxTimeInSeconds
	 */
	public void setMaxTimeInSeconds(float maxTimeInSeconds) {
		this.maxTimeInSeconds = maxTimeInSeconds;
	}
	/**
	 * Returns the date/time when this statistic was captured
	 * @param captureTime
	 */
	public void setCaptureTime(Date captureTime) {
		this.captureTime = captureTime;
	}
	
	/**
	 * 
	 * @return String representation
	 */
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(eventName).append(" ").
			append(captureTime.toString()).
			append(" count-").append(count).
			append(" maxTime-").append(maxTimeInSeconds).
			append(" minTime-").append(minTimeInSeconds).
			append(" avgTime-").append(avgTimeInSeconds);
		
		return sb.toString();
	}
	
	public static enum Stats{
		COUNT("count"),
		MIN_TIME("minTime"),
		MAX_TIME("maxTime"),
		AVG_TIME("avgTime");
		
		private final String name;
		private Stats(String name){
			this.name = name;
		}
		public String getName(){return name;}
	}
	
	private static Set<String> eventNames = new HashSet<String>();
	
	static {
		for(Stats stats : Stats.values()){
			eventNames.add(stats.getName());
		}
	}
	
	/**
	 * Gives back the name of the statistics captured for this event
	 * 
	 * @return set of event names
	 */
	public Set<String> getStatNames(){
		return eventNames;
	}
}
