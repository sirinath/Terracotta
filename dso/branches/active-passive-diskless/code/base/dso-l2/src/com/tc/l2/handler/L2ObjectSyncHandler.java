/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.context.ManagedObjectLookupForSyncContext;
import com.tc.l2.context.SyncObjectsRequest;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

import java.util.HashSet;
import java.util.Set;

public class L2ObjectSyncHandler extends AbstractEventHandler {

  private ReplicatedObjectManager replicatedObjectMgr;
  private final L2ObjectStateManager l2ObjectStateMgr;
  private ObjectManager objectManager;

  public L2ObjectSyncHandler(L2ObjectStateManager l2StateManager) {
    l2ObjectStateMgr = l2StateManager;
  }

  // Currently this has to be single threaded to maintain the correctness of objects and transactions
  public void handleEvent(EventContext context) {
    if(context instanceof SyncObjectsRequest) {
      SyncObjectsRequest request = (SyncObjectsRequest) context;
      doSyncObjects(request);
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }

  private void doSyncObjects(SyncObjectsRequest request) {
    NodeID nodeID = request.getNodeID();
    Set oids = new HashSet();
    boolean more = l2ObjectStateMgr.addAndRemoveSomeMissingOIDsTo(nodeID, oids, 500);
    ManagedObjectLookupForSyncContext lookupContext = new ManagedObjectLookupForSyncContext(nodeID, oids, more);
    // TODO:: Remove ChannelID from ObjectManager interface
    objectManager.lookupObjectsAndSubObjectsFor(ChannelID.NULL_ID, oids, lookupContext, -1);
    
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.replicatedObjectMgr = oscc.getL2Coordinator().getReplicatedObjectManager();
    this.objectManager = oscc.getObjectManager();
  }

}
