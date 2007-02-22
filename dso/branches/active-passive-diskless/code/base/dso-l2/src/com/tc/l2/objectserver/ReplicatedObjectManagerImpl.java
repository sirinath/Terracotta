/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.msg.ObjectListSyncMessage;
import com.tc.l2.msg.ObjectListSyncMessageFactory;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.GroupResponse;
import com.tc.net.groups.NodeID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.util.Assert;

import java.util.Iterator;
import java.util.Set;

public class ReplicatedObjectManagerImpl implements ReplicatedObjectManager, GroupEventsListener, GroupMessageListener {

  private static final TCLogger      logger = TCLogging.getLogger(ReplicatedObjectManagerImpl.class);

  private final ObjectManager        objectManager;
  private final GroupManager         groupManager;
  private final StateManager         stateManager;
  private final L2ObjectStateManager l2ObjectStateManager;

  public ReplicatedObjectManagerImpl(GroupManager groupManager, StateManager stateManager, ObjectManager objectManager) {
    this.groupManager = groupManager;
    this.stateManager = stateManager;
    this.objectManager = objectManager;
    this.l2ObjectStateManager = new L2ObjectStateManager();
    this.groupManager.registerForGroupEvents(this);
    this.groupManager.registerForMessages(ObjectListSyncMessage.class, this);
  }

  /**
   * This method is used to sync up all ObjectIDs from the remote ObjectManagers. It is synchronous and after when it
   * returns nobody is allowed to join the cluster with exisiting objects.
   */
  public void sync() {
    try {
      GroupResponse gr = groupManager.sendAllAndWaitForResponse(ObjectListSyncMessageFactory
          .createObjectListSyncRequestMessage());
      for (Iterator i = gr.getResponses().iterator(); i.hasNext();) {
        ObjectListSyncMessage msg = (ObjectListSyncMessage) i.next();
        add2L2StateManager(msg.messageFrom(), msg.getObjectIDs());
      }
    } catch (GroupException e) {
      logger.error(e);
      throw new AssertionError(e);
    }
  }

  public void nodeJoined(NodeID nodeID) {
    if (stateManager.isActiveCoordinator()) {
      query(nodeID);
    }
  }

  private void query(NodeID nodeID) {
    // Query current state of the other L2
    try {
      groupManager.sendTo(nodeID, ObjectListSyncMessageFactory.createObjectListSyncRequestMessage());
    } catch (GroupException e) {
      logger.error("Error Writting Msg : ", e);
    }
  }

  public void nodeLeft(NodeID nodeID) {
    if (stateManager.isActiveCoordinator()) {
      l2ObjectStateManager.removeL2(nodeID);
    }
  }

  public void messageReceived(NodeID fromNode, GroupMessage msg) {
    if (!(msg instanceof ObjectListSyncMessage)) { throw new AssertionError(
                                                                            "ReplicatedObjectManagerImpl : Received wrong message type :"
                                                                                + msg); }
    ObjectListSyncMessage clusterMsg = (ObjectListSyncMessage) msg;
    handleClusterObjectMessage(fromNode, clusterMsg);
  }

  private void handleClusterObjectMessage(NodeID nodeID, ObjectListSyncMessage clusterMsg) {
    try {
      switch (clusterMsg.getType()) {
        case ObjectListSyncMessage.REQUEST:
          handleObjectListRequest(nodeID, clusterMsg);
          break;
        case ObjectListSyncMessage.RESPONSE:
          handleObjectListResponse(nodeID, clusterMsg);
          break;

        default:
          throw new AssertionError("This message shouldn't have been routed here : " + clusterMsg);
      }
    } catch (GroupException e) {
      logger.error("Error handling message : " + clusterMsg, e);
      throw new AssertionError(e);
    }
  }

  private void handleObjectListResponse(NodeID nodeID, ObjectListSyncMessage clusterMsg) {
    Assert.assertTrue(stateManager.isActiveCoordinator());
    Set oids = clusterMsg.getObjectIDs();
    if (!oids.isEmpty()) {
      logger.error("Nodes joining the cluster after startup shouldnt have any Objects. " + nodeID + " contains "
                   + oids.size() + " Objects !!!");
      logger.error("TODO:: Force remote node to Quit !!");
      // TODO:: Force Quit !!! ACTIVE NEVER QUITS
    } else {
      add2L2StateManager(nodeID, oids);
    }
  }

  private void add2L2StateManager(NodeID nodeID, Set oids) {
      int missing = l2ObjectStateManager.setExistingObjectsList(nodeID, oids, objectManager);
      if (missing == 0) {
        stateManager.moveNodeToPassiveStandby(nodeID);
      }
    // TODO:: initiate lookups
  }

  private void handleObjectListRequest(NodeID nodeID, ObjectListSyncMessage clusterMsg) throws GroupException {
    Assert.assertFalse(stateManager.isActiveCoordinator());
    Set knownIDs = objectManager.getAllObjectIDs();
    groupManager.sendTo(nodeID, ObjectListSyncMessageFactory.createObjectListSyncResponseMessage(clusterMsg, knownIDs));
  }

}
