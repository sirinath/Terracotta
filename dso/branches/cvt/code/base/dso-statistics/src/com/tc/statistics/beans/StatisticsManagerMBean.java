/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.management.TerracottaMBean;

public interface StatisticsManagerMBean extends TerracottaMBean {
  public String[] getSupportedStatistics();

  public long createCaptureSession();

  public void disableAllStatistics(long sessionId);

  public void enableStatistic(long sessionId, String name);

  public void startCapturing(long sessionId);

  public void stopCapturing(long sessionId);

  public void setGlobalParam(String key, Object value);

  public Object getGlobalParam(String key);

  public void setSessionParam(long sessionId, String key, Object value);

  public Object getSessionParam(long sessionId, String key);
}