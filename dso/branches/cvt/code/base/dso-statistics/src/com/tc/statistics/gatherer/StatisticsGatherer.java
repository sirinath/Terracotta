/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer;

import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererException;

public interface StatisticsGatherer {
  public void initialize() throws TCStatisticsGathererException;

  public void connectManager(String managerHostName, int managerPort) throws TCStatisticsGathererException;

  public void disconnectManager() throws TCStatisticsGathererException;

  public String[] getSupportedStatistics() throws TCStatisticsGathererException;

  public void enableStatistics(String[] names) throws TCStatisticsGathererException;

  public void startCapturing() throws TCStatisticsGathererException;

  public void stopCapturing() throws TCStatisticsGathererException;
}
