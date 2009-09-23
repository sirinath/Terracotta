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

public class DsoVolatileLockID extends NonObjectLockID {
  public final static DsoVolatileLockID NULL_ID = new DsoVolatileLockID(ObjectID.NULL_ID, null);

  private Object obj;
  private String   fieldName;
  
  public DsoVolatileLockID() {
    //
  }
  
  public DsoVolatileLockID(Object obj, String fieldName) {
    if (obj instanceof ObjectID) {
      this.obj = obj;      
    } else if (obj instanceof TCObject) {
      this.obj = ((TCObject) obj).getObjectID();
    } else {
      TCObject tco = ManagerUtil.lookupExistingOrNull(obj);
      if (tco == null) {
        this.obj = obj;
      } else {
        this.obj = tco.getObjectID();
      }
    }
    this.fieldName = fieldName;
  }
  
  public DsoVolatileLockID(ObjectID oid, String fieldName) {
    this.obj = oid;
    this.fieldName = fieldName;
  }
  
  public boolean isNull() {
    return this == NULL_ID;
  }

  public String asString() {
    return null;
  }

  public boolean isClustered() {
    return obj instanceof ObjectID;
  }

  public LockIDType getLockType() {
    return LockIDType.DSO_VOLATILE;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    obj = new ObjectID(serialInput.readLong());
    fieldName = serialInput.readString();
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    if (obj instanceof ObjectID) {
      serialOutput.writeLong(((ObjectID) obj).toLong());
      serialOutput.writeString(fieldName);
    } else {
      throw new AssertionError("Attempting clustered volatiile lock on : " + obj);
    }
  }

  public int hashCode() {
    return (5 * obj.hashCode()) ^ (7 * fieldName.hashCode());
  }
  
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoVolatileLockID) {
      return obj.equals(((DsoVolatileLockID) o).obj) && fieldName.equals(((DsoVolatileLockID) o).fieldName);
    } else {
      return false;
    }
  }
}
