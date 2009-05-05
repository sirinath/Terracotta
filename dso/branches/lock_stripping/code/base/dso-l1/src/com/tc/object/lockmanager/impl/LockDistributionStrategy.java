package com.tc.object.lockmanager.impl;

import com.tc.net.GroupID;
import com.tc.net.OrderedGroupIDs;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

public class LockDistributionStrategy {
  private final GroupID[] groupIDs;
  private final int       segmentShift;
  private final int       segmentMask;

  public LockDistributionStrategy(OrderedGroupIDs orderedGroupIDs) {
    this.groupIDs = orderedGroupIDs.getGroupIDs();

    int groupCount = this.groupIDs.length;
    int sshift = 0;
    int ssize = 1;
    while (ssize < groupCount) {
      ++sshift;
      ssize <<= 1;
    }
    this.segmentShift = 32 - sshift;
    this.segmentMask = ssize - 1;

  }

  public GroupID getGroupIdForLock(String lockID) {
    int hash = hash(lockID.hashCode());
    return this.groupIDs[(hash >>> this.segmentShift) & this.segmentMask];
  }

  /**
   * Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions.
   */
  private static int hash(int h) {
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }
}
