/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.agent.listeners;

import com.tc.statistics.StatisticsManagerListener;

/**
 * Abstract class providing no-op implementations of StatisticsManager.
 * Convenience class for creating new listeners by overriding only required methods.
 */

public abstract class StatisticsManagerListenerAdapter implements StatisticsManagerListener {

  public void enabled() {}

  public void disabled() {}

  public void reinitialized() {}

  public void sessionCreated(String sessionId) {}

  public void allStatisticsDisabled(String sessionId) {}

  public void statisticEnabled(String sessionId, String statisticName) {}

  public void statisticCaptured(String sessionId, String name) {}

  public void capturingStarted(String sessionId) {}

  public void capturingStopped(String sessionId) {}

  public void globalParamSet(String key, Object value) {}

  public void sessionParamSet(String sessionId, String key, Object value) {}
}
