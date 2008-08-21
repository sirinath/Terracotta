/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import com.tc.test.MultipleServersCrashMode;

public class ActivePassiveCrashMode extends MultipleServersCrashMode {

  public static final String AP_CUSTOMERIZED_CRASH = "active-passive-customerized-crash";

  public ActivePassiveCrashMode(String mode) {
    super(mode);
    if (!mode.equals(AP_CUSTOMERIZED_CRASH)) { throw new AssertionError("Unrecognized crash mode [" + mode + "]"); }
  }
}
