/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2LockStatsManager;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
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
  private static final TCLogger logger = TCLogging.getLogger(L2LockStatisticsManagerImpl.class);

  private LockManager           lockManager;
  private DSOChannelManager     channelManager;
  private Sink                  sink;

  private final static void sendLockStatisticsMessage(MessageChannel channel, boolean statsEnable, int traceDepth,
                                                      int gatherInterval) {
    LockStatisticsMessage lockStatsMessage = (LockStatisticsMessage) channel
        .createMessage(TCMessageType.LOCK_STAT_MESSAGE);
    if (statsEnable) {
      lockStatsMessage.initializeEnableStat(traceDepth, gatherInterval);
    } else {
      lockStatsMessage.initializeDisableStat();
    }
    lockStatsMessage.send();
  }

  public L2LockStatisticsManagerImpl() {
    TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("lock.statistics");
    if (tcProperties == null) {
      this.lockStatisticsEnabled = false;
    } else {
      if (tcProperties.getProperty("enabled") == null) {
        this.lockStatisticsEnabled = false;
      } else {
        this.lockStatisticsEnabled = tcProperties.getBoolean("enabled");
      }
    }
  }

  public synchronized void start(DSOChannelManager channelManager, LockManager lockManager, Sink sink) {
    this.channelManager = channelManager;
    this.lockManager = lockManager;
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
    synchronized (this) {
      if (!lockStatisticsEnabled) { return; }
      super.setLockStatisticsConfig(traceDepth, gatherInterval);
    }

    MessageChannel[] channels = channelManager.getActiveChannels();
    for (int i = 0; i < channels.length; i++) {
      sendLockStatisticsMessage(channels[i], traceDepth > 0, traceDepth, gatherInterval);
    }
  }

  public synchronized boolean isLockStatisticsEnabled() {
    return this.lockStatisticsEnabled;
  }

  public synchronized void clear() {
    super.clear();
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

  public void enableStatsForNodeIfNeeded(NodeID nodeID) {
    try {
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      int traceDepth = getTraceDepth();
      int gatherInterval = getGatherInterval();
      if (traceDepth > 0) {
        sendLockStatisticsMessage(channel, traceDepth > 0, traceDepth, gatherInterval);
      }
    } catch (NoSuchChannelException e) {
      logger.warn(e);
    }
  }

  private void logDebug(String msg) {
    System.err.println(msg);
  }

}
