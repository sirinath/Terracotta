/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.impl.StatisticsGathererImpl;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;
import com.tc.util.UUID;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatisticsGathererTest extends TransparentTestBase {
  protected void duringRunningCluster() throws Exception {
    File tmp_dir = makeTmpDir(getClass());
    
    StatisticsStore store = new H2StatisticsStoreImpl(tmp_dir);
    StatisticsGatherer gatherer = new StatisticsGathererImpl(store);

    gatherer.connect("localhost", getAdminPort());

    String[] statistics = gatherer.getSupportedStatistics();

    String sessionid = UUID.getUUID().toString();
    gatherer.createSession(sessionid);

    gatherer.enableStatistics(statistics);

    gatherer.startCapturing();
    Thread.sleep(10000);
    gatherer.stopCapturing();

    Thread.sleep(5000);

    final List data_list = new ArrayList();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticsConsumer() {
      public boolean consumeStatisticData(StatisticData data) {
        data_list.add(data);
        return true;
      }
    });

    gatherer.disconnect();

    // check the data
    assertTrue(data_list.size() > 2);
    assertEquals(SRAStartupTimestamp.ACTION_NAME, ((StatisticData)data_list.get(0)).getName());
    assertEquals(SRAShutdownTimestamp.ACTION_NAME, ((StatisticData)data_list.get(data_list.size() - 1)).getName());
    Set received_data_names = new HashSet();
    for (int i = 1; i < data_list.size() - 1; i++) {
      StatisticData stat_data = (StatisticData)data_list.get(i);
      received_data_names.add(stat_data.getName());
    }

    // check that there's at least one data element name per registered statistic
    assertTrue(received_data_names.size() > statistics.length);
  }

  protected Class getApplicationClass() {
    return StatisticsGathererTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsGathererConfigSampleRateTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}