/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

public class UnclusteredLockID implements LockID {

  private final Object javaObject;
  
  public UnclusteredLockID(Object obj) {
    javaObject = obj;
  }
  
  public String asString() {
    return null;
  }

  public LockIDType getLockType() {
    throw new AssertionError();
  }

  public Object waitNotifyObject() {
    return javaObject;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) {
    throw new AssertionError();
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    throw new AssertionError();
  }
}
