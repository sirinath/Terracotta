/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;

public interface StatisticsStore {
  public void open() throws TCStatisticsStoreException;

  public void close() throws TCStatisticsStoreException;

  public void storeStatistic(StatisticData data) throws TCStatisticsStoreException;

  public void retrieveStatistics(StatisticsRetrievalCriteria criteria, StatisticsConsumer consumer) throws TCStatisticsStoreException;

  public String[] getAvailableSessionIds() throws TCStatisticsStoreException;

  public void clearStatistics(String sessionId) throws TCStatisticsStoreException;

  public void clearAllStatistics() throws TCStatisticsStoreException;

  public void addListener(StatisticsStoreListener listener);

  public void removeListener(StatisticsStoreListener listener);
}