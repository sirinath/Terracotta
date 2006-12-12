/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.hooks;

public interface StatsListener {

  void lockAquire(String lockID, long startTime, long endTime);
  
  void transactionCommit(String lockID, long startTime, long endTime);
  
  void objectFault(int size);
 
}
