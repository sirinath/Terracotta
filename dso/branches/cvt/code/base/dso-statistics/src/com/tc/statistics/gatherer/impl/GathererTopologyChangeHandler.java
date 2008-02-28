/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.impl;

import com.tc.statistics.StatisticsManager;
import com.tc.statistics.beans.TopologyChangeHandler;

public class GathererTopologyChangeHandler implements TopologyChangeHandler {
  private volatile boolean enabled = false;
  private volatile String sessionId = null;
  private volatile String[] enabledStatistics = null;
  private volatile boolean capturingStarted = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String[] getEnabledStatistics() {
    return enabledStatistics;
  }

  public void setEnabledStatistics(String[] enabledStatistics) {
    this.enabledStatistics = enabledStatistics;
  }

  public boolean isCapturingStarted() {
    return capturingStarted;
  }

  public void setCapturingStarted(boolean capturingStarted) {
    this.capturingStarted = capturingStarted;
  }

  public void agentAdded(StatisticsManager agent) {
    if (enabled) {
      agent.enable();
    } else {
      agent.disable();
    }

    if (sessionId != null) {
      agent.createSession(sessionId);

      if (enabledStatistics != null) {
        agent.disableAllStatistics(sessionId);
        for (int i = 0; i < enabledStatistics.length; i++) {
          agent.enableStatistic(sessionId, enabledStatistics[i]);
        }
      }
      if (capturingStarted) {
        agent.startCapturing(sessionId);
      }
    }
  }
}
