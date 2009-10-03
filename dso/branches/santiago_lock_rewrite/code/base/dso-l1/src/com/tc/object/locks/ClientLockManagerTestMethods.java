/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.lockmanager.api.WaitListener;

public interface ClientLockManagerTestMethods {
  /**
   * Called by test code to force a lock gc pass.
   * 
   * @return the count of live locks (post collection)
   */
  public int runLockGc();

  public void wait(LockID lock, WaitListener listener) throws InterruptedException;
  public void wait(LockID lock, WaitListener listener, long timeout) throws InterruptedException;
}
