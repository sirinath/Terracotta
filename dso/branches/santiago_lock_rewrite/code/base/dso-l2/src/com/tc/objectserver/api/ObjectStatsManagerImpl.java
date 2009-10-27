/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

public class ObjectStatsManagerImpl implements ObjectStatsManager {
  private final ObjectManagerMBean objectManager;

  public ObjectStatsManagerImpl(ObjectManagerMBean manager) {
    this.objectManager = manager;
  }

  public String getObjectTypeFromID(ObjectID id) {
    ManagedObjectFacade objFacade = null;
    try {
      objFacade = objectManager.lookupFacade(id, 0);
    } catch (Exception e) {
      return null;
    }
    return objFacade.getClassName();
  }

}
