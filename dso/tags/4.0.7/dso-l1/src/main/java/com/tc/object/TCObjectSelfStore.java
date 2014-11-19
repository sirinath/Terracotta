/*
 */
package com.tc.object;

import com.tc.invalidation.Invalidations;
import com.tc.net.NodeID;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;

import java.util.Set;

public interface TCObjectSelfStore extends ClearableCallback {
  void initializeTCObjectSelfStore(TCObjectSelfCallback callback);

  Object getById(ObjectID oid);

  boolean addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                          Object tcoself, boolean isNew);

  int size();

  void addAllObjectIDs(Set oids);

  boolean contains(ObjectID objectID);

  void addTCObjectSelfTemp(TCObjectSelf obj);

  void removeTCObjectSelfTemp(TCObjectSelf value, boolean notifyServer);

  public void removeTCObjectSelf(AbstractLocalCacheStoreValue localStoreValue);

  void initializeTCObjectSelfIfRequired(TCObjectSelf tcoSelf);

  public void removeObjectById(ObjectID oid);

  /**
   * Handshake manager tries to get hold of all the objects present in the local caches
   * 
   * @param remoteNode
   */
  public void addAllObjectIDsToValidate(Invalidations invalidations, NodeID remoteNode);

  void shutdown(boolean fromShutdownHook);

  void rejoinInProgress(boolean rejoinInProgress);

  void removeTCObjectSelf(TCObjectSelf self);
}
