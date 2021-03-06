/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

public interface MemoryUsage {

  public long getFreeMemory();

  public String getDescription();

  /* If -Xmx flag is not specified, this might not be correct or consistent over time */
  public long getMaxMemory();

  public long getUsedMemory();

  public int getUsedPercentage();

  /**
   * @return - the number of times GC was executed (on this memory pool, if the usage is for a specific memory pool)
   *         since the beginning. -1 if this is not supported.
   */
  public long getCollectionCount();
}
