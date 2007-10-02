/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.async.api.Sink;
import com.tc.management.stats.LRUMap;
import com.tc.management.stats.TopN;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.LockHolder;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class L2LockStatsManagerImpl implements L2LockStatsManager {
  private final static int        PING_PONG_LOOP_LENGTH               = 2;
  private final static int        TOP_N                               = 5;
  private final static Comparator LOCK_REQUESTED_COMPARATOR           = new Comparator() {
                                                                        public int compare(Object o1, Object o2) {
                                                                          LockStat s1 = (LockStat) o1;
                                                                          LockStat s2 = (LockStat) o2;
                                                                          if (s1 == s2) { return 0; }
                                                                          if (s1.getNumOfLockRequested() <= s2
                                                                              .getNumOfLockRequested()) { return -1; }
                                                                          return 1;
                                                                        }
                                                                      };
  private final static Comparator LOCK_PING_PONG_REQUESTED_COMPARATOR = new Comparator() {
                                                                        public int compare(Object o1, Object o2) {
                                                                          LockStat s1 = (LockStat) o1;
                                                                          LockStat s2 = (LockStat) o2;
                                                                          if (s1 == s2) { return 0; }
                                                                          if (s1.getNumOfPingPongRequests() <= s2
                                                                              .getNumOfPingPongRequests()) { return -1; }
                                                                          return 1;
                                                                        }
                                                                      };
  private final static Comparator LOCK_HELD_COMPARATOR                = new Comparator() {
                                                                        public int compare(Object o1, Object o2) {
                                                                          LockHolder s1 = (LockHolder) o1;
                                                                          LockHolder s2 = (LockHolder) o2;
                                                                          if (s1 == s2) { return 0; }
                                                                          if (s1.getHeldTimeInMillis() <= s2
                                                                              .getHeldTimeInMillis()) { return -1; }
                                                                          return 1;
                                                                        }
                                                                      };
  private final static Comparator PENDING_LOCK_REQUESTS_COMPARATOR    = new Comparator() {
                                                                        public int compare(Object o1, Object o2) {
                                                                          LockStat s1 = (LockStat) o1;
                                                                          LockStat s2 = (LockStat) o2;
                                                                          if (s1 == s2) { return 0; }
                                                                          if (s1.getNumOfPendingRequests() <= s2
                                                                              .getNumOfPendingRequests()) { return -1; }
                                                                          return 1;
                                                                        }
                                                                      };
  private final static Comparator LOCK_ACQUIRED_WAITING_COMPARATOR    = new Comparator() {
                                                                        public int compare(Object o1, Object o2) {
                                                                          LockHolder s1 = (LockHolder) o1;
                                                                          LockHolder s2 = (LockHolder) o2;
                                                                          if (s1 == s2) { return 0; }
                                                                          if (s1.getWaitTimeInMillis() <= s2
                                                                              .getWaitTimeInMillis()) { return -1; }
                                                                          return 1;
                                                                        }
                                                                      };

  private final LockHolderStats   holderStats;
  private final LRUMap            lockStats;
  private final LRUMap            previousLockHolders;
  private DSOChannelManager       channelManager;
  private LockManager             lockManager;
  private Sink                    sink;
  private final int               topN;
  private final Map               clientStatEnabledLock;
  private final LRUMap            lockStackTraces;

  public L2LockStatsManagerImpl() {
    TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("lock.statistics");
    clientStatEnabledLock = new HashMap();
    this.topN = tcProperties.getInt("max", TOP_N);
    holderStats = new LockHolderStats(topN);
    lockStats = new LRUMap(topN);
    previousLockHolders = new LRUMap(topN);
    lockStackTraces = new LRUMap(topN);
  }

  public synchronized void start(DSOChannelManager channelManager, LockManager lockManager, Sink sink) {
    this.channelManager = channelManager;
    this.lockManager = lockManager;
    this.sink = sink;
  }

  private LockHolder newLockHolder(LockID lockID, NodeID nodeID, ThreadID threadID, int lockLevel, long timeStamp) {
    return new LockHolder(lockID, nodeID, channelManager.getChannelAddress(nodeID), threadID, lockLevel,
                          timeStamp);
  }

  private LockKey newLockKey(LockID lockID, NodeID nodeID, ThreadID threadID) {
    return new LockKey(lockID, nodeID, threadID);
  }

  public void enableClientStat(LockID lockID) {
    synchronized (this) {
      Set statEnabledClients = (Set) clientStatEnabledLock.get(lockID);
      if (statEnabledClients == null) {
        statEnabledClients = new HashSet();
        clientStatEnabledLock.put(lockID, statEnabledClients);
      }
    }
    lockManager.enableClientStat(lockID, sink);
  }

  public void disableClientStat(LockID lockID) {
    Set statEnabledClients = null;
    synchronized (this) {
      statEnabledClients = (Set) clientStatEnabledLock.remove(lockID);
      lockStackTraces.remove(lockID);
    }
    if (statEnabledClients != null) {
      lockManager.disableClientStat(lockID, statEnabledClients, sink);
    }
  }

  public synchronized boolean isClientLockStatEnable(LockID lockID) {
    return clientStatEnabledLock.containsKey(lockID);
  }

  public synchronized boolean isLockStatEnabledInClient(LockID lockID, NodeID nodeID) {
    Set statEnabledClients = (Set) clientStatEnabledLock.get(lockID);
    if (statEnabledClients == null) { return false; }
    return statEnabledClients.contains(nodeID);
  }

  public synchronized void recordClientStatEnabled(LockID lockID, NodeID nodeID) {
    Set statEnabledClients = (Set) clientStatEnabledLock.get(lockID);
    Assert.assertNotNull(statEnabledClients);
    statEnabledClients.add(nodeID);
  }

  public synchronized void lockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, int lockLevel) {
    LockStat lockStat = (LockStat) lockStats.get(lockID);
    if (lockStat == null) {
      lockStat = new LockStat(lockID);
      lockStats.put(lockID, lockStat);
    }
    lockStat.lockRequested();

    LockHolder lockHolder = newLockHolder(lockID, nodeID, threadID, lockLevel, System.currentTimeMillis());
    addLockHolder(newLockKey(lockID, nodeID, threadID), lockHolder);
  }

  private LockHolder getLockHolder(LockKey key) {
    return (LockHolder) holderStats.get(key);
  }

  public void addLockHolder(LockKey key, LockHolder lockHolder) {
    holderStats.put(key, lockHolder);
  }

  public synchronized void lockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy,
                                       long lockAwardTimestamp) {
    LockKey lockKey = newLockKey(lockID, nodeID, threadID);
    LockHolder lockHolder = getLockHolder(lockKey);

    Assert.assertNotNull(lockHolder);

    lockHolder.lockAcquired(lockAwardTimestamp);
    if (isGreedy) {
      holderStats.remove(lockKey, lockHolder);
      lockKey = newLockKey(lockID, nodeID, ThreadID.VM_ID);
      holderStats.put(lockKey, lockHolder);
    }

    LockStat lockStat = (LockStat) lockStats.get(lockID);
    if (lockStat != null) {
      lockStat.lockAwarded();

      List previousLockHolderList = (List) previousLockHolders.get(lockID);
      if (previousLockHolderList != null) {
        if (previousLockHolderList.size() > 1 && previousLockHolderList.get(0).equals(lockHolder.getNodeID())) {
          lockStat.lockPingPong();
        }
      }
    }
  }

  public synchronized void lockReleased(LockID lockID, NodeID nodeID, ThreadID threadID) {
    LockStat lockStat = (LockStat) lockStats.get(lockID);
    if (lockStat != null) {
      lockStat.lockReleased();
    }

    LockHolder lockHolder = lockReleasedInternal(lockID, nodeID, threadID);
    Assert.assertNotNull(lockHolder);

    List previousLockHolderList = (List) previousLockHolders.get(lockID);
    if (previousLockHolderList == null) {
      previousLockHolderList = new ArrayList(PING_PONG_LOOP_LENGTH);
      previousLockHolders.put(lockID, previousLockHolderList);
    }
    previousLockHolderList.add(lockHolder.getNodeID());
    if (previousLockHolderList.size() > PING_PONG_LOOP_LENGTH) {
      previousLockHolderList.remove(0);
    }
  }

  private LockHolder lockReleasedInternal(LockID lockID, NodeID nodeID, ThreadID threadID) {
    LockKey lockKey = newLockKey(lockID, nodeID, threadID);
    LockHolder lockHolder = getLockHolder(lockKey);
    Assert.assertNotNull(lockHolder);
    lockHolder.lockReleased();
    holderStats.moveToHistory(lockKey, lockHolder);

    return lockHolder;
  }

  public synchronized void lockRejected(LockID lockID, NodeID nodeID, ThreadID threadID) {
    LockStat lockStat = (LockStat) lockStats.get(lockID);
    Assert.assertNotNull(lockStat);
    lockStat.lockRejected();

    lockReleasedInternal(lockID, nodeID, threadID);
  }

  public synchronized void lockWait(LockID lockID) {
    LockStat lockStat = (LockStat) lockStats.get(lockID);
    if (lockStat != null) {
      lockStat.lockWaited();
    }
  }

  public synchronized void lockNotified(LockID lockID, int n) {
    LockStat lockStat = (LockStat) lockStats.get(lockID);
    if (lockStat != null) {
      lockStat.lockNotified(n);
    }
  }

  public synchronized void recordStackTraces(LockID lockID, NodeID nodeID, List stackTraces) {
    Map existingStackTraces = (Map) lockStackTraces.get(lockID);
    LockKey lockKey = new LockKey(lockID, nodeID);
    if (existingStackTraces == null) {
      existingStackTraces = new LRUMap(topN);
      existingStackTraces.put(lockKey, new LockStackTracesStat(nodeID, stackTraces));
      lockStackTraces.put(lockID, existingStackTraces);
    } else {
      LockStackTracesStat stackTracesStat = (LockStackTracesStat) existingStackTraces.get(lockKey);
      if (stackTracesStat == null) {
        stackTracesStat = new LockStackTracesStat(nodeID, stackTraces);
        existingStackTraces.put(lockKey, stackTracesStat);
      } else {
        stackTracesStat.addStackTraces(stackTraces);
      }
    }
  }

  public synchronized long getNumberOfLockRequested(LockID lockID) {
    return ((LockStat) lockStats.get(lockID)).getNumOfLockRequested();
  }

  public synchronized long getNumberOfLockReleased(LockID lockID) {
    return ((LockStat) lockStats.get(lockID)).getNumOfLockReleased();
  }

  public synchronized long getNumberOfPendingRequests(LockID lockID) {
    return ((LockStat) lockStats.get(lockID)).getNumOfPendingRequests();
  }

  public synchronized LockHolder getLockHolder(LockID lockID, NodeID nodeID, ThreadID threadID) {
    return getLockHolder(newLockKey(lockID, nodeID, threadID));
  }

  public synchronized long getNumberOfLockPingPongRequests(LockID lockID) {
    return ((LockStat) lockStats.get(lockID)).getNumOfPingPongRequests();
  }

  public synchronized Collection getTopLockStats(int n) {
    Collection allLockStats = lockStats.values();
    TopN topNLockStats = new TopN(LOCK_REQUESTED_COMPARATOR, n);
    topNLockStats.evaluate(allLockStats);
    return topNLockStats.getDataSnapshot();
  }

  public synchronized Collection getTopLockHoldersStats(int n) {
    return holderStats.topN(n, LOCK_HELD_COMPARATOR);
  }

  public synchronized Collection getTopWaitingLocks(int n) {
    return holderStats.topN(n, LOCK_ACQUIRED_WAITING_COMPARATOR);
  }

  public synchronized Collection getTopContentedLocks(int n) {
    Collection allLockStats = lockStats.values();
    TopN topNLockStats = new TopN(PENDING_LOCK_REQUESTS_COMPARATOR, n);
    topNLockStats.evaluate(allLockStats);
    return topNLockStats.getDataSnapshot();
  }

  public synchronized Collection getTopPingPongLocks(int n) {
    Collection allLockStats = lockStats.values();
    TopN topNLockStats = new TopN(LOCK_PING_PONG_REQUESTED_COMPARATOR, n);
    topNLockStats.evaluate(allLockStats);
    return topNLockStats.getDataSnapshot();
  }

  public synchronized Collection getStackTraces(LockID lockID) {
    Map stackTraces = (Map) lockStackTraces.get(lockID);
    if (stackTraces == null) { return Collections.EMPTY_LIST; }
    return new ArrayList(stackTraces.values());
  }

  private static class LockKey {
    private LockID    lockID;
    private NodeID nodeID;
    private ThreadID  threadID;
    private int       hashCode;

    private LockKey   subKey;

    public LockKey(LockID lockID, NodeID nodeID) {
      this.lockID = lockID;
      this.nodeID = nodeID;
      this.threadID = null;
      this.subKey = null;
      this.hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(nodeID).toHashCode();
    }

    public LockKey(LockID lockID, NodeID nodeID, ThreadID threadID) {
      this.lockID = lockID;
      this.nodeID = nodeID;
      this.threadID = threadID;
      this.hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(nodeID).append(threadID).toHashCode();
      this.subKey = new LockKey(lockID, nodeID);
    }

    public String toString() {
      return "LockKey [ " + lockID + ", " + nodeID + ", " + threadID + ", " + hashCode + "] ";
    }

    public NodeID getNodeID() {
      return nodeID;
    }

    public LockID getLockID() {
      return lockID;
    }

    public ThreadID getThreadID() {
      return threadID;
    }

    public LockKey subKey() {
      return subKey;
    }

    public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof LockKey)) return false;
      LockKey cmp = (LockKey) o;
      if (threadID != null) {
        return lockID.equals(cmp.lockID) && nodeID.equals(cmp.nodeID) && threadID.equals(cmp.threadID);
      } else {
        return lockID.equals(cmp.lockID) && nodeID.equals(cmp.nodeID);
      }
    }

    public int hashCode() {
      return hashCode;
    }
  }

  private static class LockHolderStats {
    private final static int NO_LIMIT = -1;

    private final Map        pendingData;  // map from key to map
    private final LinkedList historyData;  // list of LockHolder
    private final int        maxSize;

    public LockHolderStats() {
      this(NO_LIMIT);
    }

    public LockHolderStats(int maxSize) {
      pendingData = new HashMap();
      historyData = new LinkedList();
      this.maxSize = maxSize;
    }

    public void put(LockKey key, Object value) {
      LockKey subKey = key.subKey();
      Map lockHolders = (Map) pendingData.get(subKey);
      if (lockHolders == null) {
        lockHolders = new HashMap();
        pendingData.put(subKey, lockHolders);
      }
      lockHolders.put(key, value);
    }

    public void remove(LockKey key, Object value) {
      LockKey subKey = key.subKey();
      Map lockHolders = (Map) pendingData.get(subKey);
      lockHolders.remove(key);
    }

    public void moveToHistory(LockKey key, Object value) {
      LockKey subKey = key.subKey();
      Map lockHolders = (Map) pendingData.get(subKey);
      Object o = lockHolders.remove(key);
      lockHolders = (Map) pendingData.get(key);
      historyData.addLast(o);
      removeOldDataIfNeeded();
    }

    private void removeOldDataIfNeeded() {
      if (maxSize != NO_LIMIT && historyData.size() > maxSize) {
        historyData.removeFirst();
      }
    }

    public Object get(LockKey key) {
      LockKey subKey = key.subKey();
      Map lockHolders = (Map) pendingData.get(subKey);
      if (lockHolders == null) return null;
      if (lockHolders.size() == 0) return null;

      return lockHolders.get(key);
    }

    public boolean contains(LockKey key) {
      LockKey subKey = key.subKey();
      Map lockHolders = (Map) pendingData.get(subKey);
      return lockHolders.containsKey(key);
    }

    public Collection topN(int n, Comparator comparator) {
      Collection val = pendingData.values();
      TopN topN = new TopN(comparator, n);
      for (Iterator i = val.iterator(); i.hasNext();) {
        Map lockHolders = (Map) i.next();
        topN.evaluate(lockHolders.values());
      }
      topN.evaluate(historyData);
      return topN.getDataSnapshot();
    }

    public String toString() {
      return pendingData.toString();
    }
  }

  private static class LockStackTracesStat implements Serializable {
    private final NodeID nodeID;
    private final List      stackTraces;

    public LockStackTracesStat(NodeID nodeID, List stackTraces) {
      this.nodeID = nodeID;
      this.stackTraces = stackTraces;
    }

    public void addStackTraces(List stackTraces) {
      this.stackTraces.addAll(stackTraces);
    }

    public String toString() {
      StringBuffer sb = new StringBuffer(nodeID.toString());
      sb.append("\n");
      for (Iterator i = stackTraces.iterator(); i.hasNext();) {
        sb.append(i.next().toString());
        sb.append("\n\n");
      }
      return sb.toString();
    }
  }

  private static class LockStat implements Serializable {
    private static final long serialVersionUID = 618840956490853662L;

    private final LockID      lockID;
    private long              numOfPendingRequests;
    private long              numOfPendingWaiters;
    private long              numOfRequested;
    private long              numOfReleased;
    private long              numOfRejected;
    private long              numOfPingPongRequests;

    public LockStat(LockID lockID) {
      this.lockID = lockID;
      numOfRequested = 0;
      numOfReleased = 0;
      numOfPendingRequests = 0;
      numOfPendingWaiters = 0;
      numOfPingPongRequests = 0;
    }

    public void lockRequested() {
      numOfRequested++;
      numOfPendingRequests++;
    }

    public void lockAwarded() {
      numOfPendingRequests--;
    }

    public void lockRejected() {
      numOfPendingRequests--;
      numOfRejected++;
    }

    public void lockWaited() {
      numOfPendingWaiters++;
    }

    public void lockNotified(int n) {
      numOfPendingWaiters -= n;
    }

    public void lockPingPong() {
      numOfPingPongRequests++;
    }

    public long getNumOfLockRequested() {
      return numOfRequested;
    }

    public void lockReleased() {
      numOfReleased++;
    }

    public long getNumOfLockReleased() {
      return numOfReleased;
    }

    public long getNumOfPendingRequests() {
      return numOfPendingRequests;
    }

    public long getNumOfPendingWaiters() {
      return numOfPendingWaiters;
    }

    public long getNumOfPingPongRequests() {
      return numOfPingPongRequests;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer("[LockID: ");
      sb.append(lockID);
      sb.append(", number of requested: ");
      sb.append(numOfRequested);
      sb.append(", number of released: ");
      sb.append(numOfReleased);
      sb.append(", number of pending requests: ");
      sb.append(numOfPendingRequests);
      sb.append(", number of pending waiters: ");
      sb.append(numOfPendingWaiters);
      sb.append(", number of ping pong: ");
      sb.append(numOfPingPongRequests);
      sb.append("]");
      return sb.toString();
    }
  }

}
