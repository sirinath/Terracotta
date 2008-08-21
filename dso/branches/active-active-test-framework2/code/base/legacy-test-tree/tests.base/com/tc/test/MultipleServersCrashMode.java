/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

public abstract class MultipleServersCrashMode {

  protected String           mode;

  protected MultipleServersCrashMode(String mode) {
    this.mode = mode;
  }
  
  public String getMode() {
    if (mode == null) { throw new AssertionError("Mode was not set"); }
    return mode;
  }
}
