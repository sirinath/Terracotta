/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.invalidation.Invalidations;
import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalStoreTransactionCompletionListener;

import java.util.Set;

public interface ServerMapLocalCache {
  L1ServerMapLocalCacheStore getInternalStore();

  /**
   * Removes all entries associated with this objectId
   */
  boolean removeEntriesForObjectId(ObjectID objectId);

  /**
   * Remove all the entries associated with this lockId
   */
  void removeEntriesForLockId(LockID lockId);

  /**
   * Lets the cache handle eviction in server
   */
  void evictedInServer(Object key);

  /**
   * Removes a key from the Local cache
   */
  void removeFromLocalCache(Object key);

  /**
   * Removes a key value pair from the local cache if the key is actually mapped to the provided value
   */
  void removeFromLocalCache(Object key, AbstractLocalCacheStoreValue value);

  /**
   * Called when the key has been evicted from the local store
   */
  void entryEvicted(Object key, Object value);

  // ///////////////////////////////
  // TCObjectServerMap methods
  // ///////////////////////////////

  /**
   * Enable/disable local caching
   */
  void setLocalCacheEnabled(boolean enable);

  void addToCache(Object key, AbstractLocalCacheStoreValue localCacheValue, MapOperationType operationType);

  AbstractLocalCacheStoreValue getLocalValueStrong(final Object key);

  /**
   * Get the value corresponding to the key if present
   */
  AbstractLocalCacheStoreValue getLocalValue(Object key);

  /**
   * Get the value corresponding to the key if present
   */
  Object getMappingUnlocked(Object key);

  /**
   * Check if the key is on-heap
   */
  boolean containsKeyOnHeap(Object key);

  /**
   * Check if the key is in off-heap
   */
  boolean containsKeyOffHeap(Object key);

  /**
   * Returns the size of the local cache
   */
  int size();

  /**
   * Size of this local cache on local heap in bytes
   */
  long onHeapSizeInBytes();

  /**
   * Size of this local cache off heap in bytes
   */
  long offHeapSizeInBytes();

  /**
   * Number of items on heap
   */
  int onHeapSize();

  /**
   * Number of items off heap
   */
  int offHeapSize();

  /**
   * Clear all elements from the local cache
   */
  void clear();

  void clearInline();

  void cleanLocalState();

  /**
   * Returns the keys present in the local cache (does not include meta items stored)
   */
  Set getKeys();

  void postTransactionCallback(Object key,
                               AbstractLocalCacheStoreValue value,
                               boolean removeEntry,
                               L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener);

  void transactionComplete(L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener);

  void recalculateSize(Object key);

  void checkInObject(Object key, Object value);

  Object checkOutObject(Object key, Object value);

  boolean registerPinnedEntryInvalidationListener(PinnedEntryInvalidationListener listener);

  boolean unRegisterPinnedEntryInvalidationListener(PinnedEntryInvalidationListener listener);

  void transactionAbortedCallback(Object key,
                                  AbstractLocalCacheStoreValue value,
                                  L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener);

  void handleObjectIDsToValidate(Invalidations invalidations);

  void addTxnInProgressKeys(Set<Object> addSet, Set<Object> removeSet);

}
