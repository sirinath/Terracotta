/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.factory;

import com.tc.object.locks.LockID;
import com.tc.objectserver.locks.Lock;
import com.tc.objectserver.locks.LockFactory;
import com.tc.objectserver.locks.NonGreedyPolicyLock;

public class NonGreedyLockPolicyFactory implements LockFactory {
  public Lock createLock(LockID lid) {
    return new NonGreedyPolicyLock(lid);
  }
}