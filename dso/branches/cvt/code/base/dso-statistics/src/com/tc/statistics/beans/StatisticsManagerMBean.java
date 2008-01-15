/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

public interface StatisticsManagerMBean {
  public String[] getSupportedStatistics();
  public long createCaptureSession();
  public void registerAction(long sessionId, String actionName);
  public void startCapturing(long sessionId);
  public void stopCapturing(long sessionId);
}