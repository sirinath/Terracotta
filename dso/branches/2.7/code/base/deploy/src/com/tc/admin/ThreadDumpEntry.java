/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.awt.Point;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadDumpEntry implements Runnable {
  private Future<String> m_threadDumpFuture;
  private String         m_text;
  private Date           m_time;
  private Point          m_viewPosition;

  private static final int TIMEOUT_SECONDS = 30;
  
  public ThreadDumpEntry(Future<String> threadDumpFuture) {
    m_threadDumpFuture = threadDumpFuture;
    m_time = new Date();
    AdminClient.getContext().submit(this);
  }

  public void run() {
    String result;
    try {
      result = m_threadDumpFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      result = MessageFormat.format("Timed-out after {0} seconds.\n", TIMEOUT_SECONDS);
    } catch (Exception e) {
      result = e.getMessage();
    }
    setThreadDump(result);
  }

  synchronized void setThreadDump(String text) {
    m_text = text;
  }

  public synchronized String getThreadDump() {
    return m_text;
  }

  public boolean isDone() {
    return getThreadDump() != null;
  }

  public void cancel() {
    if (!isDone()) {
      m_threadDumpFuture.cancel(true);
      setThreadDump("Canceled");
    }
  }

  String getContent() {
    if (isDone()) {
      return getThreadDump();
    } else {
      return "Waiting...\n";
    }
  }

  public Date getTime() {
    return new Date(m_time.getTime());
  }

  public void setViewPosition(Point pos) {
    m_viewPosition = pos;
  }

  public Point getViewPosition() {
    return m_viewPosition;
  }

  public String toString() {
    return m_time.toString();
  }
}