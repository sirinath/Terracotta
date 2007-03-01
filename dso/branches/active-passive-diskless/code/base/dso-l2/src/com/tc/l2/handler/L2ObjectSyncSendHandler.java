/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.l2.context.SyncObjectsRequest;
import com.tc.l2.msg.TransactionMessage;
import com.tc.l2.msg.TransactionMessageFactory;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class L2ObjectSyncSendHandler extends AbstractEventHandler {

  private static final TCLogger      logger = TCLogging.getLogger(L2ObjectSyncSendHandler.class);

  private final L2ObjectStateManager objectStateManager;
  private GroupManager               groupManager;

  private Sink                       syncRequestSink;

  public L2ObjectSyncSendHandler(L2ObjectStateManager objectStateManager) {
    this.objectStateManager = objectStateManager;
  }

  public void handleEvent(EventContext context) {
    if (context instanceof ManagedObjectSyncContext) {
      ManagedObjectSyncContext mosc = (ManagedObjectSyncContext) context;
      if (sendObjects(mosc) && mosc.hasMore()) {
        syncRequestSink.add(new SyncObjectsRequest(mosc.getNodeID()));
      }
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }

  private boolean sendObjects(ManagedObjectSyncContext mosc) {
    objectStateManager.close(mosc);
    TransactionMessage msg = TransactionMessageFactory.createTransactionMessageFrom(mosc);
    try {
      this.groupManager.sendTo(mosc.getNodeID(), msg);
      logger.info("Sent " + mosc.getDNACount() + " objects to " + mosc.getNodeID());
      return true;
    } catch (GroupException e) {
      logger.error("Removing " + mosc.getNodeID() + " from group because of Exception :", e);
      groupManager.zapNode(mosc.getNodeID());
      return false;
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.groupManager = oscc.getL2Coordinator().getGroupManager();
    this.syncRequestSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_STAGE).getSink();
  }

}
