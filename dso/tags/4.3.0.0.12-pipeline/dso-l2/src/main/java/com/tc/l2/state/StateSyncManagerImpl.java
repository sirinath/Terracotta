/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.net.NodeID;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Object and Index Sync states for a L2. Active maintains the sync states for all the passives it tries to sync to.
 * Passives can check-point on sync completion messages if needed via hooks available here.
 */
public class StateSyncManagerImpl implements StateSyncManager {

  private final ConcurrentMap<NodeID, SyncValue> syncMessagesProcessedMap = new ConcurrentHashMap<NodeID, SyncValue>();

  @Override
  public void objectSyncComplete(NodeID nodeID) {
    SyncValue value = getOrCreateSyncValue(nodeID);
    value.objectSyncCompleteMessageAcked();
  }

  protected SyncValue getOrCreateSyncValue(NodeID nodeID) {
    SyncValue value = createSyncValue();
    SyncValue old = syncMessagesProcessedMap.putIfAbsent(nodeID, value);
    return old == null ? value : old;
  }

  @Override
  public void indexSyncComplete(NodeID nodeID) {
    // overridden in EE
  }

  @Override
  public void removeL2(NodeID nodeID) {
    syncMessagesProcessedMap.remove(nodeID);
  }

  @Override
  public void objectSyncComplete() {
    // no-op
  }

  @Override
  public void indexSyncComplete() {
    // overridden in EE
  }

  @Override
  public boolean isSyncComplete(NodeID nodeID) {
    SyncValue value = syncMessagesProcessedMap.get(nodeID);
    if (value == null) { return false; }
    return value.isSyncMesssagesAcked();
  }

  static interface SyncValue {

    public void indexSyncCompleteMessageAcked();

    public void objectSyncCompleteMessageAcked();

    public boolean isSyncMesssagesAcked();
  }

  protected SyncValue createSyncValue() {

    return new SyncValue() {
      private volatile boolean objectSyncCompleteMessageProcessed = false;

      @Override
      public void indexSyncCompleteMessageAcked() {
        // no-op
      }

      @Override
      public void objectSyncCompleteMessageAcked() {
        this.objectSyncCompleteMessageProcessed = true;
      }

      @Override
      public boolean isSyncMesssagesAcked() {
        return this.objectSyncCompleteMessageProcessed;
      }

    };
  }

}
