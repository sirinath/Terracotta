/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer;

import com.tc.statistics.StatisticData;
import com.tc.statistics.CaptureSession;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;

public interface StatisticsBuffer {
  public void open() throws TCStatisticsBufferException;

  public void close() throws TCStatisticsBufferException;

  public CaptureSession createCaptureSession() throws TCStatisticsBufferException;

  public void startCapturing(long sessionId) throws TCStatisticsBufferException;

  public void stopCapturing(long sessionId) throws TCStatisticsBufferException;

  public long storeStatistic(long sessionId, StatisticData data) throws TCStatisticsBufferException;

  public void consumeStatistics(long sessionId, StatisticsConsumer consumer) throws TCStatisticsBufferException;

  public void addListener(StatisticsBufferListener listener);

  public void removeListener(StatisticsBufferListener listener);
}