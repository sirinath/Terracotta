/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class VirtualTCGroupManagerImpl implements GroupManager, GroupEventsListener, GroupMessageListener {
  private static final TCLogger                           logger           = TCLogging
                                                                               .getLogger(VirtualTCGroupManagerImpl.class);
  private final GroupManager                        groupManager;
  private final CopyOnWriteArrayList<GroupEventsListener> groupListeners   = new CopyOnWriteArrayList<GroupEventsListener>();
  private final Map<String, GroupMessageListener>         messageListeners = new ConcurrentHashMap<String, GroupMessageListener>();
  private final Set<NodeID>                               groupNodeIDs     = new CopyOnWriteArraySet<NodeID>();
  private final Set<String>                               groupNodes       = new HashSet<String>();

  public VirtualTCGroupManagerImpl(GroupManager groupManager, Node[] thisGroupNodes) {
    this.groupManager = groupManager;
    for (Node n : thisGroupNodes) {
      groupNodes.add(n.getServerNodeName());
    }
    groupManager.registerForGroupEvents(this);
  }

  public NodeID getLocalNodeID() throws GroupException {
    return groupManager.getLocalNodeID();
  }

  public NodeID join(Node thisNode, Node[] allNodes) throws GroupException {
    return groupManager.join(thisNode, allNodes);
  }

  public void registerForGroupEvents(GroupEventsListener listener) {
    groupListeners.add(listener);
  }

  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    messageListeners.put(msgClass.getName(), listener);
    groupManager.registerForMessages(msgClass, this);
  }

  public void messageReceived(NodeID from, GroupMessage msg) {
    if (!isThisGroup(from)) return;
    GroupMessageListener listener = messageListeners.get(msg.getClass().getName());
    if (listener != null) {
      listener.messageReceived(from, msg);
    } else {
      String errorMsg = "No Route for " + msg + " from " + from;
      logger.error(errorMsg);
      throw new AssertionError(errorMsg);
    }
  }

  public void routeMessages(Class msgClass, Sink sink) {
    registerForMessages(msgClass, new RouteGroupMessagesToSink(msgClass.getName(), sink));
  }

  public void sendAll(GroupMessage msg) {
    groupManager.sendAll(msg, groupNodeIDs);
  }
  
  public void sendAll(GroupMessage msg, Set nodeIDs) {
    groupManager.sendAll(msg, nodeIDs);
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException {
    return groupManager.sendAllAndWaitForResponse(msg, groupNodeIDs);
  }
  
  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg, Set nodeIDs) throws GroupException {
    return groupManager.sendAllAndWaitForResponse(msg, nodeIDs);
  }

  public void sendTo(NodeID nodeID, GroupMessage msg) throws GroupException {
    Assert.assertTrue(isThisGroup(nodeID));
    groupManager.sendTo(nodeID, msg);
  }

  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException {
    Assert.assertTrue(isThisGroup(nodeID));
    return groupManager.sendToAndWaitForResponse(nodeID, msg);
  }

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    groupManager.setZapNodeRequestProcessor(processor);
  }

  public void zapNode(NodeID nodeID, int type, String reason) {
    Assert.assertTrue(isThisGroup(nodeID));
    groupManager.zapNode(nodeID, type, reason);
  }

  public void nodeJoined(NodeID nodeID) {
    if (!isThisGroup(nodeID)) return;
    groupNodeIDs.add(nodeID);
    fireNodeEvent(nodeID, true);
  }

  public void nodeLeft(NodeID nodeID) {
    if (!isThisGroup(nodeID)) return;
    groupNodeIDs.remove(nodeID);
    fireNodeEvent(nodeID, false);
  }
  
  /*
   * for testing purpose only
   */
  protected GroupManager getBaseTCGroupManager() {
    return groupManager;
  }

  private boolean isThisGroup(NodeID nodeID) {
    Assert.assertTrue(nodeID instanceof ServerID);
    ServerID serverID = (ServerID) nodeID;
    return groupNodes.contains(serverID.getName());
  }

  private void fireNodeEvent(NodeID nodeID, boolean joined) {
    if (logger.isDebugEnabled()) logger.debug("VirtualTCGroupManager fireNodeEvent: joined = " + joined + ", node = " + nodeID);
    for (GroupEventsListener listener : groupListeners) {
      if (joined) {
        listener.nodeJoined(nodeID);
      } else {
        listener.nodeLeft(nodeID);
      }
    }
  }

}
