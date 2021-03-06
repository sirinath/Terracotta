/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.util.Collection;
import java.util.SortedSet;

public interface TransactionPersistor {
  
  public Collection loadAllGlobalTransactionDescriptors();
    
  public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx);

  public void deleteAllGlobalTransactionDescriptors(PersistenceTransaction tx, SortedSet<ServerTransactionID> toDelete);

}
