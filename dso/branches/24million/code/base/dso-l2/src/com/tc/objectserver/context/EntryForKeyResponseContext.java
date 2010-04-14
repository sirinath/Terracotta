/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;

public class EntryForKeyResponseContext implements EventContext {

  private final ManagedObject mo;
  private final ObjectIDSet   pendingLookups;
  private final ObjectID      mapID;
 
  public EntryForKeyResponseContext( final ManagedObject mo, final ObjectIDSet pendingLookups,
                                    final ObjectID mapID) {
    this.mo = mo;
    this.pendingLookups = pendingLookups;
    this.mapID = mapID;
  }

  public ManagedObject getManagedObject() {
    return this.mo;
  }

  public ObjectIDSet getPendingLookups() {
    return this.pendingLookups;
  }

  public ObjectID getMapID() {
    return this.mapID;
  }

 
  @Override
  public String toString() {
    return "EntryForKeyResponseContext [ map : " + this.mapID +  "]";
  }
}
