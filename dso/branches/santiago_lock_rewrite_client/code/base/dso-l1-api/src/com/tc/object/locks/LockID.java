/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCSerializable;

import java.io.Serializable;

public interface LockID extends TCSerializable, Serializable {
  static enum LockIDType {STRING, LONG, DSO;}

  public LockIDType getLockType();
  
  @Deprecated
  public String asString();

  @Deprecated
  public boolean isNull();
  
  public Object unclusteredObject();
}
