/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.context.ManagedObjectSyncContext;

public class TransactionMessageFactory {

  public static TransactionMessage createTransactionMessageFrom(ManagedObjectSyncContext mosc) {
    TransactionMessage msg = new TransactionMessage(TransactionMessage.MANAGED_OBJECT_SYNC_TYPE);
    msg.initialize(mosc.getOIDs(), mosc.getDNACount(), mosc.getSerializedDNAs(), mosc.getObjectSerializer());
    return msg;
  }

}
