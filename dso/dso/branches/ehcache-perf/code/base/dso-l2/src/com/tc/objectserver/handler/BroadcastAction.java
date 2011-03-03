/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.tx.ServerTransactionManager;

public class BroadcastAction {

  private int                            count;
  private final ServerTransactionID      sid;
  private final ServerTransactionManager transactionManager;

  public BroadcastAction(int count, ServerTransactionID sid, ServerTransactionManager transactionManager) {
    this.count = count;
    this.sid = sid;
    this.transactionManager = transactionManager;
  }

  public synchronized boolean completed() {
    count--;
    if (count == 0) {
      transactionManager.broadcasted(sid.getChannelID(), sid.getClientTransactionID());
      return true;
    }
    return false;
  }

}
