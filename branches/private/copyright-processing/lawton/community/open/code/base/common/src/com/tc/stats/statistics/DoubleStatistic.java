/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.statistics;

import javax.management.j2ee.statistics.Statistic;

public interface DoubleStatistic extends Statistic {

  double getDoubleValue();

}
