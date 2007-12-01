/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.impl.LockHolder;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Methods in this class are not synchronized because they are called from the context of its concrete subclasses.
 */
public abstract class LockStatisticsManager implements Serializable {
  protected final static int     MIN_CLIENT_TRACE_DEPTH = 0;

  protected final Map            lockStats              = new HashMap();       // map<lockID, LockStatisticsInfo>
  protected final LockStatConfig lockStatConfig         = new LockStatConfig();

//  protected LockHolderStats      holderStats;

  protected boolean              lockStatisticsEnabled;

//  protected static void mergeLockStat(Collection lockStatInfos) {
//    for (Iterator i = lockStatInfos.iterator(); i.hasNext();) {
//      LockStatisticsInfo lockStatInfo = (LockStatisticsInfo) i.next();
//      lockStatInfo.mergeLockStatElements();
//    }
//  }
//
  public void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, StackTraceElement[] stackTraces) {
    if (!lockStatisticsEnabled) { return; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    lsc.recordLockRequested(nodeID, threadID, System.currentTimeMillis(), stackTraces);
  }

  public void recordLockHopRequested(LockID lockID, StackTraceElement[] stackTraces) {
    if (!lockStatisticsEnabled) { return; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    lsc.recordLockHopRequested(stackTraces);
  }

  public boolean recordLockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy,
                                long awardedTimeInMillis, StackTraceElement[] stackTraces) {
    if (!lockStatisticsEnabled) { return false; }

    LockStatisticsInfo lsc = getLockStatInfo(lockID);
    if (lsc != null) {
      return lsc.recordLockAwarded(nodeID, threadID, isGreedy, awardedTimeInMillis, stackTraces);
    }
    return false;
  }

//  public void aggregateLockWaitTime(LockID lockID, StackTraceElement[] stackTraces, long waitTimeInMillis) {
//    if (!lockStatisticsEnabled) { return; }
//
//    LockStatisticsInfo lsc = getLockStatInfo(lockID);
//    if (lsc != null) {
//      lsc.aggregateLockWaitTime(stackTraces, waitTimeInMillis);
//    }
//  }

  public void recordLockRejected(LockID lockID, NodeID nodeID, ThreadID threadID, StackTraceElement[] stackTraces) {
    if (!lockStatisticsEnabled) { return; }

    LockStatisticsInfo lsc = getLockStatInfo(lockID);
    if (lsc != null) {
      lsc.recordLockRejected(nodeID, threadID, stackTraces);
    }
  }

  public boolean recordLockReleased(LockID lockID, NodeID nodeID, ThreadID threadID, StackTraceElement[] stackTraces) {
    if (!lockStatisticsEnabled) { return false; }

    LockStatisticsInfo lsc = getLockStatInfo(lockID);
    if (lsc != null) {
      return lsc.recordLockReleased(nodeID, threadID, stackTraces);
    }
    return false;
  }

  public void setTraceDepth(int traceDepth) {
    if (!lockStatisticsEnabled) { return; }

    lockStatConfig.setTraceDepth(traceDepth);
  }

  public void setGatherInterval(int gatherInterval) {
    if (!lockStatisticsEnabled) { return; }

    lockStatConfig.setGatherInterval(gatherInterval);
  }

  public void clear() {
    this.lockStats.clear();
    this.lockStatConfig.reset();
  }

  public synchronized int getTraceDepth() {
    if (!lockStatisticsEnabled) { return 0; }

    return lockStatConfig.getTraceDepth();
  }

  public synchronized int getGatherInterval() {
    if (!lockStatisticsEnabled) { return 0; }

    return lockStatConfig.getGatherInterval();
  }

  public synchronized void setLockStatisticsEnabled(boolean statEnable) {
    if ((this.lockStatisticsEnabled = statEnable) == false) {
      disableLockStatistics();
    }
  }

  public Map getLockStats() {
    return lockStats;
  }

  public boolean isClientStatEnabled() {
    if (!lockStatisticsEnabled) { return false; }

    return lockStatConfig.getTraceDepth() > MIN_CLIENT_TRACE_DEPTH;
  }

  protected abstract void disableLockStatistics();

  protected abstract LockStatisticsInfo newLockStatisticsContext(LockID lockID);

  protected void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    if (!lockStatisticsEnabled) { return; }

    lockStatConfig.setConfig(traceDepth, gatherInterval);
  }

  protected LockStatisticsInfo getLockStatInfo(LockID lockID) {
    return (LockStatisticsInfo) lockStats.get(lockID);
  }

  protected LockStatisticsInfo getOrCreateLockStatInfo(LockID lockID) {
    LockStatisticsInfo lsc = (LockStatisticsInfo) lockStats.get(lockID);
    if (lsc == null) {
      lsc = newLockStatisticsContext(lockID);
      lockStats.put(lockID, lsc);
    }
    return lsc;
  }
  
