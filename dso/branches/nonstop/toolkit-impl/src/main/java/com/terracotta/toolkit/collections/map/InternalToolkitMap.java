/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import com.tc.object.bytecode.TCServerMap;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.terracotta.toolkit.collections.map.ServerMap.GetType;
import com.terracotta.toolkit.object.TCToolkitObject;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public interface InternalToolkitMap<K, V> extends ConcurrentMap<K, V>, TCServerMap, TCToolkitObject,
    ValuesResolver<K, V> {

  String getName();

  ToolkitLockTypeInternal getLockType();

  boolean isEventual();

  boolean invalidateOnChange();

  boolean isLocalCacheEnabled();

  int getMaxTTISeconds();

  int getMaxTTLSeconds();

  int getMaxCountInCluster();

  V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds);

  void putNoReturn(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds);

  V putIfAbsent(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds);

  V get(Object key, boolean quiet);

  void addCacheListener(ToolkitCacheListener<K> listener);

  void removeCacheListener(ToolkitCacheListener<K> listener);

  void setConfigField(String name, Object value);

  void initializeLocalCache(L1ServerMapLocalCacheStore<K, V> localCacheStore, PinnedEntryFaultCallback callback);

  void removeNoReturn(Object key);

  V unsafeLocalGet(Object key);

  V unlockedGet(K key, boolean quiet);

  int localSize();

  Set<K> localKeySet();

  void unpinAll();

  boolean isPinned(K key);

  void setPinned(K key, boolean pinned);

  boolean containsLocalKey(Object key);

  V checkAndGetNonExpiredValue(K key, Object value, GetType getType, boolean quiet);

  void clearLocalCache();

  void cleanLocalState();

  long localOnHeapSizeInBytes();

  long localOffHeapSizeInBytes();

  int localOnHeapSize();

  int localOffHeapSize();

  boolean containsKeyLocalOnHeap(Object key);

  boolean containsKeyLocalOffHeap(Object key);

  void unlockedPutNoReturn(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds);

  void unlockedRemoveNoReturn(Object key);

  public void unlockedClear();

  boolean isCompressionEnabled();

  boolean isCopyOnReadEnabled();

  void disposeLocally();

  ToolkitReadWriteLock createLockForKey(K key);

  void registerAttributeExtractor(ToolkitAttributeExtractor extractor);
}
