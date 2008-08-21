/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activeactive;

import com.tc.test.MultipleServersCrashMode;

public class ActiveActiveCrashMode implements MultipleServersCrashMode {

  public static final String AA_CUSTOMIZED_CRASH = "active-active-customized-crash";

  protected String           mode;

  public ActiveActiveCrashMode(String mode) {
    this.mode = mode;
    if (!mode.equals(CRASH_AFTER_MUTATE) && !mode.equals(CONTINUOUS_ACTIVE_CRASH) && !mode.equals(RANDOM_SERVER_CRASH)
        && !mode.equals(AA_CUSTOMIZED_CRASH)) { throw new AssertionError("Unrecognized crash mode [" + mode + "]"); }
  }

  public String getMode() {
    if (mode == null) { throw new AssertionError("Mode was not set"); }
    return mode;
  }
}
