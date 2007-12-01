/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.async.api.Sink;
import com.tc.management.L2LockStatsManager;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class L2LockStatisticsManagerImpl extends LockStatisticsManager implements L2LockStatsManager, Serializable {
  private final static int  TOP_N              = 100;

  private DSOChannelManager channelManager;                            // data is guarded by "this"
  private LockManager       lockManager;
  private Sink              sink;
  private final Set<NodeID> statEnabledClients = new HashSet<NodeID>();

  private final int         topN;

  public L2LockStatisticsManagerImpl() {
    TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("lock.statistics");
    if (tcProperties == null) {
      this.lockStatisticsEnabled = false;
      this.topN = TOP_N;
    } else {
      if (tcProperties.getProperty("enabled") == null) {
        this.lockStatisticsEnabled = false;
      } else {
        this.lockStatisticsEnabled = tcProperties.getBoolean("enabled");
      }
      this.topN = tcProperties.getInt("max", TOP_N);
    }
    // this.holderStats = new LockHolderStats(topN);
  }

  public synchronized void start(DSOChannelManager channelManager, LockManager lockManager, Sink sink) {
    this.lockManager = lockManager;
    this.channelManager = channelManager;
    this.sink = sink;
  }

  /**
   * Abstract method implementation section begin
   */
  protected LockStatisticsInfo newLockStatisticsContext(LockID lockID) {
    return new ServerLockStatisticsInfoImpl(lockID);
  }

  protected void disableLockStatistics() {
    this.lockStatisticsEnabled = false;

    setLockStatisticsConfig(0, lockStatConfig.getGatherInterval());
    clear();
  }

  /**
   * Abstract method implementation section ends
   */

  // We cannot synchronized on the whole method in order to prevent deadlock.
  public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    Set<LockID> keysSet = null;

    synchronized (this) {
      if (!lockStatisticsEnabled) { return; }
      super.setLockStatisticsConfig(traceDepth, gatherInterval);
      keysSet = lockStats.keySet();
    }

    // TODO: Instead of loop through each lockID, loop through each node
    for (Iterator<LockID> i = keysSet.iterator(); i.hasNext();) {
      LockID lockID = i.next();
      if (traceDepth > MIN_CLIENT_TRACE_DEPTH) {
        clearClientStat();
        logDebug("Enabling client stat for lock " + lockID);
        lockManager.enableClientStat(lockID, sink, traceDepth, gatherInterval);
      } else {
        Set<NodeID> statEnabledClients = getStatEnabledClients(lockID);
        if (!statEnabledClients.isEmpty()) {
          lockManager.disableClientStat(lockID, statEnabledClients, sink);
          statEnabledClients.clear();
        }
      }
    }
  }

  public synchronized boolean isLockStatisticsEnabled() {
    return this.lockStatisticsEnabled;
  }

  public synchronized void clear() {
    super.clear();
  }

  private synchronized void clearClientStat() {
    statEnabledClients.clear();
  }

  private synchronized Set<NodeID> getStatEnabledClients(LockID lockID) {
    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    return ((ServerLockStatisticsInfoImpl) lsc).getStatEnabledClients();
  }

  // This method is called within the context of setLockStatisticsConfig(), so it does not need to be synchronized.
  // TODO: We don't need to maintain the client list per lock
  public void recordClientStatEnabled(NodeID nodeID) {
    if (!lockStatisticsEnabled) { return; }
    
    statEnabledClients.add(nodeID);
  }

  // This method is called within the context of setLockStatisticsConfig(), so it does not need to be synchronized
  // TODO: We don't need to maintain the client list per lock
  public boolean isClientStatEnabled(NodeID nodeID) {
    if (!lockStatisticsEnabled) { return false; }
    
    return statEnabledClients.contains(nodeID);
  }

  public synchronized void recordLockHopRequested(LockID lockID) {
    if (!lockStatisticsEnabled) { return; }

    super.recordLockHopRequested(lockID, null);
  }

  public synchronized void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatisticsEnabled) { return; }

    super.recordLockRequested(lockID, nodeID, threadID, null);
  }

  public synchronized void recordLockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy,
                                             long awardedTimeInMillis) {
    if (!lockStatisticsEnabled) { return; }

    super.recordLockAwarded(lockID, nodeID, threadID, isGreedy, awardedTimeInMillis, null);
  }

  public synchronized void recordLockReleased(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatisticsEnabled) { return; }

    super.recordLockReleased(lockID, nodeID, threadID, null);
  }

  public synchronized void recordLockRejected(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatisticsEnabled) { return; }

    super.recordLockRejected(lockID, nodeID, threadID, null);
  }

  public synchronized void recordClientStat(LockID lockID, NodeID nodeID, Collection lockStatElements) {
    logDebug("============== recordClientStat ================");
    logDebug(lockStatElements.toString());
    logDebug("================================================");
    ServerLockStatisticsInfoImpl lsc = (ServerLockStatisticsInfoImpl) getOrCreateLockStatInfo(lockID);
    lsc.setLockStatElements(nodeID, lockStatElements);
  }

  public synchronized long getNumberOfLockRequested(LockID lockID) {
    if (!lockStatisticsEnabled) { return 0; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    return lsc.getNumberOfLockRequested();
  }

  public synchronized long getNumberOfLockReleased(LockID lockID) {
    if (!lockStatisticsEnabled) { return 0; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    return lsc.getNumberOfLockReleased();
  }

  public synchronized long getNumberOfPendingRequests(LockID lockID) {
    if (!lockStatisticsEnabled) { return 0; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    return lsc.getNumberOfPendingRequests();
  }

  public synchronized long getNumberOfLockHopRequests(LockID lockID) {
    if (!lockStatisticsEnabled) { return 0; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    return lsc.getNumberOfLockHopRequested();
  }

  public synchronized Collection<LockSpec> getLockSpecs() {
    if (!lockStatisticsEnabled) { return Collections.EMPTY_LIST; }

    for (Iterator<LockStatisticsInfo> i = lockStats.values().iterator(); i.hasNext();) {
      LockStatisticsInfo lsc = i.next();
      lsc.aggregateLockHoldersData();
    }
    Set<LockSpec> returnSet = new HashSet<LockSpec>(lockStats.values());
    return returnSet;
  }

  public void clearAllStatsFor(NodeID nodeID) {
    //
  }

  private void logDebug(String msg) {
    System.err.println(msg);
  }

}
