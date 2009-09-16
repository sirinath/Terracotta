/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public interface LockFactory {
  Lock createLock(LockID lid);
}
