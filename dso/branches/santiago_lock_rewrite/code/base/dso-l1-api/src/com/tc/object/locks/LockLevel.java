/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public enum LockLevel {
  READ, WRITE,
  SYNCHRONOUS_WRITE,
  CONCURRENT;

  /**
   * Static lookup array used to avoid clone call in LockLevel.values()
   */
  private static final LockLevel[]   VALUES = LockLevel.values();

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
      return VALUES[integer];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException(e);
    }
  }  
}