//  protected LockKey newLockKey(LockID lockID, NodeID nodeID, ThreadID threadID) {
//    return new LockKey(lockID, nodeID, threadID);
//  }
//
//  protected LockHolder getLockHolder(LockKey key) {
//    return (LockHolder) holderStats.get(key);
//  }
//
//  protected void addLockHolder(LockKey key, LockHolder lockHolder) {
//    holderStats.put(key, lockHolder);
//  }

  protected LockHolder newLockHolder(LockID lockID, NodeID nodeID, ThreadID threadID, long timeStamp) {
    return new LockHolder(lockID, nodeID, threadID, timeStamp);
  }

  protected static class LockStatConfig {
    private final static int DEFAULT_COLLECT_FREQUENCY = 10;
    private final static int DEFAULT_TRACE_DEPTH       = MIN_CLIENT_TRACE_DEPTH;

    private int              traceDepth;
    private int              gatherInterval;

    public LockStatConfig() {
      reset();
    }

    public LockStatConfig(int traceDepth, int gatherInterval) {
      this.traceDepth = traceDepth;
      this.gatherInterval = gatherInterval;
    }

    public int getGatherInterval() {
      return gatherInterval;
    }

    public int getTraceDepth() {
      return traceDepth;
    }

    public void setTraceDepth(int traceDepth) {
      this.traceDepth = traceDepth;
    }

    public void setGatherInterval(int gatherInterval) {
      this.gatherInterval = gatherInterval;
    }

    public void setConfig(int traceDepth, int gatherInterval) {
      this.traceDepth = traceDepth;
      this.gatherInterval = gatherInterval;
    }

    public void reset() {
      this.traceDepth = DEFAULT_TRACE_DEPTH;
      this.gatherInterval = DEFAULT_COLLECT_FREQUENCY;
      TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("l1.lock");
      if (tcProperties != null) {
        this.gatherInterval = tcProperties.getInt("gatherInterval", DEFAULT_COLLECT_FREQUENCY);
      }
    }
  }

