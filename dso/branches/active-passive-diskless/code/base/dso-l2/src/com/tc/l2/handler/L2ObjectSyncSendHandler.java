/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class L2ObjectSyncSendHandler extends AbstractEventHandler {

  private ReplicatedObjectManager replicatedObjectMgr;
  private final L2ObjectStateManager objectStateManager;


  public L2ObjectSyncSendHandler(L2ObjectStateManager objectStateManager) {
    this.objectStateManager = objectStateManager;
  }

  public void handleEvent(EventContext context) {
    if (context instanceof ManagedObjectSyncContext) {
      ManagedObjectSyncContext mosc = (ManagedObjectSyncContext) context;
      sendObjects(mosc);
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }

  private void sendObjects(ManagedObjectSyncContext mosc) {
    objectStateManager.close(mosc);
  }


  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.replicatedObjectMgr = oscc.getL2Coordinator().getReplicatedObjectManager();
  }

}
