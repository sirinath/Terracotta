/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.hooks;

public interface StatsListener {

  void beginLockAquire(String lockID);
  
  void endLockAquire(String lockID);
  
  void beginTransactionCommit(String lockID);
  
  void endTransactionCommit(String lockID);
  
  void beginObjectFault(int size);
  
  void endObjectFault(int size);
 
}
