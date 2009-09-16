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

  public Object unclusteredObject() {
    if (obj instanceof ObjectID) {
      return null;
    } else {
      return obj;
    }
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    obj = new ObjectID(serialInput.readLong());
    return obj;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    if (obj instanceof ObjectID) {
      serialOutput.writeLong(((ObjectID) obj).toLong());
    }
  }

}
