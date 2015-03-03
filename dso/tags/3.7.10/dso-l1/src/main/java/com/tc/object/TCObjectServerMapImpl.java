/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.google.common.base.Preconditions;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.locks.LockID;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TCObjectServerMapImpl<L> extends TCObjectLogical implements TCObject, TCObjectServerMap<L> {

  private final static TCLogger               logger                          = TCLogging
                                                                                  .getLogger(TCObjectServerMapImpl.class);

  private static final Object[]               NO_ARGS                         = new Object[] {};

  private static final long                   GET_VALUE_FOR_KEY_LOG_THRESHOLD = 10 * 1000;

  // The condition that can require a retry is transient so we could retry immediately, this throttle is here just to
  // prevent hammering the server
  private static final long                   RETRY_GET_VALUE_FOR_KEY_SLEEP   = 10;

  private final Lock[]                        localLocks;

  static {
    boolean deprecatedProperty = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED);
    if (!deprecatedProperty) {
      // trying to disable is not supported
      logger.warn("The property '" + TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED
                  + "' has been deprecated, set the localCacheEnabled to false in config to disable local caching.");
    }
  }

  private final GroupID                       groupID;
  private final ClientObjectManager           objectManager;
  private final RemoteServerMapManager        serverMapManager;
  private final Manager                       manager;
  private volatile ServerMapLocalCache        cache;
  private volatile boolean                    invalidateOnChange;
  private volatile boolean                    localCacheEnabled;

  private volatile L1ServerMapLocalCacheStore serverMapLocalStore;
  private final TCObjectSelfStore             tcObjectSelfStore;
  final L1ServerMapLocalCacheManager          globalLocalCacheManager;
  private volatile PinnedEntryFaultCallback   callback;

  public TCObjectServerMapImpl(final Manager manager, final ClientObjectManager objectManager,
                               final RemoteServerMapManager serverMapManager, final ObjectID id, final Object peer,
                               final TCClass tcc, final boolean isNew,
                               final L1ServerMapLocalCacheManager globalLocalCacheManager) {
    super(id, peer, tcc, isNew);
    this.tcObjectSelfStore = globalLocalCacheManager;
    this.groupID = new GroupID(id.getGroupID());
    this.objectManager = objectManager;
    this.serverMapManager = serverMapManager;
    this.manager = manager;
    this.globalLocalCacheManager = globalLocalCacheManager;
    if (serverMapLocalStore != null) {
      setupLocalCache(serverMapLocalStore, callback);
    }
    int concurrency = TCPropertiesImpl.getProperties().getInt("tcObjectServerMapConcurrency", 8);
    localLocks = new Lock[concurrency];
    for (int i = 0; i < concurrency; i++) {
      localLocks[i] = new ReentrantLock();
    }
  }

  @Override
  public void initialize(final int maxTTISeconds, final int maxTTLSeconds, final int targetMaxTotalCount,
                         final boolean invalidateOnChangeFlag, final boolean localCacheEnabledFlag) {
    this.invalidateOnChange = invalidateOnChangeFlag;
    this.localCacheEnabled = localCacheEnabledFlag;
    // if tcobject is being faulted in, the TCO is created and the peer is hydrated afterwards
    // meaning initialize is called after the cache has been already created, need to update
    if (cache != null) {
      cache.setLocalCacheEnabled(localCacheEnabledFlag);
    }
  }

  /**
   * Does a logical put and updates the local cache
   * 
   * @param map ServerTCMap
   * @param lockID Lock under which this change is made
   * @param key Key Object
   * @param value Object in the mapping
   */

  @Override
  public void doLogicalPut(final TCServerMap map, final L lockID, final Object key, final Object value) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      ObjectID valueObjectID = invokeLogicalPut(map, key, value);
      addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, value, valueObjectID,
                            MapOperationType.PUT);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void doClear(TCServerMap map) {
    lockAll();
    try {
      logicalInvoke(SerializationUtil.CLEAR, SerializationUtil.PUT_SIGNATURE, NO_ARGS);
    } finally {
      unlockAll();
    }
  }

  /**
   * Does a logical put and updates the local cache but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   */

  @Override
  public void doLogicalPutUnlocked(TCServerMap map, Object key, Object value) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      ObjectID valueObjectID = invokeLogicalPut(map, key, value);

      if (!invalidateOnChange) {
        addIncoherentValueToCache(key, value, valueObjectID, MapOperationType.PUT);
      } else {
        addEventualValueToCache(key, value, valueObjectID, MapOperationType.PUT);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean doLogicalPutIfAbsentUnlocked(TCServerMap map, Object key, Object value) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
      if (item != null) {
        Object valueObject = item.getValueObject();
        if (valueObject != null) {
          // Item already present
          return false;
        }
      }

      ObjectID valueObjectID = invokeLogicalPutIfAbsent(map, key, value);

      if (!invalidateOnChange) {
        addIncoherentValueToCache(key, value, valueObjectID, MapOperationType.PUT);
      } else {
        addEventualValueToCache(key, value, valueObjectID, MapOperationType.PUT);
      }

      return true;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean doLogicalReplaceUnlocked(TCServerMap map, Object key, Object current, Object newValue) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
      if (item != null && current != item.getValueObject()) {
        // Item already present but not equal. We are doing reference equality because equals() is called at higher
        // layer
        // and because of DEV-5462
        return false;
      }
      ObjectID valueObjectID = invokeLogicalReplace(map, key, current, newValue);

      if (!invalidateOnChange) {
        addIncoherentValueToCache(key, newValue, valueObjectID, MapOperationType.PUT);
      } else {
        addEventualValueToCache(key, newValue, valueObjectID, MapOperationType.PUT);
      }

      return true;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Does a logic remove and removes from the local cache if present
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is removed
   * @param key Key Object
   */

  @Override
  public void doLogicalRemove(final TCServerMap map, final L lockID, final Object key) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      invokeLogicalRemove(map, key);
      addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, null, ObjectID.NULL_ID,
                            MapOperationType.REMOVE);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Does a two arg logical remove and removes from the local cache if present but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   */

  @Override
  public void doLogicalRemoveUnlocked(TCServerMap map, Object key) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      invokeLogicalRemove(map, key);

      if (!invalidateOnChange) {
        addIncoherentValueToCache(key, null, ObjectID.NULL_ID, MapOperationType.REMOVE);
      } else {
        addEventualValueToCache(key, null, ObjectID.NULL_ID, MapOperationType.REMOVE);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Does a two arg logical remove and removes from the local cache if present but without an associated lock
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @return
   */

  @Override
  public boolean doLogicalRemoveUnlocked(TCServerMap map, Object key, Object value) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
      if (item != null && value != item.getValueObject()) {
        // Item already present but not equal. We are doing reference equality coz equals() is called at higher layer
        // and coz of DEV-5462
        return false;
      }

      invokeLogicalRemove(map, key, value);

      if (!invalidateOnChange) {
        addIncoherentValueToCache(key, null, ObjectID.NULL_ID, MapOperationType.REMOVE);
      } else {
        addEventualValueToCache(key, null, ObjectID.NULL_ID, MapOperationType.REMOVE);
      }

      return true;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param map Map Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @param lockID Lock under which this call is made
   * @return value Object in the mapping, null if no mapping present.
   */

  @Override
  public Object getValue(final TCServerMap map, final L lockID, final Object key) {
    if (!isCacheInitialized()) { return null; }
    AbstractLocalCacheStoreValue item = this.cache.getLocalValueStrong(key);
    if (item != null) { return item.getValueObject(); }

    // Doing double checking to ensure correct value
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      item = this.cache.getLocalValueStrong(key);
      if (item != null) { return item.getValueObject(); }

      Object value = getValueForKeyFromServer(map, key, false);

      if (value != null) {
        addStrongValueToCache(this.manager.generateLockIdentifier(lockID), key, value,
                              objectManager.lookupExistingObjectID(value), MapOperationType.GET);
      }
      return value;
    } finally {
      lock.unlock();
    }
  }

  private void updateLocalCacheIfNecessary(final Object key, final Object value) {
    // Null values (i.e. cache misses) & literal values are not cached locally
    if (value != null && !LiteralValues.isLiteralInstance(value)) {
      if (invalidateOnChange) {
        //
        addEventualValueToCache(key, value, this.objectManager.lookupExistingObjectID(value), MapOperationType.GET);
      } else {
        addIncoherentValueToCache(key, value, this.objectManager.lookupExistingObjectID(value), MapOperationType.GET);
      }
    }
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap outside a lock context.
   * 
   * @param map Map Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */

  @Override
  public Object getValueUnlocked(TCServerMap map, Object key) {
    AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
    if (item != null) { return item.getValueObject(); }

    // Doing double checking to ensure correct value
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      item = getValueUnlockedFromCache(key);
      if (item != null) { return item.getValueObject(); }

      Object value = getValueForKeyFromServer(map, key, true);

      if (value != null) {
        updateLocalCacheIfNecessary(key, value);
      }
      return value;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Map<Object, Object> getAllValuesUnlocked(final Map<ObjectID, Set<Object>> mapIdToKeysMap) {
    Map<Object, Object> rv = new HashMap<Object, Object>();
    for (Iterator<Entry<ObjectID, Set<Object>>> iterator = mapIdToKeysMap.entrySet().iterator(); iterator.hasNext();) {
      Entry<ObjectID, Set<Object>> entry = iterator.next();
      Set<Object> keys = entry.getValue();
      for (Iterator i = keys.iterator(); i.hasNext();) {
        Object key = i.next();
        AbstractLocalCacheStoreValue item = getValueUnlockedFromCache(key);
        if (item != null) {
          i.remove();
          rv.put(key, item.getValueObject());
        }
      }
      if (keys.isEmpty()) {
        iterator.remove();
      }
    }

    // if everything was in local cache
    if (mapIdToKeysMap.isEmpty()) return rv;

    getAllValuesForKeyFromServer(mapIdToKeysMap, rv);

    return rv;
  }

  private AbstractLocalCacheStoreValue getValueUnlockedFromCache(Object key) {
    if (!isCacheInitialized()) { return null; }
    return this.cache.getLocalValue(key);
  }

  public void addStrongValueToCache(LockID lockId, Object key, Object value, ObjectID valueObjectID,
                                    MapOperationType mapOperation) {
    final LocalCacheStoreStrongValue localCacheValue = new LocalCacheStoreStrongValue(lockId, value, valueObjectID);
    addToCache(key, localCacheValue, valueObjectID, mapOperation);
  }

  public void addEventualValueToCache(Object key, Object value, ObjectID valueObjectID, MapOperationType mapOperation) {
    final LocalCacheStoreEventualValue localCacheValue = new LocalCacheStoreEventualValue(valueObjectID, value);
    addToCache(key, localCacheValue, valueObjectID, mapOperation);
  }

  public void addIncoherentValueToCache(Object key, Object value, ObjectID valueObjectID, MapOperationType mapOperation) {
    final LocalCacheStoreEventualValue localCacheValue = new LocalCacheStoreEventualValue(valueObjectID, value);
    addToCache(key, localCacheValue, valueObjectID, mapOperation);
  }

  private void addToCache(Object key, final AbstractLocalCacheStoreValue localCacheValue, ObjectID valueOid,
                          MapOperationType mapOperation) {
    Object value = localCacheValue.getValueObject();
    // if (value instanceof TCObjectSelf) {
    // ((TCObjectSelf) value).retain();
    // }
    try {
      boolean notifyServerForRemove = false;
      if (value instanceof TCObjectSelf) {
        if (localCacheEnabled || mapOperation.isMutateOperation()) {
          if (!this.tcObjectSelfStore.addTCObjectSelf(serverMapLocalStore, localCacheValue, value,
                                                      mapOperation.isMutateOperation())) { return; }
        } else {
          notifyServerForRemove = true;
        }
      }

      if (isCacheInitialized() && (localCacheEnabled || mapOperation.isMutateOperation())) {
        cache.addToCache(key, localCacheValue, mapOperation);
      }

      if (value instanceof TCObjectSelf) {
        this.tcObjectSelfStore.removeTCObjectSelfTemp((TCObjectSelf) value, notifyServerForRemove);
      }
      return;
    } finally {
      // if (value instanceof TCObjectSelf) {
      // ((TCObjectSelf) value).release();
      // }
    }
  }

  private Object getValueForKeyFromServer(final TCServerMap map, final Object key, final boolean retry) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException(
                                                                    "getValueForKeyInMap is not supported in a non-shared ServerMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    Object portableKey = getPortableKey(key);

    // If the key->value mapping is changed in some way during this lookup, it's possible that the value object
    // originally pointed to was deleted by DGC. If that happens we retry until we get a good value.
    long start = System.nanoTime();
    while (true) {
      final Object value = this.serverMapManager.getMappingForKey(mapID, portableKey);
      try {
        return lookupValue(value);
      } catch (TCObjectNotFoundException e) {
        if (!retry) {
          logger.warn("TCObjectNotFoundException for object " + value + " on a locked get. Returning null.");
          return null;
        }
        long timeWaited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (timeWaited > GET_VALUE_FOR_KEY_LOG_THRESHOLD) {
          logger.warn("Value for key: " + key + " still not found after " + timeWaited + "ms.");
          start = System.nanoTime();
        }
        ThreadUtil.reallySleep(RETRY_GET_VALUE_FOR_KEY_SLEEP);
      }
    }
  }

  private Object getPortableKey(final Object key) {
    Object portableKey = key;
    if (key instanceof Manageable) {
      final TCObject keyObject = ((Manageable) key).__tc_managed();
      if (keyObject == null) { throw new UnsupportedOperationException(
                                                                       "Key is portable, but not shared. This is currently not supported with TCObjectServerMap. Key = "
                                                                           + key); }
      portableKey = keyObject.getObjectID();
    }

    if (!LiteralValues.isLiteralInstance(portableKey)) {
      // formatter
      throw new UnsupportedOperationException(
                                              "Key is not portable. It needs to be a liternal or portable and shared for TCObjectServerMap. Key = "
                                                  + portableKey);
    }
    return portableKey;
  }

  private Object lookupValue(final Object value) throws TCObjectNotFoundException {
    if (value instanceof ObjectID) {
      try {
        return this.objectManager.lookupObjectQuiet((ObjectID) value);
      } catch (final ClassNotFoundException e) {
        logger.warn("Got ClassNotFoundException for objectId: " + value + ". Ignoring exception and returning null");
        return null;
      }
    } else {
      return value;
    }
  }

  private TCObjectServerMapImpl lookupTCObjectServerMapImpl(final ObjectID mapID) {
    try {
      return (TCObjectServerMapImpl) this.objectManager.lookup(mapID);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("ClassNotFoundException for mapID " + mapID);
    }
  }

  private Set<Object> getAllPortableKeys(final Set<Object> keys) {
    Set<Object> portableKeys = new HashSet<Object>();
    for (Object key : keys) {
      portableKeys.add(getPortableKey(key));
    }
    return portableKeys;
  }

  private void getAllValuesForKeyFromServer(final Map<ObjectID, Set<Object>> mapIdToKeysMap, Map<Object, Object> rv) {
    final Map<ObjectID, Set<Object>> mapIdsToLookup = new HashMap<ObjectID, Set<Object>>();
    for (Entry<ObjectID, Set<Object>> entry : mapIdToKeysMap.entrySet()) {
      // converting Map from <mapID, key> to <mapID, portableKey>
      mapIdsToLookup.put(entry.getKey(), getAllPortableKeys(entry.getValue()));
    }

    long start = System.nanoTime();
    while (!mapIdsToLookup.isEmpty()) {
      this.serverMapManager.getMappingForAllKeys(mapIdsToLookup, rv);

      for (Iterator<Entry<ObjectID, Set<Object>>> lookupIterator = mapIdsToLookup.entrySet().iterator(); lookupIterator
          .hasNext();) {
        Entry<ObjectID, Set<Object>> entry = lookupIterator.next();
        TCObjectServerMapImpl map = lookupTCObjectServerMapImpl(entry.getKey());
        Set<Object> portableKeys = entry.getValue();
        for (Iterator<Object> portableKeyIterator = portableKeys.iterator(); portableKeyIterator.hasNext();) {
          Object key = portableKeyIterator.next();
          Object value = rv.get(key);
          try {
            value = lookupValue(value);
          } catch (TCObjectNotFoundException e) {
            // We weren't able to find this particular mapping, continue for now, and try again on another pass
            continue;
          }
          portableKeyIterator.remove();

          // update the local cache of corresponding TCServerMap
          map.updateLocalCacheIfNecessary(key, value);
          rv.put(key, value);
        }

        // Remove the map from the remaining lookups when all its keys are accounted for.
        if (portableKeys.isEmpty()) {
          lookupIterator.remove();
        }
      }
      // Check if we have more lookups to do
      if (!mapIdsToLookup.isEmpty()) {
        long timeWaited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (timeWaited > GET_VALUE_FOR_KEY_LOG_THRESHOLD) {
          logger.warn("Still waiting for values after " + timeWaited + "ms.");
          start = System.nanoTime();
        }
        ThreadUtil.reallySleep(RETRY_GET_VALUE_FOR_KEY_SLEEP);
      }
    }
  }

  /**
   * Returns a snapshot of keys for the giver ServerTCMap
   * 
   * @param map ServerTCMap
   * @return set Set return snapshot of keys
   */

  @Override
  public Set keySet(final TCServerMap map) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException("keySet is not supported in a non-shared ServerMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    return this.serverMapManager.getAllKeys(mapID);
  }

  /**
   * Returns total size of an array of ServerTCMap.
   * <P>
   * The list of TCServerMaps passed in need not contain this TCServerMap, this is only a pass thru method that calls
   * getAllSize on the RemoteServerMapManager and is provided as a convenient way of batching the size calls at the
   * higher level.
   * 
   * @param maps ServerTCMap[]
   * @return long for size of map.
   */

  @Override
  public long getAllSize(final TCServerMap[] maps) {
    final ObjectID[] mapIDs = new ObjectID[maps.length];
    for (int i = 0; i < maps.length; ++i) {
      TCServerMap map = maps[i];
      final TCObject tcObject = map.__tc_managed();
      if (tcObject == null) { throw new UnsupportedOperationException(
                                                                      "getSize is not supported in a non-shared ServerMap"); }
      mapIDs[i] = tcObject.getObjectID();
    }
    return this.serverMapManager.getAllSize(mapIDs);
  }

  @Override
  public int getLocalSize() {
    if (!isCacheInitialized()) { return 0; }
    return this.cache.size();
  }

  /**
   * unpin all pinned keys
   */

  @Override
  public void unpinAll() {
    if (!isCacheInitialized()) { return; }
    cache.unpinAll();
  }

  /**
   * check the key is pinned or not
   */

  @Override
  public boolean isPinned(Object key) {
    if (!isCacheInitialized()) { return false; }
    return cache.isPinned(key);
  }

  /**
   * pin or unpin the key
   */

  @Override
  public void setPinned(Object key, boolean pinned) {
    if (!isCacheInitialized()) { return; }
    cache.setPinned(key, pinned);
  }

  private boolean isCacheInitialized() {
    if (this.cache == null) {
      logger.warn("Local cache yet not initialized");
      return false;
    }
    return true;
  }

  /**
   * Clears local cache of all entries. It is not immediate as all associated locks needs to be recalled.
   * 
   * @param map ServerTCMap
   */

  @Override
  public void clearLocalCache(final TCServerMap map) {
    lockAll();
    try {
      if (!isCacheInitialized()) { return; }
      this.cache.clear();
    } finally {
      unlockAll();
    }
  }

  @Override
  public void evictedInServer(Object key) {
    Lock lock = getLockForKey(key);
    lock.lock();
    try {
      // MNK-2875: Since server side eviction messages come in asynchronously with the initialization process, it's
      // possible to get eviction messages prior to the TCObjectServerMap being fully initialized. If that happens, we
      // can
      // just safely ignore any local cache removals since the local cache is uninitialized and thus empty.
      if (!isCacheInitialized()) { return; }
      this.cache.evictedInServer(key);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void clearAllLocalCacheInline(final TCServerMap map) {
    lockAll();
    try {
      if (!isCacheInitialized()) { return; }
      this.cache.clearInline();
    } finally {
      unlockAll();
    }
  }

  @Override
  protected boolean isEvictable() {
    return true;
  }

  /**
   * Called by the memory manager
   */

  @Override
  protected int clearReferences(final Object pojo, final int toClear) {
    // if this method is called, means there is a bug in the code, throwing AssertionError
    throw new AssertionError("clearReferences should not be called from L1 cache manager");
  }

  @Override
  public Set getLocalKeySet() {
    if (!isCacheInitialized()) { return Collections.EMPTY_SET; }
    return this.cache.getKeys();
  }

  @Override
  public boolean containsLocalKey(final Object key) {
    if (!isCacheInitialized()) { return false; }
    if (this.localCacheEnabled) {
      return this.cache.getLocalValue(key) != null;
    } else {
      return false;
    }
  }

  @Override
  public Object getValueFromLocalCache(final Object key) {
    if (!isCacheInitialized()) { return null; }
    AbstractLocalCacheStoreValue cachedItem = this.cache.getLocalValue(key);
    if (cachedItem != null) {
      return cachedItem.getValueObject();
    } else {
      return null;
    }
  }

  /**
   * Shares the entry and calls logicalPut.
   * 
   * @return ObjectID of the value
   */
  private ObjectID invokeLogicalPut(final TCServerMap map, final Object key, final Object value) {
    return invokeLogicalPutInternal(map, key, value, false);
  }

  /**
   * Shares the entry and calls logicalPutIfAbsent.
   * 
   * @return ObjectID of the value
   */
  private ObjectID invokeLogicalPutIfAbsent(final TCServerMap map, final Object key, final Object value) {
    return invokeLogicalPutInternal(map, key, value, true);
  }

  private ObjectID invokeLogicalPutInternal(TCServerMap map, Object key, Object value, boolean putIfAbsent) {
    final Object[] parameters = new Object[] { key, value };

    shareObject(key);
    ObjectID valueObjectID = shareObject(value);

    if (putIfAbsent) {
      logicalInvoke(SerializationUtil.PUT_IF_ABSENT, SerializationUtil.PUT_IF_ABSENT_SIGNATURE, parameters);
    } else {
      logicalInvoke(SerializationUtil.PUT, SerializationUtil.PUT_SIGNATURE, parameters);
    }
    return valueObjectID;
  }

  private ObjectID invokeLogicalReplace(final TCServerMap map, final Object key, final Object current,
                                        final Object newValue) {
    final Object[] parameters = new Object[] { key, current, newValue };

    shareObject(key);
    shareObject(current);
    ObjectID valueObjectID = shareObject(newValue);

    logicalInvoke(SerializationUtil.REPLACE_IF_VALUE_EQUAL, SerializationUtil.REPLACE_IF_VALUE_EQUAL_SIGNATURE,
                  parameters);
    return valueObjectID;
  }

  private ObjectID shareObject(Object param) {
    boolean isLiteral = LiteralValues.isLiteralInstance(param);
    if (!isLiteral) {
      TCObject object = this.objectManager.lookupOrCreate(param, this.groupID);
      return object.getObjectID();
    }
    return ObjectID.NULL_ID;
  }

  private void invokeLogicalRemove(final TCServerMap map, final Object key) {
    logicalInvoke(SerializationUtil.REMOVE, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { key });
  }

  private void invokeLogicalRemove(final TCServerMap map, final Object key, final Object value) {
    logicalInvoke(SerializationUtil.REMOVE_IF_VALUE_EQUAL, SerializationUtil.REMOVE_IF_VALUE_EQUAL_SIGNATURE,
                  new Object[] { key, value });
  }

  @Override
  public void addMetaData(MetaDataDescriptor mdd) {
    this.objectManager.getTransactionManager().addMetaDataDescriptor(this, (MetaDataDescriptorInternal) mdd);
  }

  @Override
  public void setupLocalStore(L1ServerMapLocalCacheStore serverMapLocalStore, PinnedEntryFaultCallback callback) {
    // this is called from CDSMDso.__tc_managed(tco)
    this.callback = callback;
    this.serverMapLocalStore = serverMapLocalStore;
    setupLocalCache(serverMapLocalStore, callback);
  }

  @Override
  public void destroyLocalStore() {
    this.serverMapLocalStore = null;
    this.cache = null;
  }

  private void setupLocalCache(L1ServerMapLocalCacheStore serverMapLocalStore, PinnedEntryFaultCallback callback) {
    this.cache = globalLocalCacheManager.getOrCreateLocalCache(this.objectID, objectManager, manager,
                                                               localCacheEnabled, serverMapLocalStore, callback);
  }

  @Override
  public long getLocalOnHeapSizeInBytes() {
    if (!isCacheInitialized()) { return 0; }
    return this.cache.onHeapSizeInBytes();
  }

  @Override
  public long getLocalOffHeapSizeInBytes() {
    if (!isCacheInitialized()) { return 0; }
    return this.cache.offHeapSizeInBytes();
  }

  @Override
  public int getLocalOnHeapSize() {
    if (!isCacheInitialized()) { return 0; }
    return this.cache.onHeapSize();
  }

  @Override
  public int getLocalOffHeapSize() {
    if (!isCacheInitialized()) { return 0; }
    return this.cache.offHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    if (!isCacheInitialized()) { return false; }
    return this.cache.containsKeyOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    if (!isCacheInitialized()) { return false; }
    return this.cache.containsKeyOffHeap(key);
  }

  @Override
  public void setLocalCacheEnabled(boolean enabled) {
    this.localCacheEnabled = enabled;
    this.cache.setLocalCacheEnabled(enabled);
  }

  @Override
  public void recalculateLocalCacheSize(Object key) {
    if (isCacheInitialized()) {
      this.cache.recalculateSize(key);
    }
  }

  @Override
  public void checkInObject(Object key, Object value) {
    this.cache.checkInObject(key, value);
  }

  @Override
  public Object checkOutObject(Object key, Object value) {
    return this.cache.checkOutObject(key, value);
  }

  private void lockAll() {
    for (Lock localLock : localLocks) {
      localLock.lock();
    }
  }

  private void unlockAll() {
    for (Lock localLock : localLocks) {
      localLock.unlock();
    }
  }

  private Lock getLockForKey(Object key) {
    Preconditions.checkNotNull(key, "Key cannot be null");
    return localLocks[Math.abs(spreadHash(key.hashCode()) % localLocks.length)];
  }

  private static int spreadHash(int h) {
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }
}
