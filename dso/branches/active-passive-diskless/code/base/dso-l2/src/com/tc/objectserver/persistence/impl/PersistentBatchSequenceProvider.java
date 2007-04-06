/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.api.PersistentSequence;
import com.tc.util.sequence.BatchSequenceProvider;
import com.tc.util.sequence.BatchSequenceReceiver;

public class PersistentBatchSequenceProvider extends AbstractEventHandler implements BatchSequenceProvider {

  private Sink                          requestBatchSink;
  private final PersistentSequence      sequence;
  private ReplicatedClusterStateManager clusterStateMgr;

  public PersistentBatchSequenceProvider(PersistentSequence sequence) {
    this.sequence = sequence;
  }

  public void setRequestBatchSink(Sink sink) {
    this.requestBatchSink = sink;
  }

  // EventHandler interface
  public void handleEvent(EventContext context) {
    GlobalTransactionIDBatchRequestContext ctxt = (GlobalTransactionIDBatchRequestContext) context;
    BatchSequenceReceiver receiver = ctxt.getReceiver();
    int batchSize = ctxt.getBatchSize();
    long start = sequence.nextBatch(batchSize);
    this.clusterStateMgr.publishNextAvailableGlobalTransactionID(start + batchSize);
    receiver.setNextBatch(start, start + batchSize);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.clusterStateMgr = scc.getL2Coordinator().getReplicatedClusterStateManager();
  }

  // BatchSequenceProvider interface
  public void requestBatch(BatchSequenceReceiver receiver, int size) {
    this.requestBatchSink.add(new GlobalTransactionIDBatchRequestContext(receiver, size));
  }

  public static final class GlobalTransactionIDBatchRequestContext implements EventContext {
    private final BatchSequenceReceiver receiver;
    private final int                   size;

    public GlobalTransactionIDBatchRequestContext(BatchSequenceReceiver receiver, int size) {
      this.receiver = receiver;
      this.size = size;
    }

    public BatchSequenceReceiver getReceiver() {
      return this.receiver;
    }

    public int getBatchSize() {
      return this.size;
    }
  }
}
