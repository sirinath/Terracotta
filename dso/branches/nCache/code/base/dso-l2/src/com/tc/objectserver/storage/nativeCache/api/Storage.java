package com.tc.objectserver.storage.nativeCache.api;

import com.tc.objectserver.storage.api.PersistenceTransaction;

public interface Storage<K, V> {
	public boolean put(K id, V value, PersistenceTransaction tx);

	public V get(K id, PersistenceTransaction tx);

	public boolean delete(K id, PersistenceTransaction tx);

	public void clear();
}
