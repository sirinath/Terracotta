/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class NonObjectLockID implements LockID {
  /** Indicates no lock identifier */
  public final static ConcurrentMap<NonObjectLockID, Object> JAVA_OBJECTS = new ConcurrentHashMap<NonObjectLockID, Object>();

  public boolean isClustered() {
    return true;
  }

  public Object javaObject() {
    Object jObject = JAVA_OBJECTS.get(this);
    if (jObject == null) {
      jObject = new Object();
      Object racer = JAVA_OBJECTS.putIfAbsent(this, jObject);
      if (racer != null) {
        return racer;
      }
    }
    return jObject;
  }
}
