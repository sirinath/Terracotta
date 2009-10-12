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
  
  public int toInt() {
    return this.ordinal();
  }

  public static LockLevel fromInt(int integer) {
    try {
      return LockLevel.values()[integer];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException(e);
    }
  }  
}
