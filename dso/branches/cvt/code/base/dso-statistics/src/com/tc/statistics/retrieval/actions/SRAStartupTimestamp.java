/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticType;

import java.util.Date;

public class SRAStartupTimestamp implements StatisticRetrievalAction {
  public StatisticType getType() {
    return null;
  }

  public StatisticData[] retrieveStatisticData() {
    Date moment = new Date();
    return new StatisticData[] { StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, moment) };
  }
}