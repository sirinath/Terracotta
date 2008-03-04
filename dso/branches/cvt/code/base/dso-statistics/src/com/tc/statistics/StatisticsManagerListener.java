/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

public interface StatisticsManagerListener {

  public void enabled();

  public void disabled();

  public void reinitialized();

  public void sessionCreated(String sessionId);

  public void allStatisticsDisabled(String sessionId);

  public void statisticEnabled(String sessionId, String statisticName);

  public void statisticCaptured(String sessionId, String name);

  public void capturingStarted(String sessionId);

  public void capturingStopped(String sessionId);

  public void globalParamSet(String key, Object value);

  public void sessionParamSet(String sessionId, String key, Object value);
}
