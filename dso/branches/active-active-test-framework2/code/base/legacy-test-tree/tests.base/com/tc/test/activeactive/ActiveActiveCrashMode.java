/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activeactive;

import com.tc.test.MultipleServersCrashMode;

public class ActiveActiveCrashMode extends MultipleServersCrashMode {
  public static final String AA_CUSTOMERIZED_CRASH = "active-active-customerized-crash";

  public ActiveActiveCrashMode(String mode) {
    super(mode);
    if (!mode.equals(AA_CUSTOMERIZED_CRASH)) { throw new AssertionError("Unrecognized crash mode [" + mode + "]"); }
  }
}
