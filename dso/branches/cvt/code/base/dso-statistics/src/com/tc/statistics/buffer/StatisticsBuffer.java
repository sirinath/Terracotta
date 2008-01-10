/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer;

import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;

import java.util.Date;

public interface StatisticsBuffer {
  public void open() throws TCStatisticsBufferException;
  public void close() throws TCStatisticsBufferException;
  public long createCaptureSession(Date start) throws TCStatisticsBufferException;
  public long storeStatistic(long sessionId, StatisticData data) throws TCStatisticsBufferException;
  public void consumeStatistics(long sessionId, StatisticsConsumer consumer) throws TCStatisticsBufferException;
}