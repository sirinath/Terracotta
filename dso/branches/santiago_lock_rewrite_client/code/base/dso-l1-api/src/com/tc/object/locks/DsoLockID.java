/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.ManagerUtil;

import java.io.IOException;

public class DsoLockID implements LockID {
  public final static DsoLockID NULL_ID = new DsoLockID(ObjectID.NULL_ID);
  
  private Object obj;
  
  public DsoLockID() {
    // for tc serialization
  }
  
  public DsoLockID(Object obj) {
    TCObject tco = ManagerUtil.lookupExistingOrNull(obj);
    if (tco == null) {
      this.obj = obj;
    } else {
      this.obj = tco.getObjectID();
    }
  }

  public DsoLockID(ObjectID obj) {
    this.obj = obj;
  }
  
  public String asString() {
    return null;
  }

  public LockIDType getLockType() {
    return LockIDType.DSO;
  }

  public boolean isNull() {
    return this == NULL_ID;
  }

  public Object javaObject() {
    if (obj instanceof ObjectID) {
      return ManagerUtil.lookupObject((ObjectID) obj);
    } else {
      return obj;
    }
  }
  
  public boolean isClustered() {
    //who gives a crap about not locking on bizarre literals
    return (obj instanceof ObjectID) || ManagerUtil.isLiteralInstance(obj);
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    obj = new ObjectID(serialInput.readLong());
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    if (obj instanceof ObjectID) {
      serialOutput.writeLong(((ObjectID) obj).toLong());
    } else {
      throw new AssertionError("Need to serialize type " + obj.getClass());
    }
  }

  public int hashCode() {
    if (obj instanceof ObjectID) {
      return obj.hashCode();
    } else {
      return ManagerUtil.calculateDsoHashCode(obj);
    }
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoLockID) {
      return obj.equals(((DsoLockID) o).obj);
    } else {
      return false;
    }
  }
  
  public String toString() {
    return "DsoLockID(" + obj + ")";
  }
}
