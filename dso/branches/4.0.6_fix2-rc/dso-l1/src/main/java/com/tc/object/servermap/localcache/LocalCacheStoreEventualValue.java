/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;

public class LocalCacheStoreEventualValue extends AbstractLocalCacheStoreValue {
  public LocalCacheStoreEventualValue() {
    //
  }

  public LocalCacheStoreEventualValue(ObjectID id, Object value) {
    super(id, value);
  }

  @Override
  public boolean isEventualConsistentValue() {
    return true;
  }

  @Override
  public ObjectID getValueObjectId() {
    return (ObjectID) id;
  }
}
