/**
 * 
 */
package com.tctest.perf.dashboard.stats.app;

public class AppEventStatisticsWrapper {

	/**
	 * 
	 */
	private AppEventStatistics stats = null;

	/**
	 * 
	 */
	private String[] path = null;

	/**
	 * 
	 */
	public AppEventStatisticsWrapper() {
	}

	/**
	 * 
	 * @return stats
	 */
	public AppEventStatistics getStats() {
		return stats;
	}

	/**
	 * 
	 * @param stats
	 */
	public void setStats(AppEventStatistics stats) {
		this.stats = stats;
	}

	/**
	 * @return the path
	 */
	public String[] getPath() {
		return path;
	}

	/**
	 * @param path
	 *            the path to set
	 */
	public void setPath(String[] path) {
		this.path = path;
	}

}
