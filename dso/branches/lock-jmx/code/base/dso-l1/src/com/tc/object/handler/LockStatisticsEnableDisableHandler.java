/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.lockmanager.api.ClientLockManager;

public class LockStatisticsEnableDisableHandler extends AbstractEventHandler {
  private static final TCLogger logger = TCLogging.getLogger(LockStatisticsEnableDisableHandler.class);
  private ClientLockManager     lockManager;

  public void handleEvent(EventContext context) {
    LockStatisticsMessage msg = (LockStatisticsMessage) context;
    lockManager.setLockStatisticsConfig(msg.getTraceDepth(), msg.getGatherInterval());
  }
  
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.lockManager = ccc.getLockManager();
  }

}
