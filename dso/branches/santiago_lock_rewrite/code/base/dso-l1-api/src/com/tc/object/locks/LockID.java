/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCSerializable;

public interface LockID extends TCSerializable {
  public static final byte STRING_TYPE = 0x01;
  public static final byte LONG_TYPE   = 0x02;

  public byte getLockType();
  
  public String asString();

  public boolean isNull();
}
