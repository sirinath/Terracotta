/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.exception.TCRuntimeException;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.statistics.StatisticData;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.util.TCTimerImpl;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

public class StatisticsEmitter extends AbstractTerracottaMBean implements StatisticsEmitterMBean, StatisticsBufferListener {
  private static final String STATISTICS_EVENT_TYPE = "tc.statistics.event";
  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

  static {
    final String[] notifTypes = new String[] { STATISTICS_EVENT_TYPE };
    final String name = Notification.class.getName();
    final String description = "Each notification sent contains a Terracotta statistics event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final SynchronizedLong sequenceNumber = new SynchronizedLong(0L);

  private final StatisticsConfig config;
  private final StatisticsBuffer buffer;
  private final Set activeSessionIds = Collections.synchronizedSet(new HashSet());

  private Timer timer = null;
  private TimerTask task = null;

  public StatisticsEmitter(StatisticsConfig config, StatisticsBuffer buffer) throws NotCompliantMBeanException {
    super(StatisticsEmitterMBean.class, true, false);
    Assert.assertNotNull("config", config);
    Assert.assertNotNull("buffer", buffer);
    this.config = config;
    this.buffer = buffer;
    this.buffer.addListener(this);
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

  protected synchronized void enabledStateChanged() {
    if (isEnabled()) {
      enableTimer();
    } else {
      disableTimer();
    }
  }

  private synchronized void enableTimer() {
    if (timer != null &&
        task != null) {
      disableTimer();
    }

    timer = new TCTimerImpl("Statistics Emitter Timer", true);
    task = new SendStatsTask();
    timer.scheduleAtFixedRate(task, 0, config.getParamLong(StatisticsConfig.KEY_EMITTER_SCHEDULE_PERIOD));
  }

  private synchronized void disableTimer() {
    if (timer != null &&
        task != null) {
      task.cancel();
      timer.cancel();
      task = null;
      timer = null;
    }
  }

  public void reset() {
  }

  public void capturingStarted(long sessionId) {
    activeSessionIds.add(new Long(sessionId));
  }

  public void capturingStopped(long sessionId) {
  }

  private class SendStatsTask extends TimerTask {
    public void run() {
      boolean has_listeners = hasListeners();
      if (has_listeners
          && !activeSessionIds.isEmpty()) {
        Iterator it = activeSessionIds.iterator();
        while (it.hasNext()) {
          try {
            buffer.consumeStatistics(((Long)it.next()).longValue(), new StatisticsConsumer() {
              public boolean consumeStatisticData(StatisticData data) {
                // create the notification event
                final Notification notification = new Notification(STATISTICS_EVENT_TYPE, StatisticsEmitter.this, sequenceNumber.increment(), System.currentTimeMillis());
                notification.setUserData(data);
                sendNotification(notification);

                // detect when a capture session has shut down and remove it
                // from the list of active sessions
                if (SRAShutdownTimestamp.ACTION_NAME.equals(data.getName())) {
                  activeSessionIds.remove(data.getSessionId());
                }

                return true;
              }
            });
          } catch (TCStatisticsBufferException e) {
            throw new TCRuntimeException(e); // HACK: properly handle exception, need to investigate if it should be propagated or logged
          }
        }
      }
    }
  }
}