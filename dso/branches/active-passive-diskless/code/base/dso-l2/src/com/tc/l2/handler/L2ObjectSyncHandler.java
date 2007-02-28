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
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class L2ObjectSyncHandler extends AbstractEventHandler {

  private ReplicatedObjectManager    replicatedObjectMgr;
  private final L2ObjectStateManager l2ObjectStateMgr;
  private ObjectManager              objectManager;
  private Sink dehydrateSink;

  public L2ObjectSyncHandler(L2ObjectStateManager l2StateManager) {
    l2ObjectStateMgr = l2StateManager;
  }

  // Currently this has to be single threaded to maintain the correctness of objects and transactions
  public void handleEvent(EventContext context) {
    if (context instanceof SyncObjectsRequest) {
      SyncObjectsRequest request = (SyncObjectsRequest) context;
      doSyncObjects(request);
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }
  
  
  //TODO:: Update stats so that admin console reflects these data
  private void doSyncObjects(SyncObjectsRequest request) {
    NodeID nodeID = request.getNodeID();
    ManagedObjectSyncContext lookupContext = l2ObjectStateMgr.getSomeObjectsToSyncContext(nodeID, 500, dehydrateSink);
    // TODO:: Remove ChannelID from ObjectManager interface
    if (lookupContext != null) {
      objectManager.lookupObjectsAndSubObjectsFor(ChannelID.NULL_ID, lookupContext.getOIDs(), lookupContext, -1);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.replicatedObjectMgr = oscc.getL2Coordinator().getReplicatedObjectManager();
    this.objectManager = oscc.getObjectManager();
    this.dehydrateSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_DEHYDRATE_STAGE).getSink();
  }

}
