/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.stats.counter.sampled.TimeStampedCounterValue;

public interface ObjectManagerStats {

  double getCacheHitRatio();
  
  TimeStampedCounterValue getCacheMissRate();
  
  long getTotalRequests();

  long getTotalCacheMisses();

  long getTotalCacheHits();
  
  long getTotalObjectsCreated();

}
