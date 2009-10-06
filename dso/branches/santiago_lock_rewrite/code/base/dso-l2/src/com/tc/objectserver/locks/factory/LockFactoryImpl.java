/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.factory;

import com.tc.object.locks.LockID;
import com.tc.objectserver.locks.ServerLock;
import com.tc.objectserver.locks.LockFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class LockFactoryImpl implements LockFactory {
  private final static boolean LOCK_LEASE_ENABLE = TCPropertiesImpl
                                                     .getProperties()
                                                     .getBoolean(TCPropertiesConsts.L2_LOCKMANAGER_GREEDY_LEASE_ENABLED);
  private final LockFactory    factory;

  public LockFactoryImpl() {
    // if (false) {
    if (LOCK_LEASE_ENABLE) {
      factory = new GreedyPolicyFactory();
    } else {
      factory = new NonGreedyLockPolicyFactory();
    }
  }

  public ServerLock createLock(LockID lid) {
    return factory.createLock(lid);
  }
}
