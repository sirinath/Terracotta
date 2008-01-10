/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class SRASystemProperties implements StatisticRetrievalAction {
  public StatisticType getType() {
    return StatisticType.STARTUP;
  }

  public StatisticData retrieveStatisticData() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Properties properties = System.getProperties();
    try {
      properties.store(out, null);
      System.out.println(out.toString("ISO-8859-1"));
      return StatisticData.buildInstanceForClassAtLocalhost(getClass(), new Date(), out.toString("ISO-8859-1"));
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }
  }
}