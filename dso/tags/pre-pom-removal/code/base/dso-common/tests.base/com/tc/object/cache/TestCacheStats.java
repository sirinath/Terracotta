/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import com.tc.util.Assert;
import com.tc.util.State;

import java.util.List;

public class TestCacheStats implements CacheStats {

  private static final State INIT       = new State("INIT");
  private static final State PROCESSING = new State("PROCESSING");
  private static final State COMPLETE   = new State("COMPLETE");

  public int                 countBefore;
  public int                 toKeep;
  public int                 evicted;
  public List                objectsEvicted;
  public int                 countAfter;
  private State              state      = INIT;

  public int getObjectCountToEvict(int currentCount) {
    this.countBefore = currentCount;
    int toEvict = currentCount - toKeep;
    if (toEvict > 0) {
      state = PROCESSING;
    }
    return toEvict;
  }

  public void objectEvicted(int evictedCount, int currentCount, List targetObjects4GC) {
    this.evicted = evictedCount;
    this.countAfter = currentCount;
    this.objectsEvicted = targetObjects4GC;
    state = COMPLETE;
  }

  public void validate() {
    Assert.assertTrue(state != PROCESSING);
  }

}
