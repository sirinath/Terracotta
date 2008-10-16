package com.tctest.perf.dashboard.stats.app.test;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.tctest.perf.dashboard.common.cache.CacheException;
import com.tctest.perf.dashboard.stats.app.data.cache.AppDataCache;

public class Reader {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Reader reader = new Reader();
		reader.startReading();
	}

	AppDataCache appDataCache = TestCacheBuilder.buildCache();

	private double appProb = 0.80d;
	private double dcProb = 0.60d;
	private double podProb = 0.40d;
	private double hostProb = 0.50d;
	private double instanceProb = 0.50d;

	/**
	 * 
	 */
	private void startReading() {

		System.out.println("startReading()");
		int loopCount = 0;
		long statCount = 0;
		long grandtotalTime = 0;
		while (true) {
			loopCount++;
			long startTime = System.currentTimeMillis();
			// System.out.println(new Date(startTime));
			int thisLoopStatCount = 0;
			for (int eventId = 1; eventId <= 5; eventId++) {

				for (int appId = 1; appId <= 5; appId++) { // 5 applications

					String eventName = "Event_" + appId + "_" + eventId;
					if (printLogs)
						System.out.println("Getting Event : " + eventName);

					if (Math.random() < appProb) {
						getStats(eventName, "APP-" + appId);
						thisLoopStatCount++;
					}

					for (int dcId = 1; dcId <= 2; dcId++) { // 2 dcs each (total
						// 10)
						if (Math.random() < dcProb) {
							getStats(eventName, "APP-" + appId, "DC-" + dcId);
							thisLoopStatCount++;
						}
						for (int podId = 1; podId <= 6; podId++) { // 6
							// partitions
							// each
							// (total
							// 60)
							if (Math.random() < podProb) {
								getStats(eventName, "APP-" + appId, "DC-"
										+ dcId, "POD-" + podId);
								thisLoopStatCount++;
							}
							for (int hostId = 1; hostId <= 20; hostId++) { // 20
								// hosts
								// each
								// (
								// total
								// 1200
								// )
								if (Math.random() < hostProb) {
									getStats(eventName, "APP-" + appId, "DC-"
											+ dcId, "POD-" + podId, "HOST-"
											+ hostId);
									thisLoopStatCount++;
								}
								for (int instanceId = 1; instanceId <= 6; instanceId++) { // 6
									// instances
									// each
									// (
									// total
									// 7200
									// )
									if (Math.random() < instanceProb) {
										getStats(eventName, "APP-" + appId,
												"DC-" + dcId, "POD-" + podId,
												"HOST-" + hostId, "INSTANCE-"
														+ instanceId);
										thisLoopStatCount++;
									}
								}

							}
						}
					}
				}
			}
			statCount = statCount + thisLoopStatCount;

			long totalTime = System.currentTimeMillis() - startTime;
			grandtotalTime += totalTime;
			System.out
					.println(new Date()
							+ " : "
							+ loopCount
							+ " loops : "
							+ thisLoopStatCount
							+ " reads  : "
							+ totalTime
							+ "  ms : AvgTime "
							+ grandtotalTime
							/ loopCount
							+ " ms : FreeMemory  "
							+ (int) (100 * (double) Runtime.getRuntime()
									.freeMemory() / (double) Runtime
									.getRuntime().maxMemory()) + " %");
			try {
				Thread.sleep(sleepTimeInMillis);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}// sleep for a second
		}
	}

	/**
	 * 
	 * @param path
	 */
	private void getStats(String eventName, String... path) {

		int mins = (int) Math.random() * 20;
		String[] statNames = { "maxTime", "avgTime" };
		try {
			long t1 = System.currentTimeMillis();
			appDataCache.getCache().getStatistics(eventName, statNames,
					new Date(System.currentTimeMillis() - 1000 * 60 * mins),
					null, path);
			long t2 = System.currentTimeMillis();
			if (printLogs)
				System.out.println(" Got Stats for : " + eventName + " : "
						+ StringUtils.join(path, ".") + " IN " + (t2 - t1)
						+ " ms");
		} catch (CacheException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private boolean printLogs = false;
	private long sleepTimeInMillis = 0l;

	/**
	 * @return the appDataCache
	 */
	public AppDataCache getAppDataCache() {
		return appDataCache;
	}

	/**
	 * @param appDataCache
	 *            the appDataCache to set
	 */
	public void setAppDataCache(AppDataCache appDataCache) {
		this.appDataCache = appDataCache;
	}

	/**
	 * @return the printLogs
	 */
	public boolean isPrintLogs() {
		return printLogs;
	}

	/**
	 * @param printLogs
	 *            the printLogs to set
	 */
	public void setPrintLogs(boolean printLogs) {
		this.printLogs = printLogs;
	}

	/**
	 * @return the sleepTimeInMillis
	 */
	public long getSleepTimeInMillis() {
		return sleepTimeInMillis;
	}

	/**
	 * @param sleepTimeInMillis
	 *            the sleepTimeInMillis to set
	 */
	public void setSleepTimeInMillis(long sleepTimeInMillis) {
		this.sleepTimeInMillis = sleepTimeInMillis;
	}

	/**
	 * @return the appProb
	 */
	public double getAppProb() {
		return appProb;
	}

	/**
	 * @param appProb
	 *            the appProb to set
	 */
	public void setAppProb(double appProb) {
		this.appProb = appProb;
	}

	/**
	 * @return the dcProb
	 */
	public double getDcProb() {
		return dcProb;
	}

	/**
	 * @param dcProb
	 *            the dcProb to set
	 */
	public void setDcProb(double dcProb) {
		this.dcProb = dcProb;
	}

	/**
	 * @return the podProb
	 */
	public double getPodProb() {
		return podProb;
	}

	/**
	 * @param podProb
	 *            the podProb to set
	 */
	public void setPodProb(double podProb) {
		this.podProb = podProb;
	}

	/**
	 * @return the hostProb
	 */
	public double getHostProb() {
		return hostProb;
	}

	/**
	 * @param hostProb
	 *            the hostProb to set
	 */
	public void setHostProb(double hostProb) {
		this.hostProb = hostProb;
	}

	/**
	 * @return the instanceProb
	 */
	public double getInstanceProb() {
		return instanceProb;
	}

	/**
	 * @param instanceProb
	 *            the instanceProb to set
	 */
	public void setInstanceProb(double instanceProb) {
		this.instanceProb = instanceProb;
	}

}
