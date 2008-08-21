/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activeactive;

import com.tc.test.MultipleServersCrashMode;

public class ActiveActiveCrashMode extends MultipleServersCrashMode {
  public static final String CRASH_AFTER_MUTATE      = "crash-after-mutate";
  public static final String CONTINUOUS_ACTIVE_CRASH = "continuous-active-crash";
  public static final String RANDOM_SERVER_CRASH     = "random-server-crash";
  public static final String AA_CUSTOMIZED_CRASH     = "active-active-customized-crash";

  public ActiveActiveCrashMode(String mode) {
    super(mode);
    if (!mode.equals(CRASH_AFTER_MUTATE) && !mode.equals(CONTINUOUS_ACTIVE_CRASH) && !mode.equals(RANDOM_SERVER_CRASH)
        && !mode.equals(AA_CUSTOMIZED_CRASH)) { throw new AssertionError("Unrecognized crash mode [" + mode + "]"); }
  }
}
