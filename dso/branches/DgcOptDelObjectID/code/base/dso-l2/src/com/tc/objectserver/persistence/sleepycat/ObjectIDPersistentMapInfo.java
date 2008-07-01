/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;

public interface ObjectIDPersistentMapInfo {

  public boolean isPersistMapped(ObjectID id);

  public void setPersistent(ObjectID id);

  public void clrPersistent(ObjectID id);
}
