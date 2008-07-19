/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.reflect.Method;

public class ThreadIDMapInfo {

  private static final TCLogger logger = TCLogging.getLogger(ThreadIDMapInfo.class);
  private static Class          threadIDMapInfoJdk15Type;

  static {
    try {
      threadIDMapInfoJdk15Type = Class.forName("com.tc.util.runtime.ThreadIDMapInfoJDK15");
    } catch (ClassNotFoundException cnfe) {
      threadIDMapInfoJdk15Type = null;
    }
  }

  public synchronized static void addThread(Long tcThreadID) {
    try {
      if (!Vm.isJDK15Compliant()) {
        logger.warn("VM Thread and TC Thread ID Mapping is only available from JDK1.5");
        return;
      }

      Method method = null;
      if (threadIDMapInfoJdk15Type != null) {
        method = threadIDMapInfoJdk15Type.getMethod("addThread", new Class[] { Long.class });
      } else {
        logger.info("addThread Method not found");
        return;
      }
      method.invoke(null, new Long[] { tcThreadID });
    } catch (Exception e) {
      logger.error("Exception during adding map for TC Thread" + e.getMessage(), e);
    }
  }

  public static Long getTCThreadID(Long vmThreadID) {
    try {
      if (!Vm.isJDK15Compliant()) { return null; }
      Method method = null;
      if (threadIDMapInfoJdk15Type != null) {
        method = threadIDMapInfoJdk15Type.getMethod("getThread", new Class[] { Long.class });
      } else {
        logger.info("getThread Method not found");
        return null;
      }
      Object o = method.invoke(null, new Long[] { vmThreadID });
      if (o == null) return null;
      else return (Long) o;
    } catch (Exception e) {
      logger.error("Exception during getting map for VM Thread" + e.getMessage(), e);
    }
    return null;
  }
}