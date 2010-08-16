package com.tc.objectserver.storage.nativeCache.api;

import org.terracotta.nativecache.buffersource.BufferSource;
import org.terracotta.nativecache.storage.NativeBufferStorageEngine;
import org.terracotta.nativecache.storage.StorageEngine;
import org.terracotta.nativecache.storage.StorageEngineFactory;
import org.terracotta.nativecache.storage.allocator.Allocator;
import org.terracotta.nativecache.storage.allocator.BestFitAllocator;
import org.terracotta.nativecache.storage.portability.Portability;

public class MyStorageEngine<K, V> extends NativeBufferStorageEngine<K, V> {

  public static <K, V> StorageEngineFactory<K, V> createFactory(BufferSource source, int initialSize,
                                                                Portability<K> keyProtability,
                                                                Portability<V> valueProtability) {
    Factory<K, V> factory = new Factory<K, V>(source, initialSize, keyProtability, valueProtability);
    return factory;
  }

  protected MyStorageEngine(Allocator allocator, Portability<K> keyProtability, Portability<V> valueProtability) {
    super(allocator, keyProtability, valueProtability);
  }

  static class Factory<K, V> implements StorageEngineFactory<K, V> {

    private final BufferSource   source;
    private final int            initialSize;
    private final Portability<K> keyProtability;
    private final Portability<V> valueProtability;

    Factory(BufferSource source, int initialSize, Portability<K> keyProtability, Portability<V> valueProtability) {
      this.source = source;
      this.initialSize = initialSize;
      this.keyProtability = keyProtability;
      this.valueProtability = valueProtability;
    }

    public StorageEngine<K, V> newInstance() {
      return new MyStorageEngine<K, V>(new BestFitAllocator(source, initialSize), keyProtability, valueProtability);
    }

  }
}
