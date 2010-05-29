/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class provides basic map operations like put,get, remove with the specified amount of concurrency. It doesn't do
 * various optimizations like {@link java.util.concurrent.ConcurrentHashMap} on reads so get might be slower than CHM
 * but is still striped so faster than Hashtable for concurrent usecases.
 * <p>
 * Where this class will excel is when you want to perform certain operation (using the callbacks) with in the lock for
 * that segment. for example things like these are simple.
 * <p>
 * <hr>
 * <blockquote>
 * 
 * <pre>
 * ArrayList list = new ArrayList();
 * list.add(value);
 * tcConcurrentStore.putIfAbsent(key, new ArrayList(), new Callback() {
 *   //TODO 
 * });
 * </pre>
 * 
 * </blockquote>
 * <p>
 * Someday this class could implement all the methods of {@link java.util.concurrent.ConcurrentMap}
 * <hr>
 */
public class TCConcurrentStore<K, V> {

  static final int              MAX_SEGMENTS             = 1 << 16;
  static final int              MAXIMUM_CAPACITY         = 1 << 30;
  static final float            DEFAULT_LOAD_FACTOR      = 0.75f;
  static final int              DEFAULT_INITIAL_CAPACITY = 256;
  static final int              DEFAULT_SEGMENTS         = 16;

  private final int             segmentShift;
  private final int             segmentMask;

  private final Segment<K, V>[] segments;

  public TCConcurrentStore(final int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_SEGMENTS);
  }

  public TCConcurrentStore() {
    this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_SEGMENTS);
  }

  public TCConcurrentStore(int initialCapacity, final float loadFactor, int concurrencyLevel) {
    if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0) { throw new IllegalArgumentException(); }

    if (concurrencyLevel > MAX_SEGMENTS) {
      concurrencyLevel = MAX_SEGMENTS;
    }

    // Find power-of-two sizes best matching arguments
    int sshift = 0;
    int ssize = 1;
    while (ssize < concurrencyLevel) {
      ++sshift;
      ssize <<= 1;
    }
    this.segmentShift = 32 - sshift;
    this.segmentMask = ssize - 1;

    this.segments = new Segment[ssize];

    if (initialCapacity > MAXIMUM_CAPACITY) {
      initialCapacity = MAXIMUM_CAPACITY;
    }
    int c = initialCapacity / ssize;
    if (c * ssize < initialCapacity) {
      ++c;
    }
    int cap = 1;
    while (cap < c) {
      cap <<= 1;
    }

    for (int i = 0; i < this.segments.length; ++i) {
      this.segments[i] = new Segment<K, V>(cap, loadFactor);
    }
  }

  /**
   * Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions. This
   * is critical because CachedItemStore uses power-of-two length hash tables, that otherwise encounter collisions for
   * hashCodes that do not differ in lower or upper bits.
   */
  private static int hash(int h) {
    // Spread bits to regularize both segment and index locations,
    // using variant of single-word Wang/Jenkins hash.
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  /**
   * Returns the segment that should be used for key with given hash
   * 
   * @param hash the hash code for the key
   * @return the segment
   */
  final Segment<K, V> segmentFor(Object key) {
    final int hash = hash(key.hashCode()); // throws NullPointerException if key null
    return this.segments[(hash >>> this.segmentShift) & this.segmentMask];
  }

  public V get(K key) {
    return segmentFor(key).get(key);
  }

  public V put(K key, V value) {
    return segmentFor(key).put(key, value);
  }

  public V putIfAbsent(K key, V value) {
    return segmentFor(key).putIfAbsent(key, value);
  }

  public V remove(K key) {
    return segmentFor(key).remove(key);
  }

  public interface TCConcurrentStoreCallback<K, V> {

    public void callback(K key, V newValue, V oldValue);

  }

  private static final class Segment<K, V> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final HashMap<K, V>          map;

    public Segment(int initialCapacity, float loadFactor) {
      this.map = new HashMap<K, V>(initialCapacity, loadFactor);
    }

    public V get(K key) {
      this.lock.readLock().lock();
      try {
        return this.map.get(key);
      } finally {
        this.lock.readLock().unlock();
      }
    }

    public V put(K key, V value) {
      this.lock.writeLock().lock();
      try {
        return this.map.put(key, value);
      } finally {
        this.lock.writeLock().unlock();
      }
    }

    public V putIfAbsent(K key, V value) {
      this.lock.writeLock().lock();
      try {
        if (!this.map.containsKey(key)) {
          return this.map.put(key, value);
        } else {
          return this.map.get(key);
        }
      } finally {
        this.lock.writeLock().unlock();
      }
    }

    public V remove(K key) {
      this.lock.writeLock().lock();
      try {
        return this.map.remove(key);
      } finally {
        this.lock.writeLock().unlock();
      }
    }

  }

}
