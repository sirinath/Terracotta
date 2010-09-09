/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

public class TransactionState {
  private static final int APPLY_COMMITTED            = 0x01;
  private static final int BROADCAST_COMPLETED        = 0x02;
  private static final int TXN_RELAYED                = 0x04;
  private static final int PROCESS_METADATA_COMPLETED = 0x08;

  private static final int TXN_PROCESSING_COMPLETE    = (APPLY_COMMITTED | BROADCAST_COMPLETED | TXN_RELAYED | PROCESS_METADATA_COMPLETED);

  private int              state                      = 0x00;

  public void applyAndCommitSkipped() {
    state |= APPLY_COMMITTED;
  }

  public boolean isComplete() {
    return (state == TXN_PROCESSING_COMPLETE);
  }

  public void broadcastCompleted() {
    state |= BROADCAST_COMPLETED;
  }
  
  public void processMetaDataCompleted() {
    state |= PROCESS_METADATA_COMPLETED;
  }

  public void applyCommitted() {
    state |= APPLY_COMMITTED;
  }
  
  public void relayTransactionComplete() {
    state |= TXN_RELAYED;
  }

  public String toString() {
    return "TransactionState = [ " 
           + ((state & APPLY_COMMITTED) == APPLY_COMMITTED ? " APPLY_COMMITED : " : " : ")
           + ((state & TXN_RELAYED) == TXN_RELAYED ? " TXN_RELAYED : " : " : ")
           + ((state & BROADCAST_COMPLETED) == BROADCAST_COMPLETED ? " BROADCAST_COMPLETE } " : " : ")
           + ((state & PROCESS_METADATA_COMPLETED) == PROCESS_METADATA_COMPLETED ? " PROCESS_METADATA_COMPLETED } " : " ]");
  }

}