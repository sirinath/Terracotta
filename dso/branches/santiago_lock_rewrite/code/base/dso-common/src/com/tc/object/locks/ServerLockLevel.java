/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.LockLevel;

public enum ServerLockLevel {
  READ, WRITE;

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

  public static ServerLockLevel fromClientLockLevel(LockLevel lockLevel) {
    switch (lockLevel) {
      case READ:
        return ServerLockLevel.READ;
      case SYNCHRONOUS_WRITE:
      case WRITE:
        return ServerLockLevel.WRITE;
      default:
        throw new AssertionError("Unknown State: " + lockLevel);
    }
  }
  
  @Deprecated
  public static ServerLockLevel fromLegacyInt(int level) {
    switch (level) {
      case com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.READ:
      case com.tc.object.lockmanager.api.LockLevel.READ:
        return READ;
      case com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.WRITE:
      case com.tc.object.lockmanager.api.LockLevel.WRITE:
        return WRITE;
      default:
        return null;
    }
  }

  @Deprecated
  public static int toLegacyInt(ServerLockLevel level) {
    switch (level) {
      case READ:
        return com.tc.object.lockmanager.api.LockLevel.READ;
      case WRITE:
        return com.tc.object.lockmanager.api.LockLevel.WRITE;
    }
    throw new IllegalArgumentException();
  }
}
