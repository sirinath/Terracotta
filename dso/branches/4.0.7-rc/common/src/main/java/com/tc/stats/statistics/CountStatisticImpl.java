/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.statistics;

public class CountStatisticImpl extends StatisticImpl implements CountStatistic {
  private long m_count;

  public CountStatisticImpl() {
    this(0L);
  }

  public CountStatisticImpl(long count) {
    m_count = count;
  }

  public void setCount(long count) {
    m_count = count;
  }

  @Override
  public long getCount() {
    return m_count;
  }
}
