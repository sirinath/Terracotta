/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticType;
import com.tc.exception.TCRuntimeException;

import java.util.Date;

public class SRACpu implements StatisticRetrievalAction {
  public final static String ELEMENT_IDLE = "idle";
  public final static String ELEMENT_NICE = "nice";
  public final static String ELEMENT_SYS = "sys";
  public final static String ELEMENT_TOTAL = "total";
  public final static String ELEMENT_USER = "user";
  public final static String ELEMENT_WAIT = "wait";

  private Sigar sigar;

  public SRACpu() {
    sigar = new Sigar();
  }

  public StatisticData[] retrieveStatisticData() {
    Date moment = new Date();
    try {
      Cpu cpu = sigar.getCpu();
      return new StatisticData[] {
          StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, ELEMENT_IDLE, new Long(cpu.getIdle())),
          StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, ELEMENT_NICE, new Long(cpu.getNice())),
          StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, ELEMENT_SYS, new Long(cpu.getSys())),
          StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, ELEMENT_TOTAL, new Long(cpu.getTotal())),
          StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, ELEMENT_USER, new Long(cpu.getUser())),
          StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, ELEMENT_WAIT, new Long(cpu.getWait()))
        };
    } catch (SigarException e) {
      throw new TCRuntimeException(e);
    }
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public void cleanup() {
    if (sigar != null) {
      sigar.close();
      sigar = null;
    }
  }
}
