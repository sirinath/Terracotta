/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

public class UnclusteredLockID implements LockID {

  public static final UnclusteredLockID UNCLUSTERED_LOCK_ID = new UnclusteredLockID();
  
  private UnclusteredLockID() {
    //
  }
  
  public String asString() {
    return null;
  }

  public LockIDType getLockType() {
    throw new AssertionError();
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) {
    throw new AssertionError();
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    throw new AssertionError();
  }
}
