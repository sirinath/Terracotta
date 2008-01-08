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

  private LockStatElement                                clientStatElement, serverStatElement;
  private LockStats                                      clientStat, serverStat;
  private String                                         lockType;

  public ServerLockStatisticsInfoImpl(LockID lockID) {
    this.lockID = lockID;
    init();
  }

  public void init() {
    statEnabledClients.clear();
    clientLockStatElements.clear();
    clientStatElement = new LockStatElement(lockID, null);
    clientStat = clientStatElement.getStats();
    serverStatElement = new LockStatElement(lockID, null);
    serverStat = serverStatElement.getStats();
    System.err.println("&&&&&&&&&&&&&&&&&& holdersDataStat after serverStatElement: " + serverStatElement.holderStats + " " + System.identityHashCode(serverStatElement));
  }

  public LockID getLockID() {
    return lockID;
  }
  
  public LockStats getServerStats() {
    return serverStat;
  }

  public LockStats getClientStats() {
    return clientStat;
  }

  // TODO: return empty string for now
  public String getObjectType() {
    return lockType;
  }

  public Collection children() {
    return clientStatElement.children();
  }

  public void recordLockRequested(NodeID nodeID, ThreadID threadID, long requestedTimeInMillis,
                                  StackTraceElement[] stackTraces, String contextInfo) {
    this.lockType = contextInfo;
    serverStatElement.recordLockRequested(nodeID, threadID, requestedTimeInMillis, contextInfo, stackTraces, 0);
  }

  public void recordLockRejected(NodeID nodeID, ThreadID threadID) {
    serverStatElement.recordLockRejected(nodeID, threadID);
  }

  public void recordLockHopRequested() {
    serverStatElement.recordLockHopped();
  }

  public boolean recordLockAwarded(NodeID nodeID, ThreadID threadID, boolean isGreedy, long awardedTimeInMillis,
                                   int nestedLockDepth) {
    return serverStatElement.recordLockAwarded(nodeID, threadID, isGreedy, awardedTimeInMillis, nestedLockDepth);
  }

  public boolean recordLockReleased(NodeID nodeID, ThreadID threadID) {
    return serverStatElement.recordLockReleased(nodeID, threadID);
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
    return serverStat.getNumOfLockRequested();
  }

  public long getNumberOfLockReleased() {
    return serverStat.getNumOfLockReleased();
  }

  public long getNumberOfLockHopRequested() {
    return serverStat.getNumOfLockHopRequests();
  }

  public long getNumberOfPendingRequests() {
    return serverStat.getNumOfLockPendingRequested();
  }

  public LockStatElement getLockStatElement() {
    return serverStatElement;
  }

  public void setLockStatElements(NodeID nodeID, Collection lockStatElements) {
    clientLockStatElements.put(nodeID, lockStatElements);
  }

  private void mergeLockStatElements() {
    clientStatElement.clearChild();

    logDebug("*******Getting from clientLockStatElement: " + clientLockStatElements.size());
    for (Iterator<Collection<LockStatElement>> i = clientLockStatElements.values().iterator(); i.hasNext();) {
      Collection<LockStatElement> lockStatElements = i.next();
      logDebug("*******Getting from clientLockStatElement: " + lockStatElements);
      for (Iterator<LockStatElement> it = lockStatElements.iterator(); it.hasNext();) {
        LockStatElement lse = it.next();
        clientStatElement.mergeChild(lse);
        logDebug("=====>statElement after merge: " + clientStatElement);
      }
    }
  }

  public void aggregateLockHoldersData() {
    mergeLockStatElements();
    clientStatElement.aggregateLockStat();
    serverStatElement.aggregateLockHoldersData(serverStat, 0);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("lockType: " );
    sb.append(lockType);
    sb.append(" ");
    sb.append(serverStatElement.toString());
    sb.append("\n");
    return sb.toString();
  }

  private void logDebug(String msg) {
    System.err.println(msg);
  }
}
