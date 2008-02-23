/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;

/**
 * A Utility class containing utility methods for thread dumps
 * Only supports JDK 1.5 and above.
 */
public class ThreadDumpUtil {

  public static final TCLogger logger = TCLogging.getLogger(ThreadDumpUtil.class);

  private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

  private ThreadDumpUtil() {
  }

  public static String getThreadDump() {

    StringBuilder sb = new StringBuilder();

    sb.append(new Date().toString());
    sb.append('\n');
    sb.append("Full thread dump ");
    sb.append(System.getProperty("java.vm.name"));
    sb.append(" (");
    sb.append(System.getProperty("java.vm.version"));
    sb.append(' ');
    sb.append(System.getProperty("java.vm.info"));
    sb.append("):\n\n");

    try {
      long[] threadIds = getAllThreadIds();

      for (int i = 0; i < threadIds.length; i++) {
        ThreadInfo threadInfo = getThreadInfo(threadIds[i]);

        if (threadInfo != null) {
          String s = threadInfo.toString();

          if (s.indexOf('\n') == -1) { // 1.5
            sb.append(getThreadHeader(threadInfo));
            sb.append('\n');

            StackTraceElement[] stea = getStackTrace(threadInfo);
            for (int j = 0; j < stea.length; j++) {
              sb.append("\tat ");
              sb.append(stea[j].toString());
              sb.append('\n');
            }
            sb.append('\n');
          } else {
            sb.append(s);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      sb.append(e.toString());
    }

    String text = sb.toString();
    logger.info(text);

    return text;
  }

  public static String getThreadHeader(long threadId) {
    return getThreadHeader(getThreadInfo(threadId));
  }

  public static String getThreadHeader(ThreadInfo threadInfo) {
    try {
      String threadName = threadInfo.getThreadName();
      java.lang.Thread.State threadState = threadInfo.getThreadState();
      String lockName = threadInfo.getLockName();
      String lockOwnerName = threadInfo.getLockOwnerName();
      Long lockOwnerId = threadInfo.getLockOwnerId();
      Boolean isSuspended = threadInfo.isSuspended();
      Boolean isInNative = threadInfo.isInNative();

      StringBuilder sb = new StringBuilder();
      sb.append("\"");
      sb.append(threadName);
      sb.append("\" ");
      sb.append("Id=");
      sb.append(threadInfo.getThreadId());
      sb.append(" ");
      sb.append(threadState);
      if (lockName != null) {
        sb.append(" on ");
        sb.append(lockName);
      }
      if (lockOwnerName != null) {
        sb.append(" owned by \"");
        sb.append(lockOwnerName);
        sb.append("\" Id=");
        sb.append(lockOwnerId);
      }
      if (isSuspended.booleanValue()) {
        sb.append(" (suspended)");
      }
      if (isInNative.booleanValue()) {
        sb.append(" (in native)");
      }

      return sb.toString();
    } catch (Exception e) {
      return threadInfo.toString();
    }
  }


  public static StackTraceElement[] getStackTrace(long threadId) {
    return getThreadInfo(threadId).getStackTrace();
  }

  public static StackTraceElement[] getStackTrace(ThreadInfo threadInfo) {
    return threadInfo.getStackTrace();
  }


  public static long[] getAllThreadIds() {
    return ManagementFactory.getThreadMXBean().getAllThreadIds();
  }

  public static ThreadInfo getThreadInfo(long threadId) {
    return threadMXBean.getThreadInfo(threadId, Integer.MAX_VALUE);
  }

  public static void main(String[] args) throws InterruptedException {
    System.out.println("First Dump");
    System.out.println(getThreadDump());

    new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        System.out.println("Second Dump: \n" + getThreadDump());
      }
    }).start();
    System.out.println("Main thread going to sleep...");
    Thread.sleep(5000);
  }

}
