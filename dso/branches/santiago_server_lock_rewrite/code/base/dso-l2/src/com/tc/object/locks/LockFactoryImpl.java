/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class LockFactoryImpl implements LockFactory {
  private final static boolean LOCK_LEASE_ENABLE = TCPropertiesImpl
                                                     .getProperties()
                                                     .getBoolean(TCPropertiesConsts.L2_LOCKMANAGER_GREEDY_LEASE_ENABLED);
  private final LockFactory    factory;

  public LockFactoryImpl() {
    // if(false) {
    if (LOCK_LEASE_ENABLE) {
      System.out.println("using greedy factory");
      factory = new GreedyLockFactory();
    } else {
      System.out.println("using non greedy factory");
      factory = new NonGreedyLockFactory();
    }
  }

  public Lock createLock(LockID lid) {
    return factory.createLock(lid);
  }

  private static class GreedyLockFactory implements LockFactory {
    public Lock createLock(LockID lid) {
      return new ServerLock(lid);
    }
  }

  private static class NonGreedyLockFactory implements LockFactory {
    public Lock createLock(LockID lid) {
      return new NonGreedyPolicyLock(lid);
    }
  }
}
