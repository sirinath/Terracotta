/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.object.gtx.GlobalTransactionIDGenerator;
import com.tc.object.msg.CommitTransactionMessage;

import java.io.IOException;

public final class CommitTransactionMessageToTransactionBatchReader implements TransactionBatchReaderFactory {

  private final GlobalTransactionIDGenerator gtxm;
  private final ServerTransactionFactory     activeTxnFactory  = new ActiveServerTransactionFactory();
  private final ServerTransactionFactory     passiveTxnFactory = new PassiveServerTransactionFactory();

  public CommitTransactionMessageToTransactionBatchReader(GlobalTransactionIDGenerator gtxm) {
    this.gtxm = gtxm;
  }

  // Used by active server
  public TransactionBatchReader newTransactionBatchReader(CommitTransactionMessage ctm) throws IOException {
    return new TransactionBatchReaderImpl(gtxm, ctm.getBatchData(), ctm.getClientID(), ctm.getSerializer(),
                                          activeTxnFactory);
  }

  // Used by passive server
  public TransactionBatchReader newTransactionBatchReader(RelayedCommitTransactionMessage ctm) throws IOException {
    return new TransactionBatchReaderImpl(ctm, ctm.getBatchData(), ctm.getClientID(), ctm.getSerializer(),
                                          passiveTxnFactory);
  }
}