//  /**
//   * Inner classes section
//   */
//  protected static class LockHolderStats {
//    private static class PendingStat {
//      private long numOfHolders;
//      private long totalWaitTimeInMillis;
//      private long totalHeldTimeInMillis;
//
//      public PendingStat(long waitTimeInMillis, long heldTimeInMillis) {
//        addPendingHolderData(waitTimeInMillis, heldTimeInMillis);
//      }
//
//      public void addPendingHolderData(long waitTimeInMillis, long heldTimeInMillis) {
//        this.numOfHolders++;
//        this.totalHeldTimeInMillis += heldTimeInMillis;
//        this.totalWaitTimeInMillis += waitTimeInMillis;
//      }
//    }
//
//    private final static int NO_LIMIT = -1;
//
//    private final Map        pendingData;  // map<LockKey.subKey, map<LockKey, LockHolder>>
//    private final LinkedList historyData;  // list of LockHolder
//    private final int        maxSize;
//
//    public LockHolderStats() {
//      this(NO_LIMIT);
//    }
//
//    public LockHolderStats(int maxSize) {
//      pendingData = new HashMap();
//      historyData = new LinkedList();
//      this.maxSize = maxSize;
//    }
//
//    public void clear() {
//      this.pendingData.clear();
//      this.historyData.clear();
//    }
//
//    public void remove(LockKey key) {
//      LockKey subKey = key.subKey();
//      Map lockHolders = (Map) pendingData.get(subKey);
//      if (lockHolders != null) {
//        lockHolders.remove(key);
//      }
//    }
//
//    public void put(LockKey key, LockHolder value) {
//      LockKey subKey = key.subKey();
//      Map lockHolders = (Map) pendingData.get(subKey);
//      if (lockHolders == null) {
//        lockHolders = new HashMap();
//        pendingData.put(subKey, lockHolders);
//      }
//      lockHolders.put(key, value);
//    }
//
//    public void remove(LockKey key, Object value) {
//      LockKey subKey = key.subKey();
//      Map lockHolders = (Map) pendingData.get(subKey);
//      lockHolders.remove(key);
//    }
//
//    public void moveToHistory(LockKey key, Object value) {
//      LockKey subKey = key.subKey();
//      Map lockHolders = (Map) pendingData.get(subKey);
//      LockHolder o = (LockHolder) lockHolders.remove(key);
//      historyData.addLast(o);
//      removeOldDataIfNeeded();
//    }
//
//    private void removeOldDataIfNeeded() {
//      if (maxSize != NO_LIMIT && historyData.size() > maxSize) {
//        historyData.removeFirst();
//      }
//    }
//
//    public Object get(LockKey key) {
//      LockKey subKey = key.subKey();
//      Map lockHolders = (Map) pendingData.get(subKey);
//      if (lockHolders == null) return null;
//      if (lockHolders.size() == 0) return null;
//
//      return lockHolders.get(key);
//    }
//
//    public boolean contains(LockKey key) {
//      LockKey subKey = key.subKey();
//      Map lockHolders = (Map) pendingData.get(subKey);
//      return lockHolders.containsKey(key);
//    }
//
//    /**
//     * @param lockStats A Map of <LockID, ServerLockStatisticsInfoImpl>
//     * @return Collection<LockSpec>
//     */
//    public Collection aggregateLockHoldersData(Map lockStats) {
//      Map aggregateData = new HashMap(); // map<LockID, PendingStat>
//
//      Collection val = pendingData.values();
//      for (Iterator i = val.iterator(); i.hasNext();) {
//        Map lockHolders = (Map) i.next();
//        for (Iterator j = lockHolders.values().iterator(); j.hasNext();) {
//          LockHolder lockHolder = (LockHolder) j.next();
//          updateAggregateLockHolder(aggregateData, lockHolder);
//        }
//      }
//      for (Iterator i = aggregateData.keySet().iterator(); i.hasNext();) {
//        LockID lockID = (LockID) i.next();
//        PendingStat pendingStat = (PendingStat) aggregateData.get(lockID);
//        LockStatisticsInfo lsc = (LockStatisticsInfo) lockStats.get(lockID);
//        lsc.aggregateAvgWaitTimeInMillis(pendingStat.totalWaitTimeInMillis, pendingStat.numOfHolders);
//        lsc.aggregateAvgHeldTimeInMillis(pendingStat.totalHeldTimeInMillis, pendingStat.numOfHolders);
//      }
//      mergeLockStat(lockStats.values());
//      Set returnSet = new HashSet(lockStats.values());
//      return returnSet;
//    }
//
//    private void updateAggregateLockHolder(Map aggregateData, LockHolder lockHolder) {
//      PendingStat pendingStat = (PendingStat) aggregateData.get(lockHolder.getLockID());
//      if (pendingStat == null) {
//        pendingStat = new PendingStat(lockHolder.getWaitTimeInMillis(), lockHolder.getHeldTimeInMillis());
//        aggregateData.put(lockHolder.getLockID(), pendingStat);
//      } else {
//        pendingStat.addPendingHolderData(lockHolder.getWaitTimeInMillis(), lockHolder.getHeldTimeInMillis());
//      }
//    }
//
//    public void clearAllStatsFor(NodeID nodeID) {
//      Set lockKeys = pendingData.keySet();
//      for (Iterator i = lockKeys.iterator(); i.hasNext();) {
//        LockKey key = (LockKey) i.next();
//        if (nodeID.equals(key.getNodeID())) {
//          i.remove();
//        }
//      }
//    }
//
//    public String toString() {
//      return pendingData.toString();
//    }
//    
//    public void addLockHolder(LockKey key, LockHolder lockHolder) {
//      put(key, lockHolder);
//    }
//  }
//
}
