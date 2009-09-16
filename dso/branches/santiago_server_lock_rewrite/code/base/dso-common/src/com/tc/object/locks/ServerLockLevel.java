/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.LockLevel;

public enum ServerLockLevel {
  READ, WRITE, NONE;

  public static LockLevel toClientLockLevel(ServerLockLevel lockLevel) {
    switch (lockLevel) {
      case READ:
        return LockLevel.READ;
      case WRITE:
        return LockLevel.WRITE;
      default:
        throw new AssertionError("Unknown State: " + lockLevel);
    }
  }

  @Deprecated
  public static ServerLockLevel fromLegacyInt(int level) {
    switch (level) {
      case com.tc.object.lockmanager.api.LockLevel.NIL_LOCK_LEVEL:
        return NONE;
      case com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.READ:
      case com.tc.object.lockmanager.api.LockLevel.READ:
        return READ;
      case com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.WRITE:
      case com.tc.object.lockmanager.api.LockLevel.WRITE:
        return WRITE;
    }
    throw new IllegalArgumentException("Level is " + level);
  }

  @Deprecated
  public static int toLegacyInt(ServerLockLevel level) {
    switch (level) {
      case READ:
        return com.tc.object.lockmanager.api.LockLevel.READ;
      case WRITE:
        return com.tc.object.lockmanager.api.LockLevel.WRITE;
      case NONE:
        return com.tc.object.lockmanager.api.LockLevel.NIL_LOCK_LEVEL;
    }
    throw new IllegalArgumentException();
  }
}
