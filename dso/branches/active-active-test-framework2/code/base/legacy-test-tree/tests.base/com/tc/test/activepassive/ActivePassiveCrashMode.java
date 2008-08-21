/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import com.tc.test.MultipleServersCrashMode;

public class ActivePassiveCrashMode extends MultipleServersCrashMode {

  public static final String CRASH_AFTER_MUTATE      = "crash-after-mutate";
  public static final String CONTINUOUS_ACTIVE_CRASH = "continuous-active-crash";
  public static final String RANDOM_SERVER_CRASH     = "random-server-crash";
  public static final String AP_CUSTOMIZED_CRASH     = "active-passive-customized-crash";

  public ActivePassiveCrashMode(String mode) {
    super(mode);
    if (!mode.equals(CRASH_AFTER_MUTATE) && !mode.equals(CONTINUOUS_ACTIVE_CRASH) && !mode.equals(RANDOM_SERVER_CRASH)
        && !mode.equals(AP_CUSTOMIZED_CRASH)) { throw new AssertionError("Unrecognized crash mode [" + mode + "]"); }
  }
}
