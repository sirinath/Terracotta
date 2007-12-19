/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.ImplementMe;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.ConnectionPolicy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TCGroupManager implements GroupManager {
  private static final TCLogger                   logger           = TCLogging.getLogger(TCGroupManager.class);
  private NodeID                                  thisNodeID;
  private final TCGroupMembership                 membership;
  private final Map<String, GroupMessageListener> messageListeners = new ConcurrentHashMap<String, GroupMessageListener>();

  public TCGroupManager(L2TVSConfigurationSetupManager configSetupManager, ConnectionPolicy connectionPolicy) {
    
    final TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger));

    membership = new TCGroupMembershipImpl(configSetupManager, connectionPolicy, threadGroup);
  }

  public NodeID getLocalNodeID() throws GroupException {
    if (this.thisNodeID == null) { throw new GroupException("Node hasnt joined the group yet !"); }
    return this.thisNodeID;
  }

  public NodeID join(Node thisNode, Node[] allNodes) throws GroupException {
    return (membership.join(thisNode, allNodes));
  }

  public void registerForGroupEvents(GroupEventsListener listener) {
    throw new ImplementMe();
  }

  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    GroupMessageListener prev = messageListeners.put(msgClass.getName(), listener);
    if (prev != null) {
      logger.warn("Previous listener removed : " + prev);
    }
  }

  public void routeMessages(Class msgClass, Sink sink) {
    registerForMessages(msgClass, new RouteGroupMessagesToSink(msgClass.getName(), sink));
  }

  public void sendAll(GroupMessage msg) throws GroupException {
    membership.sendAll(msg);
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException {
    throw new ImplementMe();
  }

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException {
    membership.sendTo(node, msg);
  }

  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException {
    throw new ImplementMe();
  }

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    throw new ImplementMe();
  }

  public void zapNode(NodeID nodeID, int type, String reason) {
    throw new ImplementMe();
  }

}
