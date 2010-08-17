/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.nativeCache;

import com.tc.properties.TCPropertiesImpl;

public class NativeCacheConfig {

  private static final int  KILOBYTE    = 1024;
  private static final int  MEGABYTE    = 1024 * 1024;
  private static final long GIGABYTE    = 1024 * 1024 * 1024;

  private final boolean     enabled     = TCPropertiesImpl.getProperties().getBoolean("nCache.enabled", true);
  private final long        maxDataSize = TCPropertiesImpl.getProperties().getLong("nCache.maxDataSize", 1 * GIGABYTE);
  private final int         tableSize   = TCPropertiesImpl.getProperties().getInt("nCache.tableSize", 4 * MEGABYTE);
  private final int         concurrency = TCPropertiesImpl.getProperties().getInt("nCache.concurrency", 2 * KILOBYTE);

  public int getInitialDataSize() {
    return 1 * MEGABYTE;
  }

  public long getMaxDataSize() {
    return maxDataSize;
  }

  public int getTableSize() {
    return tableSize;
  }

  public int getConcurrency() {
    return concurrency;
  }

  public boolean enabled() {
    return enabled;
  }
}
