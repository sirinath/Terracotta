/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

public interface MultipleServersCrashMode {

  public static final String CRASH_AFTER_MUTATE      = "crash-after-mutate";
  public static final String CONTINUOUS_ACTIVE_CRASH = "continuous-active-crash";
  public static final String RANDOM_SERVER_CRASH     = "random-server-crash";

  public String getMode();
}
