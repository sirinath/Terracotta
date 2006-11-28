/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ImplementMe;
import com.tc.management.beans.tx.ClientTxMonitorMBean;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TestClientTransaction implements ClientTransaction {

  public TransactionID txID;
  public LockID        lockID;
  public TxnType       txnType;
  private boolean      hasChangesOrNotifies = true;
  public Collection    allLockIDs           = new HashSet();
  public SequenceID    sequenceID;
  public Map           newRoots             = new HashMap();
  public Map           changeBuffers        = new HashMap();

  public TestClientTransaction() {
    super();
  }

  public Map getChangeBuffers() {
    return changeBuffers;
  }

  public Map getNewRoots() {
    return newRoots;
  }

  public LockID getLockID() {
    return lockID;
  }

  public LockID[] getAllLockIDs() {
    return (LockID[]) allLockIDs.toArray(new LockID[allLockIDs.size()]);
  }

  public TransactionID getTransactionID() {
    return txID;
  }

  public void createObject(TCObject source) {
    throw new ImplementMe();
  }

  public void createRoot(String name, ObjectID rootID) {
    throw new ImplementMe();
  }

  public boolean holdsLock(LockID lid) {
    throw new ImplementMe();
  }

  public ClientTransaction getNextTransaction() {
    throw new ImplementMe();
  }

  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    throw new ImplementMe();
  }

  public void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName) {
    throw new ImplementMe();
  }

  public boolean hasChangesOrNotifies() {
    return this.hasChangesOrNotifies;
  }

  public boolean isNull() {
    throw new ImplementMe();
  }

  public TxnType getTransactionType() {
    return txnType;
  }

  public void addNotify(Notify notify) {
    throw new ImplementMe();
  }

  public List addNotifiesTo(List notifies) {
    return notifies;
  }

  public void setSequenceID(SequenceID sequenceID) {
    return;
  }

  public SequenceID getSequenceID() {
    return this.sequenceID;
  }

  public boolean isConcurrent() {
    return false;
  }

  public void setTransactionContext(TransactionContext transactionContext) {
    throw new ImplementMe();
  }

  public boolean isAlreadyCommitted() {
    throw new ImplementMe();
  }

  public void setAlreadyCommitted(boolean alreadyCommittedFlag) {
    throw new ImplementMe();
  }

  public void readOnlyCheck() {
    throw new ImplementMe();

  }

  public boolean hasChanges() {
    throw new ImplementMe();
  }

  public int getNotifiesCount() {
    throw new ImplementMe();
  }

  public void arrayChanged(TCObject source, int startPos, Object array, int length) {
    throw new ImplementMe();
  }

  public void updateMBean(ClientTxMonitorMBean txMBean) {
    throw new ImplementMe();
  }

  public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    throw new ImplementMe();
  }

}
