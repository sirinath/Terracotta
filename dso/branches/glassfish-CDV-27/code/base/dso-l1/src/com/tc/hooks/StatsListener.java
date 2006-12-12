/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.hooks;

public abstract class StatsListener {

  public abstract void lockAquire(String lockID, long startTime);
  
  public abstract void transactionCommit();
  
  public abstract void objectFault();
  
  protected long currentTime() {
    return System.currentTimeMillis();
  }
}
