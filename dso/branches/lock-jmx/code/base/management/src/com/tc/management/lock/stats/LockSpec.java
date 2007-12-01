/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.object.lockmanager.api.LockID;

import java.util.Collection;

public interface LockSpec {
  public LockID getLockID();
  
  public String getObjectType();
  
  public LockStats getStats();
  
  public Collection children();
  
}
