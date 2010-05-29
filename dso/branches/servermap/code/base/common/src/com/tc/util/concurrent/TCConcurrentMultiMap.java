/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.util.concurrent.TCConcurrentStore.TCConcurrentStoreCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A concurrent implementation of a MultiMap (one to many mapping) with configurable concurrency level. Basic methods
 * are implemented. Could one day implement all map interfaces.
 * 
 * @author Saravanan Subbiah
 */
public class TCConcurrentMultiMap<K, V> {

  private final TCConcurrentStore<K, List<V>> store;
  private final AddCallBack                   callback = new AddCallBack();

  /**
   * Creates a Multimap with a default initial capacity (16), load factor (0.75) and concurrencyLevel (16).
   */
  public TCConcurrentMultiMap() {
    this.store = new TCConcurrentStore<K, List<V>>();
  }

  /**
   * Creates a Multimap with the specified initial capacity, and with default load factor (0.75) and concurrencyLevel
   * (16).
   * 
   * @param initialCapacity the initial capacity.
   * @throws IllegalArgumentException if the initial capacity of elements is negative.
   */
  public TCConcurrentMultiMap(final int initialCapacity) {
    this.store = new TCConcurrentStore<K, List<V>>(initialCapacity);
  }

  /**
   * Creates a Multimap with the specified initial capacity and load factor and with the default concurrencyLevel (16).
   * 
   * @param initialCapacity the initial capacity.
   * @param loadFactor the load factor threshold, used to control resizing.
   * @throws IllegalArgumentException if the initial capacity of elements is negative or the load factor is non-positive
   */
  public TCConcurrentMultiMap(final int initialCapacity, final float loadFactor) {
    this.store = new TCConcurrentStore<K, List<V>>(initialCapacity, loadFactor);
  }

  /**
   * Creates a Multimap with the specified initial capacity, load factor and concurrency level.
   * 
   * @param initialCapacity the initial capacity.
   * @param loadFactor the load factor threshold, used to control resizing.
   * @param concurrencyLevel the estimated number of concurrently updating threads.
   * @throws IllegalArgumentException if the initial capacity is negative or the load factor or concurrencyLevel are
   *         non-positive.
   */
  public TCConcurrentMultiMap(final int initialCapacity, final float loadFactor, final int concurrencyLevel) {
    this.store = new TCConcurrentStore<K, List<V>>(initialCapacity, loadFactor, concurrencyLevel);
  }

  /**
   * Adds a mapping of key to value to the Multimap. If there already exists a mapping for key, that mapping is still
   * retained while adding the new mapping. Also the mapping is added if there exists a mapping for key to value.
   * 
   * @return true, if this is the first mapping for key in this Multimap at this point in time, else false
   * @throws NullPointerException if key or value is null
   */
  public boolean add(final K key, final V value) {
    return (Boolean) this.store.executeUnderWriteLock(key, value, this.callback);
  }

  /**
   * Removes all the mapping for the key and returns as a List. If there are no mapping present for the key, returns an
   * empty list.
   * 
   * @return list of mappings for key
   * @throws NullPointerException if key is null
   */
  public List<V> removeAll(final K key) {
    final List<V> list = this.store.remove(key);
    if (list == null) { return Collections.EMPTY_LIST; }
    return list;
  }

  /**
   * Returns all the mapping for the key as an immutable List. If there are no mapping present for the key, returns an
   * empty list. Note that even though the returned list is immutable, the list is backed by the mappings in the
   * Multimap, so iterating the returned list while there are concurrent operations for the same key will produce
   * undetermined results.
   * 
   * @return list of mappings for key
   * @throws NullPointerException if key is null
   */
  public List<V> get(final K key) {
    final List<V> list = this.store.get(key);
    if (list == null) { return Collections.EMPTY_LIST; }
    return Collections.unmodifiableList(list);
  }

  private final class AddCallBack implements TCConcurrentStoreCallback<K, List<V>> {
    // Called under segment lock
    public Object callback(final K key, final Object value, final Map<K, List<V>> segment) {
      boolean newEntry = false;
      List<V> list = segment.get(key);
      if (list == null) {
        list = new ArrayList<V>();
        segment.put(key, list);
        newEntry = true;
      }
      list.add((V) value);
      return newEntry;
    }
  }
}
