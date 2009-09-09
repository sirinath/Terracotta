/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;

public class EntryForKeyResponseContext implements EventContext {

  private final ClientID      clientID;
  private final ManagedObject mo;
  private final ObjectIDSet   pendingLookups;
  private final ObjectID      mapID;
  private final Object        portableKey;

  public EntryForKeyResponseContext(final ClientID clientID, final ManagedObject mo, final ObjectIDSet pendingLookups,
                                    final ObjectID mapID, final Object portableKey) {
    this.clientID = clientID;
    this.mo = mo;
    this.pendingLookups = pendingLookups;
    this.mapID = mapID;
    this.portableKey = portableKey;
  }

  public ClientID getClientID() {
    return this.clientID;
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

  public Object getPortableKey() {
    return this.portableKey;
  }

  @Override
  public String toString() {
    return "EntryForKeyResponseContext [ clientID : " + this.clientID + "map : " + this.mapID + " key : "
           + this.portableKey + "]";
  }
}
