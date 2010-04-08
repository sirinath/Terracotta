/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.TCObject;

public class NullManagerInternal extends NullManager implements ManagerInternal {

  public TCObject lookupExistingOrNull(Object obj) {
    return null;
  }

  public TCObject lookupArrayTCObjectOrNull(Object array) {
    return null;
  }

  public void charArrayCopy(char[] src, int srcPos, char[] dest, int destPos, int length, TCObject tco) {
    throw new AssertionError("should never be called");
  }

}
