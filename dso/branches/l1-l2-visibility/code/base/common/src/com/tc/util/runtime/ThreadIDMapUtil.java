/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

public class ThreadIDMapUtil {

  private final TCLogger logger = TCLogging.getLogger(ThreadIDMapUtil.class);
  private final Class    threadIDMapInfoJdk15Type;

  public ThreadIDMapUtil() {
    if (Vm.isJDK15Compliant()) {
      try {
        threadIDMapInfoJdk15Type = Class.forName("com.tc.util.runtime.ThreadIDMapUtilJDK15");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("Class ThreadIDMapUtilJDK15 not found");
      }
    } else {
      threadIDMapInfoJdk15Type = null;
      logger.warn("VM ThreadID and TC ThreadID Mapping is available only from JDK1.5");
    }
  }

  public ThreadIDMap getInstance() {
    if (threadIDMapInfoJdk15Type != null) {
      try {
        return (ThreadIDMap) threadIDMapInfoJdk15Type.newInstance();
      } catch (Exception e) {
        logger.warn("Not able to create new instance of ThreadIDMapInfoJDK15 : " + e);
      }
    }
    return new NullThreadIDMap();
  }
}