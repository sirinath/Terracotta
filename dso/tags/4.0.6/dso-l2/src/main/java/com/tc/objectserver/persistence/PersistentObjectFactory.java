package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.util.Conversion;
import com.tc.util.concurrent.ConcurrentHashMap;
import com.tc.util.concurrent.ThreadUtil;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author tim
 */
public class PersistentObjectFactory {
  private static final TCLogger LOGGER = TCLogging.getLogger(PersistentObjectFactory.class);

  private static final KeyValueStorageConfig<Object, Object> MAP_CONFIG = ImmutableKeyValueStorageConfig.builder(Object.class, Object.class)
      .keyTransformer(LiteralSerializer.INSTANCE)
      .valueTransformer(LiteralSerializer.INSTANCE)
      .concurrency(1).build();

  private final StorageManager storageManager;
  private final KeyValueStorageConfig<Object, Object> defaultConfig;

  private final Map<ObjectID, KeyValueStorage> storages = new ConcurrentHashMap<ObjectID, KeyValueStorage>();

  public PersistentObjectFactory(final StorageManager storageManager, final StorageManagerFactory storageManagerFactory) {
    this.storageManager = storageManager;
    defaultConfig = storageManagerFactory.wrapMapConfig(MAP_CONFIG);
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          long reservedSpace = 0;
          try {
            for (KeyValueStorage keyValueStorage : storages.values()) {
              Field backingField = keyValueStorage.getClass().getDeclaredField("backing");
              backingField.setAccessible(true);
              Object backing = backingField.get(keyValueStorage);
              reservedSpace += (Long) backing.getClass().getMethod("getDataAllocatedMemory").invoke(backing);
            }
            LOGGER.info("Total map reserved space " + Conversion.memoryBytesAsSize(reservedSpace));
            ThreadUtil.reallySleep(1000);
          } catch (Exception e) {
          }
        }
      }
    });
    t.setDaemon(true);
    t.start();
  }

  public synchronized KeyValueStorage<Object, Object> getKeyValueStorage(ObjectID objectID, final boolean create) throws ObjectNotFoundException {
    KeyValueStorage<Object, Object> map = storageManager.getKeyValueStorage(objectID.toString(), Object.class, Object.class);
    if (map == null) {
      if (create) {
        map = storageManager.createKeyValueStorage(objectID.toString(), defaultConfig);
      } else {
        throw new ObjectNotFoundException("Map for object id " + objectID + " not found.");
      }
    }
    storages.put(objectID, map);
    return map;
  }

  public synchronized void destroyKeyValueStorage(ObjectID oid) {
    storageManager.destroyKeyValueStorage(oid.toString());
  }
}
