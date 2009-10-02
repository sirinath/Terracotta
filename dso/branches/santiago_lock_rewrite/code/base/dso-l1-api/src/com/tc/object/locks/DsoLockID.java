/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.ManagerUtil;

import java.io.IOException;

public class DsoLockID implements LockID {
  private long   objectId;
  private Object javaObject;
  
  public DsoLockID() {
    // for tc serialization
  }
  
  public DsoLockID(ObjectID objectId, Object javaObject) {
    this.objectId = objectId.toLong();
    this.javaObject = javaObject;
  }

  public DsoLockID(ObjectID objectId) {
    this.objectId = objectId.toLong();
  }
  
  public DsoLockID(Object javaObject) {
    this.objectId = -1;
    this.javaObject = javaObject;
  }
  
  public String asString() {
    return null;
  }

  public LockIDType getLockType() {
    return LockIDType.DSO;
  }

  @Deprecated
  public boolean isNull() {
    return false;
  }

  public Object javaObject() {
    return javaObject;
  }
  
  public boolean isClustered() {
    if (objectId == -1) {
      return false;
    } else {
      return !ManagerUtil.lookupExistingOrNull(javaObject()).autoLockingDisabled();
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
    if (objectId == -1) {
      return System.identityHashCode(javaObject());
    } else {
      return ((int) objectId) ^ ((int) (objectId >>> 32));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoLockID) {
      if (objectId == -1) {
        return javaObject() == ((DsoLockID) o).javaObject();
      } else {
        return objectId == ((DsoLockID) o).objectId;
      }
    } else {
      return false;
    }
  }
  
  public String toString() {
    if (objectId == -1) {
      return "DsoLockID(" + hashCode() + ")";
    } else {
      return "DsoLockID(" + new ObjectID(objectId) + ")";
    }
  }
}
