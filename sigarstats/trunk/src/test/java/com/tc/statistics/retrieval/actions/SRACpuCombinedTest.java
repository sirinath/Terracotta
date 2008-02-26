/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import org.hyperic.sigar.Sigar;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.SigarUtil;

import java.math.BigDecimal;
import java.util.Date;

import junit.framework.TestCase;

public class SRACpuCombinedTest extends TestCase {
  static {
    SigarUtil.sigarInit();
  }
  private Sigar sigar;

  public void setUp() {
    sigar = new Sigar();
  }

  public void tearDown() {
    sigar.close();
  }

  public void testRetrieval() throws Exception {
    int cpuCount = sigar.getCpuInfoList().length;

    StatisticRetrievalAction action = new SRACpuCombined();

    Date before1 = new Date();
    StatisticData[] data1 = action.retrieveStatisticData();
    Date after1 = new Date();

    BigDecimal[] values1 = assertCpuData(cpuCount, data1, before1, after1);

    // creating more threads than CPUs, this should have at least one of these threads running on each CPU
    int threadCount = cpuCount*2;
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] = new UseCpuThread();
      threads[i].start();
    }

    Thread.sleep(2000);

    Date before2 = new Date();
    StatisticData[] data2 = action.retrieveStatisticData();
    Date after2 = new Date();

    BigDecimal[] values2 = assertCpuData(cpuCount, data2, before2, after2);

    // stop the threads and wait for them to finish
    for (int i = 0; i < threadCount; i++) {
      threads[i].interrupt();
    }

    for (int i = 0; i < threadCount; i++) {
      threads[i].join();
    }

    // assert that the cpu usage was higher during the second data collection
    for (int i = 0; i < cpuCount; i++) {
      assertTrue(values1[i].compareTo(values2[i]) < 0);
    }

    // assert that the cpu usage was almost the maximum during the second data collection
    for (int i = 0; i < cpuCount; i++) {
      assertTrue(values2[i].compareTo(new BigDecimal("0.95")) > 0);
    }
  }

  private class UseCpuThread extends Thread {
    public void run() {
      for (int i = 0; i < Integer.MAX_VALUE; i++) {
        if (0 == i % 1000) {
          if (isInterrupted()) {
            return;
          }
          Thread.yield();
        }
      }
    }
  }

  private BigDecimal[] assertCpuData(int cpuCount, StatisticData[] data, Date before, Date after) throws Exception {
    BigDecimal[] values = new BigDecimal[cpuCount];
    assertEquals(cpuCount, data.length);
    for (int i = 0; i < data.length; i++) {
      assertTrue(data[i].getName().equals(SRACpuCombined.ACTION_NAME));
      assertNull(data[i].getAgentIp()); // will be filled in with default
      assertTrue(before.compareTo(data[i].getMoment()) <= 0);
      assertTrue(after.compareTo(data[i].getMoment()) >= 0);

      assertEquals("cpu " + i, data[i].getElement());
      values[i] = (BigDecimal)data[i].getData();
    }

    return values;
  }
}