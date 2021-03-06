/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ManagedObjectFaultingContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.api.ManagedObjectStore;

public class ManagedObjectFaultHandler extends AbstractEventHandler {

  private ObjectManager objectManager;
  private ManagedObjectStore objectStore;

  public void handleEvent(EventContext context) {
    ManagedObjectFaultingContext mfc = (ManagedObjectFaultingContext) context;
    ObjectID oid = mfc.getId();
    ManagedObject mo = objectStore.getObjectByID(oid);
    objectManager.addFaultedObject(oid, mo, mfc.isRemoveOnRelease());
  }
  
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    objectManager = oscc.getObjectManager();
    objectStore = oscc.getObjectStore();
  }

}
