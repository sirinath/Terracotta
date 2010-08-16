/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.nativeCache;

import com.tc.properties.TCPropertiesImpl;

public class NativeCacheConfig {

  private static final int  MEGABYTE    = 1024 * 0124;
  private static final long GIGABYTE    = 1024 * 1024 * 1024;

  private final long        maxDataSize = TCPropertiesImpl.getProperties().getLong("nCache.maxDataSize", 1 * GIGABYTE);
  private final int         segments    = TCPropertiesImpl.getProperties().getInt("nCache.segments", 256);
  private final boolean     enabled     = TCPropertiesImpl.getProperties().getBoolean("nCache.enabled", true);

  public int getInitialDataSize() {
    return 1 * MEGABYTE;
  }

  public long getMaxDataSize() {
    return maxDataSize;
  }

  public int getSegments() {
    return segments;
  }

  public boolean enabled() {
    return enabled;
  }
}
