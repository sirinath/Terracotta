/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.net.GroupID;

public interface TransactionBatchFactory {
  
  public ClientTransactionBatch nextBatch(GroupID groupID);
  
  public boolean isFoldingSupported();

}
