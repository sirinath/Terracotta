/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.object.msg.CommitTransactionMessage;

import java.util.Collection;
import java.util.List;

public interface ReplicatedObjectManager {

  /**
   * This method is used to sync up all ObjectIDs from the remote ObjectManagers. It is synchronous and after when it
   * returns nobody is allowed to join the cluster with exisiting objects.
   */
  public void sync();

  public void incomingTransactions(CommitTransactionMessage ctm, List txns, Collection serverTxnIDs, Collection completedTxnIds);

}