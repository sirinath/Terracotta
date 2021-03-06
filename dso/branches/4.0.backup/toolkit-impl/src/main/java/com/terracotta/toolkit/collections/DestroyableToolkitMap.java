/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.toolkit.collections.map.SubTypeWrapperCollection;
import com.terracotta.toolkit.collections.map.SubTypeWrapperSet;
import com.terracotta.toolkit.collections.map.ToolkitMapImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitMap;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;
import com.terracotta.toolkit.util.ToolkitSubtypeStatusImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class DestroyableToolkitMap<K, V> extends AbstractDestroyableToolkitObject<ToolkitMap> implements
    ToolkitMap<K, V>, RejoinAwareToolkitMap<K, V> {

  private final String                                        name;
  private volatile ToolkitMap<K, V>                           map;
  private final IsolatedClusteredObjectLookup<ToolkitMapImpl> lookup;
  private final ToolkitSubtypeStatusImpl                      status;

  public DestroyableToolkitMap(ToolkitObjectFactory<ToolkitMap> factory,
                               IsolatedClusteredObjectLookup<ToolkitMapImpl> lookup, ToolkitMapImpl<K, V> map,
                               String name) {
    super(factory);
    this.lookup = lookup;
    this.map = map;
    this.name = name;
    status = new ToolkitSubtypeStatusImpl();
    map.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void doRejoinStarted() {
    this.map = ToolkitInstanceProxy.newRejoinInProgressProxy(name, ToolkitMap.class);
    status.incrementRejoinCount();
  }

  @Override
  public void doRejoinCompleted() {
    if (!isDestroyed()) {
      ToolkitMapImpl afterRejoin = lookup.lookupClusteredObject(name, ToolkitObjectType.MAP, null);
      if (afterRejoin == null) {
        destroyApplicator.applyDestroy();
      } else {
        this.map = afterRejoin;
      }
    }
  }

  @Override
  public void applyDestroy() {
    this.map = ToolkitInstanceProxy.newDestroyedInstanceProxy(name, ToolkitMap.class);
  }

  @Override
  public void doDestroy() {
    map.destroy();
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return map.getReadWriteLock();
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return map.get(key);
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    return new SubTypeWrapperSet<K>(map.keySet(), status, this.name, ToolkitObjectType.MAP);
  }

  @Override
  public Collection<V> values() {
    return new SubTypeWrapperCollection<V>(map.values(), status, this.name, ToolkitObjectType.MAP);
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new SubTypeWrapperSet<Entry<K, V>>(map.entrySet(), status, this.name, ToolkitObjectType.MAP);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return map.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(Object arg0, Object arg1) {
    return map.remove(arg0, arg1);
  }

  @Override
  public V replace(K key, V value) {
    return map.replace(key, value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return map.replace(key, oldValue, newValue);
  }

}
