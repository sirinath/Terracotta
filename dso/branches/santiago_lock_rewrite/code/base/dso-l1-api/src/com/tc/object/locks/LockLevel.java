/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public enum LockLevel {
  READ, WRITE,
  SYNCHRONOUS_WRITE,
  CONCURRENT;
  
  public boolean isWrite() {
    switch (this) {
      case WRITE:
      case SYNCHRONOUS_WRITE:
        return true;
      default:
        return false;
    }
  }
  
  public boolean isRead() {
    switch (this) {
      case READ:
        return true;
      default:
        return false;
    }
  }
  
  public boolean flushOnUnlock() {
    return SYNCHRONOUS_WRITE.equals(this);
  }
  
  @Deprecated
  public static LockLevel fromLegacyInt(int level) {
    switch (level) {
      case com.tc.object.lockmanager.api.LockLevel.READ: return READ;
      case com.tc.object.lockmanager.api.LockLevel.WRITE: return WRITE;

      case com.tc.object.lockmanager.api.LockLevel.SYNCHRONOUS_WRITE: return SYNCHRONOUS_WRITE;
      case com.tc.object.lockmanager.api.LockLevel.CONCURRENT: return CONCURRENT;
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
    }
    throw new IllegalArgumentException();
  }  
}
