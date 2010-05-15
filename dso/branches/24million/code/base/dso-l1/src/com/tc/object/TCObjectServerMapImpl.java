/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCClassNotFoundException;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.cache.CachedItem;
import com.tc.object.locks.LockID;

import java.util.concurrent.ConcurrentHashMap;

public class TCObjectServerMapImpl extends TCObjectLogical implements TCObject, TCObjectServerMap {

  private final ClientObjectManager                   objectManager;
  private final RemoteServerMapManager                serverMapManager;
  private final ConcurrentHashMap<Object, CachedItem> cache = new ConcurrentHashMap<Object, CachedItem>();

  public TCObjectServerMapImpl(final ClientObjectManager objectManager, final RemoteServerMapManager serverMapManager,
                               final ObjectID id, final Object peer, final TCClass tcc, final boolean isNew) {
    super(id, peer, tcc, isNew);
    this.objectManager = objectManager;
    this.serverMapManager = serverMapManager;
  }

  /**
   * Does a logical put and updates the local cache
   * 
   * @param map ServerTCMap
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPut(final TCServerMap map, final String lockID, final Object key, final Object value) {
    addToCache(lockID, key, value);
    ManagerUtil.logicalInvoke(map, SerializationUtil.PUT_SIGNATURE, new Object[] { key, value });
  }

  /**
   * Does a logical put but doesn't add it to the local cache, old cache entries could be cleared
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is added
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalPutButDontCache(final TCServerMap map, final Object key, final Object value) {
    // NOTE:: Even though the local cache is cleared, you could still race to see the new value if you read immediately,
    // which is the same for all nodes.
    removeFromCache(key);
    ManagerUtil.logicalInvoke(map, SerializationUtil.PUT_SIGNATURE, new Object[] { key, value });
  }

  /**
   * Does a logic remove and removes from the local cache if present
   * 
   * @param map ServerTCMap
   * @param lockID, lock under which this entry is removed
   * @param key Key Object
   * @param value Object in the mapping
   */
  public void doLogicalRemove(final TCServerMap map, final Object key) {
    removeFromCache(key);
    ManagerUtil.logicalInvoke(map, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { key });
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param pojo Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValue(final TCServerMap map, final String lockID, final Object key) {

    final CachedItem item = getFromCache(key);
    if (item != null) { return item.getValue(); }

    final Object value = getValueForKeyFromServer(map, key);
    addToCache(lockID, key, value);

    return value;
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap but doesn't cache the value locally
   * 
   * @param pojo Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValueButDontCache(final TCServerMap map, final Object key) {
    final CachedItem item = getFromCache(key);
    if (item != null) { return item.getValue(); }

    return getValueForKeyFromServer(map, key);
  }

  private Object getValueForKeyFromServer(final TCServerMap map, final Object key) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException(
                                                                    "getValueForKeyInMap is not supported in a non-shared ServerTCMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    Object portableKey = key;
    if (key instanceof Manageable) {
      final TCObject keyObject = ((Manageable) key).__tc_managed();
      if (keyObject == null) { throw new UnsupportedOperationException(
                                                                       "Key is portable, but not shared. This is currently not supported with ServerTCMap. Map ID = "
                                                                           + mapID + " key = " + key); }
      portableKey = keyObject.getObjectID();
    }

    if (!LiteralValues.isLiteralInstance(portableKey)) {
      // formatter
      throw new UnsupportedOperationException(
                                              "Key is not portable. It needs to be a liternal or portable and shared for ServerTCMap. Key = "
                                                  + portableKey + " map id = " + mapID);
    }

    final Object value = this.serverMapManager.getMappingForKey(mapID, portableKey);

    if (value instanceof ObjectID) {
      try {
        return this.objectManager.lookupObject((ObjectID) value);
      } catch (final ClassNotFoundException e) {
        throw new TCClassNotFoundException(e);
      }
    } else {
      return value;
    }
  }

  public int getSize(final TCServerMap map) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException(
                                                                    "getSize is not supported in a non-shared ServerTCMap"); }
    final ObjectID mapID = tcObject.getObjectID();

    return this.serverMapManager.getSize(mapID);
  }

  /* Local Cache operations */

  // These cache operations should be performed under the lock "lockID" elsewhere. So there is no race between adding in
  // local cache and serverMapManager.
  private void addToCache(final String lockID, final Object key, final Object value) {
    final LockID lock = ManagerUtil.getManager().generateLockIdentifier(lockID);
    final CachedItem item = new CachedItem(this.cache, lock, key, value);
    this.cache.put(key, item);
    this.serverMapManager.addCachedItemForLock(lock, item);
  }

  private void removeFromCache(final Object key) {
    final CachedItem removed = this.cache.remove(key);
    if (removed != null) {
      this.serverMapManager.removeCachedItemForLock(removed.getLockID(), removed);
    }
  }

  private CachedItem getFromCache(final Object key) {
    return this.cache.get(key);
  }

}
