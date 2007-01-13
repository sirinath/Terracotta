/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles.instrumentation;

import com.tc.config.lock.LockLevel;

public class LockSpec {
  public static final String TC_AUTOLOCK_NAME = "tc:autolock";
  
  private final String lockName;
  private final String methodJoinPointExpression;
  private final LockLevel lockLevel;
  
  public LockSpec(String methodJoinPointExpression, LockLevel lockLevel) {
    this.lockName = TC_AUTOLOCK_NAME;
    this.methodJoinPointExpression = methodJoinPointExpression;
    this.lockLevel = lockLevel;
  }
  
  public LockSpec(String lockName, String methodJoinPointExpression, LockLevel lockLevel) {
    this.lockName = lockName;
    this.methodJoinPointExpression = methodJoinPointExpression;
    this.lockLevel = lockLevel;
  }

  public LockLevel getLockLevel() {
    return lockLevel;
  }

  public String getLockName() {
    return lockName;
  }

  public String getMethodJoinPointExpression() {
    return methodJoinPointExpression;
  }
  
}
