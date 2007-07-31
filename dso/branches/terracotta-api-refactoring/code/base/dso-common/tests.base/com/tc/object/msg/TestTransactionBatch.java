/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.ITCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.object.tx.TransactionBatch;

import java.util.Collection;

public class TestTransactionBatch implements TransactionBatch {

  private final ITCByteBuffer[] batchData;
  private final Collection     acknowledged;

  public TestTransactionBatch(ITCByteBuffer[] batchData, Collection acknowledged) {
    this.batchData = batchData;
    this.acknowledged = acknowledged;
  }

  public Collection getAcknowledgedTransactionIDs() {
    return this.acknowledged;
  }

  public boolean isEmpty() {
    throw new ImplementMe();
  }

  public ITCByteBuffer[] getData() {
    return batchData;
  }

  public void recycle() {
    return;
  }

}
