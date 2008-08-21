/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;

public abstract class TestSetupManagerBase {

  private int                            serverCount;
  private long                           serverCrashWaitTimeInSec = 15;
  private int                            maxCrashCount            = Integer.MAX_VALUE;
  private ActivePassiveSharedDataMode    activePassiveMode;
  private MultipleServersPersistenceMode persistenceMode;
  protected MultipleServersCrashMode     crashMode;

  public void setServerCount(int count) {
    if (count < 2) { throw new AssertionError("Server count must be 2 or more:  count=[" + count + "]"); }
    serverCount = count;
  }

  public int getServerCount() {
    return serverCount;
  }

  public abstract void setServerCrashMode(String mode);

  public void setMaxCrashCount(int count) {
    if (count < 0) { throw new AssertionError("Max crash count should not be a neg number"); }
    maxCrashCount = count;
  }

  public int getMaxCrashCount() {
    return maxCrashCount;
  }

  public String getServerCrashMode() {
    if (crashMode == null) { throw new AssertionError("Server crash mode was not set."); }
    return crashMode.getMode();
  }

  public void setServerShareDataMode(String mode) {
    activePassiveMode = new ActivePassiveSharedDataMode(mode);
  }

  public String getServerSharedDataMode() {
    return activePassiveMode.getMode();
  }

  public boolean isNetworkShare() {
    if (activePassiveMode == null) { throw new AssertionError("Server share mode was not set."); }
    return activePassiveMode.isNetworkShare();
  }

  public void setServerPersistenceMode(String mode) {
    persistenceMode = new MultipleServersPersistenceMode(mode);
  }

  public String getServerPersistenceMode() {
    if (persistenceMode == null) { throw new AssertionError("Server persistence mode was not set."); }
    return persistenceMode.getMode();
  }

  public void setServerCrashWaitTimeInSec(long time) {
    if (time < 0) { throw new AssertionError("Wait time should not be a negative number."); }
    serverCrashWaitTimeInSec = time;
  }

  public long getServerCrashWaitTimeInSec() {
    return serverCrashWaitTimeInSec;
  }

}
