/**
 * 
 */
package com.tctest.perf.dashboard.stats.app.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.tctest.perf.dashboard.common.cache.CacheException;
import com.tctest.perf.dashboard.common.util.Tuple2;
import com.tctest.perf.dashboard.stats.app.AppEventStatistics;
import com.tctest.perf.dashboard.stats.app.data.cache.AppDataCache;

public class Ingestor {

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		Ingestor ingestor = new Ingestor();
		ingestor.startLoading();
	}

	AppDataCache appDataCache = TestCacheBuilder.buildCache();

	int queueSize = 50;
	int threadCount = 3;
	int sleepTimeInMillis = 10;

	int startAppId = 1;
	int endAppId = 5;
	boolean printLogs = false;

	// Long[] keys = null;
	int currentIndex = 0;

	final BlockingQueue<Tuple2<AppEventStatistics, String[]>> q = new LinkedBlockingQueue<Tuple2<AppEventStatistics, String[]>>(
			queueSize);

	private void startLoading() throws CacheException {

		// map = new HashMap<Long, BlockingQueue<Tuple2<AppEventStatistics,
		// String[]>>>();

		for (int i = 0; i < threadCount; i++) {

			Thread t = new Thread(new Runnable() {
				public void run() {
					while (true) {
						try {
							Tuple2<AppEventStatistics, String[]> statPath = q
									.take();
							appDataCache.getCache().addStatistics(statPath._1,
									statPath._2);

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}

			});
			t.setName("ingestor-" + (i + 1));

			// map.put(t.getId(), q);

			t.start();
			System.out.println("launched thread " + t.getName());

		}

		// keys = map.keySet().toArray(new Long[0]);

		System.out.println("startLoading()");
		System.out.println("startAppId " + startAppId);
		System.out.println("endAppId " + endAppId);
		int loopCount = 0;
		long statCount = 0;
		long grandTotal = 0;
		while (true) {
			loopCount++;
			long startTime = System.currentTimeMillis();
			// System.out.println(new Date(startTime));
			for (int eventId = 1; eventId <= 5; eventId++) {

				for (int appId = startAppId; appId <= endAppId; appId++) { // 5
					// applications
					String eventName = "Event_" + appId + "_" + eventId;
					if (printLogs)
						System.out.println("AddingEvent : " + eventName);

					AppEventStatistics stat = createStatisticsObject(eventName);
					addStat(stat, "APP-" + appId);
					statCount++;
					// System.out.println("APP-"+appId);

					for (int dcId = 1; dcId <= 2; dcId++) { // 2 dcs each (total
						// 10)
						stat = createStatisticsObject(eventName);
						addStat(stat, "APP-" + appId, "DC-" + dcId);
						statCount++;
						// System.out.println("APP-"+appId+"DC-"+dcId);
						for (int podId = 1; podId <= 6; podId++) { // 6
							// partitions
							// each
							// (total
							// 60)
							stat = createStatisticsObject(eventName);
							addStat(stat, "APP-" + appId, "DC-" + dcId, "POD-"
									+ podId);
							statCount++;
							// System.out.println(
							// "APP-"+appId+"DC-"+dcId+"POD-"+podId);
							for (int hostId = 1; hostId <= 20; hostId++) { // 20
								// hosts
								// each
								// (
								// total
								// 1200
								// )
								stat = createStatisticsObject(eventName);
								addStat(stat, "APP-" + appId, "DC-" + dcId,
										"POD-" + podId, "HOST-" + hostId);
								statCount++;
								// System.out.println(
								// "APP-"+appId+"DC-"+dcId+"POD-"
								// +podId+"HOST-"+hostId);
								for (int instanceId = 1; instanceId <= 6; instanceId++) { // 6
									// instances
									// each
									// (
									// total
									// 7200
									// )
									stat = createStatisticsObject(eventName);
									addStat(stat, "APP-" + appId, "DC-" + dcId,
											"POD-" + podId, "HOST-" + hostId,
											"INSTANCE-" + instanceId);
									statCount++;
									// System.out.println("APP-"+appId+"DC-"+dcId
									// +"POD-"+podId+"HOST-"+hostId+"INSTANCE-"+
									// instanceId);
								}
							}
						}
					}
				}
			}

			// BlockingQueue<Tuple2<AppEventStatistics, String[]>> q = null;
			// for (Long tid : map.keySet()) {
			// q = map.get(tid);
			// ;
			long time = System.currentTimeMillis();

			while (q.size() > 0) {
			}
			System.out.println(Thread.currentThread().getName()
					+ ": waited for " + (System.currentTimeMillis() - time)
					+ "ms for the Q to become empty");
			// }

			long totalTime = System.currentTimeMillis() - startTime;
			grandTotal += totalTime;
			System.out
					.println(new Date()
							+ " : "
							+ loopCount
							+ " loops, "
							+ statCount
							+ " stat objects, time taken : "
							+ totalTime
							+ " ms, avg time : "
							+ (grandTotal / loopCount)
							+ " ms  FreeMemory : "
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
	 * @param appId
	 * @param stat
	 * @throws CacheException
	 */
	private void addStat(AppEventStatistics stat, String... path)
			throws CacheException {

		// appDataCache.getCache().addStatistics(stat, path);

		Tuple2<AppEventStatistics, String[]> statPath = new Tuple2<AppEventStatistics, String[]>(
				stat, path);
		try {

			/*
			 * BlockingQueue<Tuple2<AppEventStatistics, String[]>> q = null;
			 * 
			 * long key = keys[currentIndex]; currentIndex++; currentIndex =
			 * currentIndex % keys.length; q = map.get(key);
			 */

			q.put(statPath);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @param eventName
	 * @return
	 */
	private AppEventStatistics createStatisticsObject(String eventName) {
		AppEventStatistics stat = new AppEventStatistics(eventName, new Date());
		stat.setAvgTimeInSeconds((float) Math.random() * 10);
		stat.setCount((int) Math.random() * 10);
		stat.setMaxTimeInSeconds((float) Math.random() * 10);
		stat.setMinTimeInSeconds((float) Math.random() * 10);
		return stat;
	}

	/**
	 * @return the startAppId
	 */
	public int getStartAppId() {
		return startAppId;
	}

	/**
	 * @param startAppId
	 *            the startAppId to set
	 */
	public void setStartAppId(int startAppId) {
		this.startAppId = startAppId;
	}

	/**
	 * @return the endAppId
	 */
	public int getEndAppId() {
		return endAppId;
	}

	/**
	 * @param endAppId
	 *            the endAppId to set
	 */
	public void setEndAppId(int endAppId) {
		this.endAppId = endAppId;
	}

	/**
	 * @return the sleepTime
	 */
	public int getSleepTimeInMillis() {
		return sleepTimeInMillis;
	}

	/**
	 * @param sleepTime
	 *            the sleepTime to set
	 */
	public void setSleepTimeInMillis(int sleepTimeInMillis) {
		this.sleepTimeInMillis = sleepTimeInMillis;
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
	 * @return the queueSize
	 */
	public int getQueueSize() {
		return queueSize;
	}

	/**
	 * @param queueSize
	 *            the queueSize to set
	 */
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	/**
	 * @return the threadCount
	 */
	public int getThreadCount() {
		return threadCount;
	}

	/**
	 * @param threadCount
	 *            the threadCount to set
	 */
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	/**
	 * @return the cache
	 */
	public AppDataCache getAppDataCache() {
		return appDataCache;
	}

	/**
	 * @param cache
	 *            the cache to set
	 */
	public void setAppDataCache(AppDataCache appDataCache) {
		this.appDataCache = appDataCache;
	}

}
