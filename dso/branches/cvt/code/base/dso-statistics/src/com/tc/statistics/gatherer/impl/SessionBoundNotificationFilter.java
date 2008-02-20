/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.impl;

import com.tc.statistics.StatisticData;
import com.tc.util.Assert;

import java.io.Serializable;

import javax.management.Notification;
import javax.management.NotificationFilter;

class SessionBoundNotificationFilter implements NotificationFilter, Serializable {
  private final String sessionId;

  public SessionBoundNotificationFilter(final String sessionId) {
    Assert.assertNotNull("sessionId", sessionId);
    this.sessionId = sessionId;
  }

  public boolean isNotificationEnabled(Notification notification) {
    StatisticData data = (StatisticData)notification.getUserData();
    return data.getSessionId().equals(sessionId);
  }
}