/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;

public class NullObjectIDPersistentMapInfo implements ObjectIDPersistentMapInfo {

  public void clrPersistent(ObjectID id) {
    // No oper
  }

  public boolean isPersistMapped(ObjectID id) {
    return false;
  }

  public void setPersistent(ObjectID id) {
    // No oper
  }

  public void clear() {
    // No oper
  }

}
