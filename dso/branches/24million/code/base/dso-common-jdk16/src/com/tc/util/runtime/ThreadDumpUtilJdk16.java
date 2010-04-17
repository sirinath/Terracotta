/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.Date;

public class ThreadDumpUtilJdk16 extends ThreadDumpUtil {

  public static String getThreadDump() {
    return getThreadDump(new NullLockInfoByThreadIDImpl(), new NullThreadIDMapImpl());
  }

  public static String getThreadDump(final LockInfoByThreadID lockInfo, final ThreadIDMap threadIDMap) {
    final MonitorInfo[] emptyMI = new MonitorInfo[0];
    final StringBuilder sb = new StringBuilder();
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
      final Thread[] threads = ThreadDumpUtil.getAllThreads();

      for (final Thread thread : threads) {
        final long id = thread.getId();
        final ThreadInfo threadInfo = threadMXBean.getThreadInfo(new long[] { id }, true, true)[0];
        sb.append(threadHeader(thread, threadInfo));
        sb.append('\n');

        final StackTraceElement[] stea = thread.getStackTrace();
        final MonitorInfo[] monitorInfos = threadInfo != null ? threadInfo.getLockedMonitors() : emptyMI;
        for (final StackTraceElement element : stea) {
          sb.append("\tat ");
          sb.append(element.toString());
          sb.append('\n');
          for (final MonitorInfo monitorInfo : monitorInfos) {
            final StackTraceElement lockedFrame = monitorInfo.getLockedStackFrame();
            if (lockedFrame != null && lockedFrame.equals(element)) {
              sb.append("\t- locked <0x");
              sb.append(Integer.toHexString(monitorInfo.getIdentityHashCode()));
              sb.append("> (a ");
              sb.append(monitorInfo.getClassName());
              sb.append(")");
              sb.append('\n');
            }
          }
        }
        sb.append(ThreadDumpUtil.getLockList(lockInfo, threadIDMap.getTCThreadID(thread)));
        if (!threadMXBean.isObjectMonitorUsageSupported() && threadMXBean.isSynchronizerUsageSupported()) {
          sb.append(threadLockedSynchronizers(threadInfo));
        }
        sb.append('\n');
      }
    } catch (final Exception e) {
      e.printStackTrace();
      sb.append(e.toString());
    }
    return sb.toString();
  }

  private static String threadHeader(final Thread thread, final ThreadInfo threadInfo) {
    final String threadName = thread.getName();
    final StringBuffer sb = new StringBuffer();
    sb.append("\"");
    sb.append(threadName);
    sb.append("\" ");
    sb.append("Id=");
    sb.append(thread.getId());

    if (threadInfo != null) {
      try {
        final Thread.State threadState = threadInfo.getThreadState();
        final String lockName = threadInfo.getLockName();
        final String lockOwnerName = threadInfo.getLockOwnerName();
        final Long lockOwnerId = threadInfo.getLockOwnerId();
        final Boolean isSuspended = threadInfo.isSuspended();
        final Boolean isInNative = threadInfo.isInNative();

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
        if (isSuspended) {
          sb.append(" (suspended)");
        }
        if (isInNative) {
          sb.append(" (in native)");
        }
      } catch (final Exception e) {
        return threadInfo.toString();
      }
    } else {
      sb.append(" (unrecognized thread id; thread state is unavailable)");
    }

    return sb.toString();
  }

  private static String threadLockedSynchronizers(final ThreadInfo threadInfo) {
    final String NO_SYNCH_INFO = "no locked synchronizers information available\n";
    if (null == threadInfo) { return NO_SYNCH_INFO; }
    try {
      final LockInfo[] lockInfos = threadInfo.getLockedSynchronizers();
      if (lockInfos.length > 0) {
        final StringBuffer lockedSynchBuff = new StringBuffer();
        lockedSynchBuff.append("\nLocked Synchronizers: \n");
        for (final LockInfo lockInfo : lockInfos) {
          lockedSynchBuff.append(lockInfo.getClassName()).append(" <").append(lockInfo.getIdentityHashCode())
              .append("> \n");
        }
        return lockedSynchBuff.append("\n").toString();
      } else {
        return "";
      }
    } catch (final Exception e) {
      return NO_SYNCH_INFO;
    }
  }

  public static void main(final String[] args) {
    System.out.println(getThreadDump());
  }

}
