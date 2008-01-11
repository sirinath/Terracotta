/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.StatisticRetrievalAction;
import com.tc.statistics.retrieval.actions.SRACpu;

import java.net.InetAddress;
import java.util.Date;

import junit.framework.TestCase;

public class SRACpuTest extends TestCase {
  public void testRetrieval() throws Exception {
    StatisticRetrievalAction action = new SRACpu();

    Date before1 = new Date();
    StatisticData[] data1 = action.retrieveStatisticData();
    Date after1 = new Date();

    assertCpuData(data1, before1, after1);
  }

  private long[] assertCpuData(StatisticData[] data, Date before, Date after) throws Exception {
    long[] values = new long[6];
    for (int i = 0; i < data.length; i++) {
      assertEquals("SRACpu", data[i].getName());
      assertEquals(InetAddress.getLocalHost().getHostAddress(), data[i].getAgentIp());
      assertTrue(before.compareTo(data[i].getMoment()) <= 0);
      assertTrue(after.compareTo(data[i].getMoment()) >= 0);
      switch (i) {
        case 0:
          assertEquals(SRACpu.ELEMENT_IDLE, data[i].getElement());
          values[0] = ((Long)data[i].getData()).longValue();
          break;
        case 1:
          assertEquals(SRACpu.ELEMENT_NICE, data[i].getElement());
          values[1] = ((Long)data[i].getData()).longValue();
          break;
        case 2:
          assertEquals(SRACpu.ELEMENT_SYS, data[i].getElement());
          values[2] = ((Long)data[i].getData()).longValue();
          break;
        case 3:
          assertEquals(SRACpu.ELEMENT_TOTAL, data[i].getElement());
          values[3] = ((Long)data[i].getData()).longValue();
          break;
        case 4:
          assertEquals(SRACpu.ELEMENT_USER, data[i].getElement());
          values[4] = ((Long)data[i].getData()).longValue();
          break;
        case 5:
          assertEquals(SRACpu.ELEMENT_WAIT, data[i].getElement());
          values[5] = ((Long)data[i].getData()).longValue();
          break;
        default:
          fail();
          break;
      }
    }

    System.out.println("CPU: "+values[0]+", "+values[1]+", "+values[2]+", "+values[3]+", "+values[4]+", "+values[5]);

    return values;
  }
}