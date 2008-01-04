/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.ConnectionPolicy;

import java.io.Serializable;
import java.util.HashSet;

public class TCGroupManager implements GroupManager, TCGroupMemberListener, TCGroupMessageListener {
  private static final TCLogger                   logger           = TCLogging.getLogger(TCGroupManager.class);
  private NodeID                                  thisNodeID;
  private final TCGroupMembership                 membership;
  private boolean                                 debug            = false;

  public TCGroupManager(L2TVSConfigurationSetupManager configSetupManager, ConnectionPolicy connectionPolicy)
      throws Exception {

    final TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger));

    membership = new TCGroupMembershipImpl(configSetupManager, connectionPolicy, threadGroup);
    membership.setDiscover(new TCGroupMemberDiscoveryStatic(configSetupManager));
    membership.start(new HashSet());
  }

  public NodeID getLocalNodeID() throws GroupException {
    if (this.thisNodeID == null) { throw new GroupException("Node hasnt joined the group yet !"); }
    return this.thisNodeID;
  }

  public NodeID join(Node thisNode, Node[] allNodes) throws GroupException {
    return (thisNodeID = membership.join(thisNode, allNodes));
  }

  public void registerForGroupEvents(GroupEventsListener listener) {
    membership.registerForGroupEvents(listener);
  }

  public void messageReceived(Serializable serializable, TCGroupMember member) {
    throw new RuntimeException("Match API but not to be called");
  }

  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    membership.registerForMessages(msgClass, listener);
  }

  public void routeMessages(Class msgClass, Sink sink) {
    registerForMessages(msgClass, new RouteGroupMessagesToSink(msgClass.getName(), sink));
  }

  public void sendAll(GroupMessage msg) throws GroupException {
    membership.sendAll(msg);
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException {
    return membership.sendAllAndWaitForResponse(msg);
  }

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException {
    membership.sendTo(node, msg);
  }

  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException {
    return membership.sendToAndWaitForResponse(nodeID, msg);
  }

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    membership.setZapNodeRequestProcessor(processor);
  }

  public void zapNode(NodeID nodeID, int type, String reason) {
    membership.zapNode(nodeID, type, reason);
  }

  public void memberAdded(TCGroupMember member) {
    throw new RuntimeException("Match API but not to be called");
  }

  public void memberDisappeared(TCGroupMember member) {
    throw new RuntimeException("Match API but not to be called");
  }
  


}
