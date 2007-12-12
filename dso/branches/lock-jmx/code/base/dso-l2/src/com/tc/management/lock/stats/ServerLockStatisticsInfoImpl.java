/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ServerLockStatisticsInfoImpl implements LockSpec, LockStatisticsInfo, Serializable {
  private final LockID                                   lockID;
  private final Set<NodeID>                              statEnabledClients     = new HashSet<NodeID>();
  private final Map<NodeID, Collection<LockStatElement>> clientLockStatElements = new HashMap<NodeID, Collection<LockStatElement>>();

  private LockStatElement                                statElement;
  private LockStats                                      stat;
  private String                                         lockType;

  public ServerLockStatisticsInfoImpl(LockID lockID) {
    this.lockID = lockID;
    init();
  }

  public void init() {
    statEnabledClients.clear();
    clientLockStatElements.clear();
    statElement = new LockStatElement(lockID, null);
    stat = statElement.getStats();
  }

  public LockID getLockID() {
    return lockID;
  }

  public LockStats getStats() {
    return stat;
  }

  // TODO: return empty string for now
  public String getObjectType() {
    return lockType;
  }

  public Collection children() {
    return statElement.children();
  }

  public void recordLockRequested(NodeID nodeID, ThreadID threadID, long requestedTimeInMillis,
                                  StackTraceElement[] stackTraces, String contextInfo) {
    this.lockType = contextInfo;
    statElement.recordLockRequested(nodeID, threadID, requestedTimeInMillis, contextInfo, stackTraces, 0);
  }

  public void recordLockRejected(NodeID nodeID, ThreadID threadID) {
    statElement.recordLockRejected(nodeID, threadID);
  }

  public void recordLockHopRequested() {
    statElement.recordLockHopped();
  }

  public boolean recordLockAwarded(NodeID nodeID, ThreadID threadID, boolean isGreedy, long awardedTimeInMillis,
                                   int nestedLockDepth) {
    return statElement.recordLockAwarded(nodeID, threadID, isGreedy, awardedTimeInMillis, nestedLockDepth);
  }

  public boolean recordLockReleased(NodeID nodeID, ThreadID threadID) {
    return statElement.recordLockReleased(nodeID, threadID);
  }

  public void addClient(NodeID nodeID) {
    statEnabledClients.add(nodeID);
  }

  public Set getStatEnabledClients() {
    return statEnabledClients;
  }

  public boolean isClientLockStatEnabled(NodeID nodeID) {
    return statEnabledClients.contains(nodeID);
  }

  public long getNumberOfLockRequested() {
    return stat.getNumOfLockRequested();
  }

  public long getNumberOfLockReleased() {
    return stat.getNumOfLockReleased();
  }

  public long getNumberOfLockHopRequested() {
    return stat.getNumOfLockHopRequests();
  }

  public long getNumberOfPendingRequests() {
    return stat.getNumOfLockPendingRequested();
  }

  public LockStatElement getLockStatElement() {
    return statElement;
  }

  public void setLockStatElements(NodeID nodeID, Collection lockStatElements) {
    clientLockStatElements.put(nodeID, lockStatElements);
  }

  private void mergeLockStatElements() {
    statElement.clearChild();

    logDebug("*******Getting from clientLockStatElement: " + clientLockStatElements.size());
    for (Iterator<Collection<LockStatElement>> i = clientLockStatElements.values().iterator(); i.hasNext();) {
      Collection<LockStatElement> lockStatElements = i.next();
      logDebug("*******Getting from clientLockStatElement: " + lockStatElements);
      for (Iterator<LockStatElement> it = lockStatElements.iterator(); it.hasNext();) {
        LockStatElement lse = it.next();
        statElement.mergeChild(lse);
        logDebug("=====>statElement after merge: " + statElement);
      }
    }
  }

  public void aggregateLockHoldersData() {
    statElement.aggregateLockHoldersData(stat, 0);
    mergeLockStatElements();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("lockType: " );
    sb.append(lockType);
    sb.append(" ");
    sb.append(statElement.toString());
    sb.append("\n");
    return sb.toString();
  }

  private void logDebug(String msg) {
    System.err.println(msg);
  }
}
