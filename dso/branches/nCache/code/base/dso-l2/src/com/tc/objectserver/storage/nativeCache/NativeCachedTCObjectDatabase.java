/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.nativeCache;

import org.terracotta.nativecache.storage.portability.ByteArrayPortability;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.api.TCObjectDatabase;
import com.tc.objectserver.storage.nativeCache.api.LongPortability;
import com.tc.objectserver.storage.nativeCache.api.OffheapStorage;
import com.tc.util.ObjectIDSet;

public class NativeCachedTCObjectDatabase implements TCObjectDatabase {

  private static final TCLogger              logger = TCLogging.getLogger(NativeCachedTCObjectDatabase.class);

  private final TCObjectDatabase             objectDatabase;
  private final OffheapStorage<Long, byte[]> offheapCache;

  public NativeCachedTCObjectDatabase(final NativeCacheConfig cacheConfig, final TCObjectDatabase objectDatabase) {
    this.objectDatabase = objectDatabase;
    this.offheapCache = new OffheapStorage<Long, byte[]>(cacheConfig.getInitialDataSize(),
                                                         cacheConfig.getMaxDataSize(), cacheConfig.getSegments(),
                                                         new LongPortability(), new ByteArrayPortability());
    logger.info("XXX Using Native Cache for Object Database");
  }

  public Status insert(long id, byte[] b, PersistenceTransaction tx) {
    Status status = objectDatabase.insert(id, b, tx);
    if (status == Status.SUCCESS) {
      offheapCache.put(id, b, tx);
    }
    return status;
  }

  public Status update(long id, byte[] b, PersistenceTransaction tx) {
    Status status = objectDatabase.update(id, b, tx);
    if (status == Status.SUCCESS) {
      offheapCache.put(id, b, tx);
    }
    return status;
  }

  public byte[] get(long id, PersistenceTransaction tx) {
    byte[] rv = offheapCache.get(id, tx);
    if (rv == null) {
      return objectDatabase.get(id, tx);
    } else {
      return rv;
    }
  }

  public Status delete(long id, PersistenceTransaction tx) {
    Status status = objectDatabase.delete(id, tx);
    if (status == Status.SUCCESS) {
      offheapCache.delete(id, tx);
    }
    return status;
  }

  public ObjectIDSet getAllObjectIds(PersistenceTransaction tx) {
    // can this be served from cache
    return objectDatabase.getAllObjectIds(tx);
  }

}
