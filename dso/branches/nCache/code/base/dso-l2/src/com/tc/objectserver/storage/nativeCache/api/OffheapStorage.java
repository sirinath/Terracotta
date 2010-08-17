package com.tc.objectserver.storage.nativeCache.api;

import org.terracotta.nativecache.buffersource.BufferSource;
import org.terracotta.nativecache.buffersource.UnlimitedBufferSource;
import org.terracotta.nativecache.buffersource.UpfrontAllocatingBufferSource;
import org.terracotta.nativecache.concurrent.ConcurrentNativeClockCache;
import org.terracotta.nativecache.storage.StorageEngineFactory;
import org.terracotta.nativecache.storage.portability.Portability;

import com.tc.objectserver.storage.api.PersistenceTransaction;

public class OffheapStorage<K, V> implements Storage<K, V> {
  private static final int                       MAX_SEGMENTS = 1 << 16; // slightly conservative

  private final ConcurrentNativeClockCache<K, V> cache;

  public OffheapStorage(int initialDataSize, long maximalDataSize, int segments, Portability<K> keyProtability,
                        Portability<V> valueProtability) {
    long chunkSize = Math.min(Integer.MAX_VALUE, Long.highestOneBit(maximalDataSize / segments) << 1);
    BufferSource source = new UpfrontAllocatingBufferSource(new UnlimitedBufferSource(), maximalDataSize,
                                                            (int) chunkSize);
    StorageEngineFactory<K, V> factory = MyStorageEngine.createFactory(source, initialDataSize, keyProtability,
                                                                       valueProtability);
    this.cache = new ConcurrentNativeClockCache<K, V>(source, factory);
  }

  public OffheapStorage(int initialDataSize, long maximalDataSize, int tableSize, int concurrency,
                        Portability<K> keyProtability, Portability<V> valueProtability) {
    int segments = calculateSegments(concurrency);
    long chunkSize = Math.min(Integer.MAX_VALUE, Long.highestOneBit(maximalDataSize / segments) << 1);
    BufferSource source = new UpfrontAllocatingBufferSource(new UnlimitedBufferSource(), maximalDataSize,
                                                            (int) chunkSize);
    StorageEngineFactory<K, V> factory = MyStorageEngine.createFactory(source, initialDataSize, keyProtability,
                                                                       valueProtability);
    this.cache = new ConcurrentNativeClockCache<K, V>(source, factory, tableSize, concurrency);
  }

  private int calculateSegments(int concurrency) {
    if (concurrency > MAX_SEGMENTS) {
      concurrency = MAX_SEGMENTS;
    }

    // Find power-of-two sizes best matching arguments
    int sshift = 0;
    int ssize = 1;
    while (ssize < concurrency) {
      ++sshift;
      ssize <<= 1;
    }
    return ssize;
  }

  public boolean delete(K id, PersistenceTransaction tx) {
    cache.remove(id);
    return true;
  }

  public V get(K id, PersistenceTransaction tx) {
    return cache.get(id);
  }

  public boolean put(K id, V value, PersistenceTransaction tx) {
    cache.put(id, value);
    return true;
  }

  public void clear() {
    cache.clear();
  }

  public long getMemorySize() {
    return cache.memorySize();
  }

  public long getSize() {
    return cache.size();
  }
}
