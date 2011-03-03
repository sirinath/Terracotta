/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collections;
import java.util.List;

public class TestClientGlobalTransactionManager implements ClientGlobalTransactionManager {

  public final NoExceptionLinkedQueue pauseCalls                     = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue startingCalls                  = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue unpauseCalls                   = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue resendOutstandingCalls         = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue flushCalls                     = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue getTransactionSequenceIDsCalls = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue getTransactionIDsCalls         = new NoExceptionLinkedQueue();
  public List                         transactionSequenceIDs;

  public void setLowWatermark(GlobalTransactionID lowWatermark) {
    throw new ImplementMe();
  }

  public void flush(LockID lockID) {
    flushCalls.put(lockID);
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    throw new ImplementMe();
  }

  public void unpause() {
    unpauseCalls.put(new Object());
  }

  public void starting() {
    startingCalls.put(new Object());
  }

  public void pause() {
    pauseCalls.put(new Object());
  }

  public void resendOutstanding() {
    resendOutstandingCalls.put(new Object());
  }

  public List getTransactionSequenceIDs() {
    this.getTransactionSequenceIDsCalls.put(new Object());
    return transactionSequenceIDs;
  }

  public boolean startApply(NodeID nodeID, TransactionID transactionID, GlobalTransactionID globalTransactionID) {
    throw new ImplementMe();
  }

  public int size() {
    throw new ImplementMe();
  }

  public List getResentTransactionIDs() {
    this.getTransactionIDsCalls.put(new Object());
    return Collections.EMPTY_LIST;
  }

  public void resendOutstandingAndUnpause() {
    resendOutstanding();
    unpause();
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    return true;
  }
}
