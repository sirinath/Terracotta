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
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionalObjectManager;

import java.util.ArrayList;
import java.util.Collections;

public class L2ObjectSyncHandler extends AbstractEventHandler {

  private final L2ObjectStateManager l2ObjectStateMgr;
  private ObjectManager              objectManager;
  private TransactionalObjectManager txnObjectMgr;
  private Sink dehydrateSink;

  public L2ObjectSyncHandler(L2ObjectStateManager l2StateManager) {
    l2ObjectStateMgr = l2StateManager;
  }

  public void handleEvent(EventContext context) {
    if (context instanceof SyncObjectsRequest) {
      SyncObjectsRequest request = (SyncObjectsRequest) context;
      doSyncObjectsRequest(request);
    } else if( context instanceof ObjectSyncMessage) {
      ObjectSyncMessage syncMsg = (ObjectSyncMessage) context;
      doSyncObjectsResponse(syncMsg);
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }
  
  
  private void doSyncObjectsResponse(ObjectSyncMessage syncMsg) {
    ArrayList txns = new ArrayList(1);
    ServerTransaction txn = ServerTransactionFactory.createTxnFrom(syncMsg);
    txns.add(txn);
    txnObjectMgr.addTransactions(ChannelID.NULL_ID, txns, Collections.EMPTY_LIST);
  }

  //TODO:: Update stats so that admin console reflects these data
  private void doSyncObjectsRequest(SyncObjectsRequest request) {
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
    this.objectManager = oscc.getObjectManager();
    this.txnObjectMgr = oscc.getTransactionalObjectManager();
    this.dehydrateSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_DEHYDRATE_STAGE).getSink();
  }

}
