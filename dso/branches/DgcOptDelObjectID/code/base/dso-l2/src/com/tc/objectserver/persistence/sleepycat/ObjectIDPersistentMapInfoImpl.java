/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class ObjectIDPersistentMapInfoImpl implements ObjectIDPersistentMapInfo {
  private final int             longsPerDiskEntry;
  private final OidBitsArrayMap bitsArray;

  public ObjectIDPersistentMapInfoImpl() {
    longsPerDiskEntry = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_STATE_LONGS_PERDISKENTRY);
    bitsArray = new OidBitsArrayMap(longsPerDiskEntry, null);
  }

  public boolean isPersistMapped(ObjectID id) {
    return bitsArray.contains(id);
  }

  public void setPersistent(ObjectID id) {
    bitsArray.getAndSet(id);
  }

  public void clrPersistent(ObjectID id) {
    bitsArray.removeIfEmpty(bitsArray.getAndClr(id));
  }

  public void clear() {
    bitsArray.clear();
  }
}
