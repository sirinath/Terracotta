/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.LockLevel;

import java.util.Enumeration;
import java.util.Vector;

public class EnumerationWrapper implements Enumeration {

  private final Vector      vector;
  private final Enumeration realEnumeration;

  public EnumerationWrapper(Vector vector, Enumeration realEnumeration) {
    this.vector = vector;
    this.realEnumeration = realEnumeration;
  }

  public final boolean hasMoreElements() {
    return realEnumeration.hasMoreElements();
  }

  public final Object nextElement() {
    ManagerUtil.monitorEnter(vector, LockLevel.WRITE);
    Object o = null;
    try {
      o = realEnumeration.nextElement();
    } finally {
      ManagerUtil.monitorExit(vector);
    }
    return o;
  }
}
