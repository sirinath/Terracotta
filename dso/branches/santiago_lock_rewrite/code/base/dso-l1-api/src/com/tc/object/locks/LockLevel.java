/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public enum LockLevel {
  READ, WRITE,
  SYNCHRONOUS_WRITE,
  CONCURRENT,
  
  GREEDY_READ, GREEDY_WRITE;
  
  @Deprecated
  public static LockLevel fromLegacyInt(int level) {
    switch (level) {
      case com.tc.object.lockmanager.api.LockLevel.READ: return READ;
      case com.tc.object.lockmanager.api.LockLevel.WRITE: return WRITE;

      case com.tc.object.lockmanager.api.LockLevel.SYNCHRONOUS_WRITE: return SYNCHRONOUS_WRITE;
      case com.tc.object.lockmanager.api.LockLevel.CONCURRENT: return CONCURRENT;
      
      case com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.READ:
        return GREEDY_READ;
      case com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.WRITE:
        return GREEDY_WRITE;
    }
    throw new IllegalArgumentException();
  }
  
  @Deprecated
  public static int toLegacyInt(LockLevel level) {
    switch (level) {
      case READ: return com.tc.object.lockmanager.api.LockLevel.READ;
      case WRITE: return com.tc.object.lockmanager.api.LockLevel.WRITE;

      case SYNCHRONOUS_WRITE: return com.tc.object.lockmanager.api.LockLevel.SYNCHRONOUS_WRITE;
      case CONCURRENT: return com.tc.object.lockmanager.api.LockLevel.CONCURRENT;

      case GREEDY_READ:
        return com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.READ;
      case GREEDY_WRITE:
        return com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.WRITE;
    }
    throw new IllegalArgumentException();
  }
}
