/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.factory;

import com.tc.object.locks.LockID;
import com.tc.objectserver.locks.Lock;
import com.tc.objectserver.locks.LockFactory;
import com.tc.objectserver.locks.ServerLock;

public class GreedyPolicyFactory implements LockFactory {
  public Lock createLock(LockID lid) {
    return new ServerLock(lid);
  }
}