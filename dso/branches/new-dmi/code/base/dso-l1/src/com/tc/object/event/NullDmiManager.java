/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.event;

import com.tc.object.dmi.DmiDescriptor;

public final class NullDmiManager implements DmiManager {

  public void distributedInvoke(Object receiver, String method, Object[] params) {
    // nothing to do
  }

  public void invoke(DmiDescriptor dd) {
    // nothing to do
  }

}
