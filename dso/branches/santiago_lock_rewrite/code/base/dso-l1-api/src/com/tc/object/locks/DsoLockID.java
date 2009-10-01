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
  private ObjectID objectID;
  private Object   javaObject;
  
  public DsoLockID() {
    // for tc serialization
  }
  
  public DsoLockID(ObjectID objectID, Object javaObject) {
    this.objectID = objectID;
    this.javaObject = javaObject;
  }

  public DsoLockID(ObjectID objectID) {
    this.objectID = objectID;
  }
  
  public DsoLockID(Object javaObject) {
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
    if (objectID == null) {
      return false;
    } else {
      return !ManagerUtil.lookupExistingOrNull(javaObject()).autoLockingDisabled();
    }
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    objectID = new ObjectID(serialInput.readLong());
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(objectID.toLong());
  }

  @Override
  public int hashCode() {
    if (objectID == null) {
      return System.identityHashCode(javaObject());
    } else {
      return objectID.hashCode();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoLockID) {
      if (objectID == null) {
        return javaObject() == ((DsoLockID) o).javaObject();
      } else {
        return objectID.equals(((DsoLockID) o).objectID);
      }
    } else {
      return false;
    }
  }
  
  public String toString() {
    if (objectID == null) {
      return "DsoLockID(" + hashCode() + ")";
    } else {
      return "DsoLockID(" + objectID + ")";
    }
  }
}
