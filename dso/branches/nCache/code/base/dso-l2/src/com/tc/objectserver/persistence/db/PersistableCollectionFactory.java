/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;
import com.tc.objectserver.storage.api.TCMapsDatabase.BackingMapFactory;

import java.util.Map;
import java.util.Set;

public class PersistableCollectionFactory implements PersistentCollectionFactory {

  private final BackingMapFactory factory;

  public PersistableCollectionFactory(final BackingMapFactory factory) {
    this.factory = factory;
  }

  public Map createPersistentMap(final ObjectID id) {
    return new TCPersistableMap(id, this.factory.createBackingMapFor(id));
  }

  public Set createPersistentSet(final ObjectID id) {
    return new TCPersistableSet(id, this.factory.createBackingMapFor(id));
  }

}
