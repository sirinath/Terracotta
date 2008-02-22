/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class SRACpu implements StatisticRetrievalAction {
  
  public final static String ACTION_NAME = "cpu";

  public final static String DATA_NAME_COMBINED = ACTION_NAME + " combined";
  public final static String DATA_NAME_IDLE = ACTION_NAME + " idle";
  public final static String DATA_NAME_NICE = ACTION_NAME + " nice";
  public final static String DATA_NAME_SYS = ACTION_NAME + " sys";
  public final static String DATA_NAME_USER = ACTION_NAME + " user";
  public final static String DATA_NAME_WAIT = ACTION_NAME + " wait";

  private final static String ELEMENT_PREFIX = "cpu ";

  private Sigar sigar;

  public SRACpu() {
    sigar = new Sigar();
  }

  public StatisticData[] retrieveStatisticData() {
    Date moment = new Date();
    try {
      NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
      format.setGroupingUsed(false);
      format.setMaximumFractionDigits(3);

      CpuPerc[] cpuPercList = sigar.getCpuPercList();
      StatisticData[] data = new StatisticData[cpuPercList.length * 6];
      for (int i = 0; i < cpuPercList.length; i++) {
        String element = ELEMENT_PREFIX + i;
        data[i * 6] = new StatisticData(DATA_NAME_COMBINED, moment, element, new BigDecimal(format.format(cpuPercList[i].getCombined())));
        data[i * 6 + 1] = new StatisticData(DATA_NAME_IDLE, moment, element, new BigDecimal(format.format(cpuPercList[i].getIdle())));
        data[i * 6 + 2] = new StatisticData(DATA_NAME_NICE, moment, element, new BigDecimal(format.format(cpuPercList[i].getNice())));
        data[i * 6 + 3] = new StatisticData(DATA_NAME_SYS, moment, element, new BigDecimal(format.format(cpuPercList[i].getSys())));
        data[i * 6 + 4] = new StatisticData(DATA_NAME_USER, moment, element, new BigDecimal(format.format(cpuPercList[i].getUser())));
        data[i * 6 + 5] = new StatisticData(DATA_NAME_WAIT, moment, element, new BigDecimal(format.format(cpuPercList[i].getWait())));
      }
      return data;
    } catch (SigarException e) {
      throw new TCRuntimeException(e);
    }
  }

  public String getName() {
    return ACTION_NAME;
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
