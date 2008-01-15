/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval;

import com.tc.statistics.StatisticRetrievalAction;

public interface StatisticsRetriever {
  public void startup();

  public void shutdown();

  public long getSessionId();

  public void removeAllActions();

  public void registerAction(StatisticRetrievalAction action);
}