/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manager;

import java.io.IOException;

/**
 * LockID implementation representing a lock on a non-literal DSO clustered object.
 */
public class DsoLockID implements LockID {
  private final Manager manager;
  private long          objectId;

  
  public DsoLockID() {
    // for tc serialization
    this(null);
  }
  
  public DsoLockID(Manager manager) {
    this.manager = manager;
  }
  
  public DsoLockID(Manager manager, ObjectID objectId) {
    this(manager);
    this.objectId = objectId.toLong();
  }

  public String asString() {
    return null;
  }

  public LockIDType getLockType() {
    return LockIDType.DSO;
  }

  public Object waitNotifyObject() {
    if (manager == null) {
      return null;
    } else {
      try {
        return manager.lookupObject(new ObjectID(objectId));
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }
  }
  
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    objectId = serialInput.readLong();
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(objectId);
  }

  @Override
  public int hashCode() {
    return ((int) objectId) ^ ((int) (objectId >>> 32));
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoLockID) {
      return objectId == ((DsoLockID) o).objectId;
    } else {
      return false;
    }
  }
  
  public String toString() {
    return "DsoLockID(" + new ObjectID(objectId) + ")";
  }
}
