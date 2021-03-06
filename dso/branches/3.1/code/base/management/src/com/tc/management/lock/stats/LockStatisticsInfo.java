/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.Collection;

public interface LockStatisticsInfo {
  public void recordLockRequested(NodeID nodeID, ThreadID threadID, long requestTimeInMillis, int numberOfPendingRequests,
                                  StackTraceElement[] stackTraces, String contextInfo);

  public boolean recordLockAwarded(NodeID nodeID, ThreadID threadID, boolean isGreedy, long awardedTimeInMillis,
                                   int nestedLockDepth);

  public void recordLockRejected(NodeID nodeID, ThreadID threadID);

  public boolean recordLockReleased(NodeID nodeID, ThreadID threadID);
  
  public long getNumberOfLockRequested();

  public long getNumberOfLockReleased();

  public long getNumberOfLockHopRequested();

  public long getNumberOfPendingRequests();

  public boolean hasChildren();
  
  public Collection children();

  public LockStatElement getLockStatElement();

  public void aggregateLockHoldersData();
}
