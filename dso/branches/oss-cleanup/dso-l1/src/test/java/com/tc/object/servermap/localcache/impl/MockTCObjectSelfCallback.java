/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.util.BitSetObjectIDSet;

import java.util.Set;

public class MockTCObjectSelfCallback implements TCObjectSelfCallback {
  private final Set<ObjectID> oids = new BitSetObjectIDSet();

  @Override
  public void initializeTCClazzIfRequired(TCObjectSelf tcoObjectSelf) {
    // NO OP
    // We do not have tc class factory here
  }

  @Override
  public synchronized void removedTCObjectSelfFromStore(TCObjectSelf tcoObjectSelf) {
    oids.add(tcoObjectSelf.getObjectID());
  }

  public synchronized Set<ObjectID> getRemovedSet() {
    return oids;
  }

  public void removedTCObjectSelfFromStore(ObjectID objectID) {
    oids.add(objectID);
  }
}