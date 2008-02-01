/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.util.Assert;

import java.util.Collection;

import javax.management.Notification;
import javax.management.NotificationListener;

public class CollectingNotificationListener implements NotificationListener {
  private boolean shutdown = false;

  public boolean getShutdown() {
    return shutdown;
  }

  public void handleNotification(Notification notification, Object o) {
    Assert.assertTrue("Expecting notification data to be a collection", o instanceof Collection);

    StatisticData data = (StatisticData)notification.getUserData();
    ((Collection)o).add(data);
    if (SRAShutdownTimestamp.ACTION_NAME.equals(data.getName())) {
      shutdown = true;
      synchronized (this) {
        this.notifyAll();
      }
    }
  }
}