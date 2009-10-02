/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCSerializable;

public interface LockID extends TCSerializable {
  static enum LockIDType {STRING, LONG, DSO, DSO_LITERAL, DSO_VOLATILE;}

  public LockIDType getLockType();
  
  @Deprecated
  public String asString();

  @Deprecated
  public boolean isNull();
  
  public Object javaObject();
  
  public boolean isClustered();
}
