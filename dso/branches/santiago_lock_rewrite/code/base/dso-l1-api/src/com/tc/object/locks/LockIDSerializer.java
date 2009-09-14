/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

public class LockIDSerializer implements TCSerializable {
  private LockID lockID;

  public LockIDSerializer() {
    // 
  }

  public LockIDSerializer(LockID lockID) {
    this.lockID = lockID;
  }

  public LockID getLockID() {
    return lockID;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    byte type = serialInput.readByte();
    this.lockID = getImpl(type);
    this.lockID.deserializeFrom(serialInput);
    return this;
  }

  private LockID getImpl(byte type) {
    switch (type) {
      case LockID.LONG_TYPE:
        return new LongLockID();
      case LockID.STRING_TYPE:
        return new StringLockID();
      default:
        throw new AssertionError("Unknown type : " + type);
    }
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeByte(lockID.getLockType());
    lockID.serializeTo(serialOutput);
  }
}
