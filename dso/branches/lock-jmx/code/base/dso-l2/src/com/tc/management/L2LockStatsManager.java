/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.Sink;
import com.tc.management.lock.stats.LockSpec;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.LockManager;

import java.util.Collection;
import java.util.Collections;

public interface L2LockStatsManager {
  public final static L2LockStatsManager NULL_LOCK_STATS_MANAGER = new L2LockStatsManager() {
    public void start(DSOChannelManager channelManager, LockManager lockManager, Sink sink) {
      // do nothing
    }
    
    public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
      // do nothing
    }
    
    public boolean isClientStatEnabled() {
      return false;
    }
    
    public void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID) {
      // do nothing
    }
    
    public void recordLockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy, long lockAwardTimestamp) {
      // do nothing
    }
    
    public void recordLockReleased(LockID lockID, NodeID nodeID, ThreadID threadID) {
      // do nothing
    }
    
    public void recordLockRejected(LockID lockID, NodeID nodeID, ThreadID threadID) {
      // do nothing
    }
    
    public long getNumberOfLockRequested(LockID lockID) {
      return 0;
    }
    
    public long getNumberOfLockReleased(LockID lockID) {
      return 0;
    }
    
    public long getNumberOfPendingRequests(LockID lockID) {
      return 0;
    }
    
    public long getNumberOfLockHopRequests(LockID lockID) {
      return 0;
    }
    
    public void recordClientStat(LockID lockID, NodeID nodeID, Collection lockStatElements) {
      // do nothing
    }
    
    public boolean isClientStatEnabled(NodeID nodeID) {
      return false;
    }

    public void recordClientStatEnabled(NodeID nodeID) {
      // do nothing
    }

    public int getTraceDepth() {
      return 0;
    }

    public int getGatherInterval() {
      return 0;
    }
    
    public void setLockStatisticsEnabled(boolean lockStatsEnabled) {
      // do nothing
    }

    public boolean isLockStatisticsEnabled() {
      return false;
    }
    
    public void clearAllStatsFor(NodeID nodeID) {
      //
    }

    public void recordLockHopRequested(LockID lockID) {
      //
    }
    
    public Collection<LockSpec> getLockSpecs() {
      return Collections.EMPTY_LIST;
    }
    
  };
  
  public void start(DSOChannelManager channelManager, LockManager lockManager, Sink sink);
  
  public void setLockStatisticsConfig(int traceDepth, int gatherInterval);
  
  public boolean isClientStatEnabled();
  
  public boolean isClientStatEnabled(NodeID nodeID);
  
  public void recordClientStatEnabled(NodeID nodeID);
  
  public void recordLockHopRequested(LockID lockID);
  
  public void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID);
  
  public void recordLockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy, long lockAwardTimestamp);
  
  public void recordLockReleased(LockID lockID, NodeID nodeID, ThreadID threadID);
  
  public void recordLockRejected(LockID lockID, NodeID nodeID, ThreadID threadID);
  
  public void recordClientStat(LockID lockID, NodeID nodeID, Collection lockStatElements);
  
  public long getNumberOfLockRequested(LockID lockID);
  
  public long getNumberOfLockReleased(LockID lockID);
  
  public long getNumberOfPendingRequests(LockID lockID);
  
  public long getNumberOfLockHopRequests(LockID lockID);
  
  public Collection<LockSpec> getLockSpecs();
  
  public int getTraceDepth();
  
  public int getGatherInterval();
  
  public void setLockStatisticsEnabled(boolean lockStatsEnabled);

  public boolean isLockStatisticsEnabled();
  
  public void clearAllStatsFor(NodeID nodeID);
}
