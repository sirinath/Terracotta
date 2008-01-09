/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.TCGroupNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCGroupCommunicationsManagerImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handler.ReceiveGroupMessageHandler;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.sequence.SimpleSequence;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TCGroupManagerImpl extends SEDA implements TCGroupManager, ChannelManagerEventListener,
    TCGroupMemberListener {
  private boolean                                         debug                   = false;
  private static final TCLogger                           logger                  = TCLogging
                                                                                      .getLogger(TCGroupManagerImpl.class);
  private final NodeID                                    thisNodeID;

  private final L2TVSConfigurationSetupManager            configSetupManager;
  private TCProperties                                    l2Properties;
  private CommunicationsManager                           communicationsManager;
  private NetworkListener                                 groupListener;
  private final ConnectionPolicy                          connectionPolicy;
  private TCGroupMemberDiscovery                          discover;
  private ArrayList<TCGroupMember>                        members                 = new ArrayList<TCGroupMember>();
  private final CopyOnWriteArrayList<GroupEventsListener> groupListeners          = new CopyOnWriteArrayList<GroupEventsListener>();
  private final Map<String, GroupMessageListener>         messageListeners        = new ConcurrentHashMap<String, GroupMessageListener>();
  private final Map<MessageID, GroupResponse>             pendingRequests         = new Hashtable<MessageID, GroupResponse>();
  private ZapNodeRequestProcessor                         zapNodeRequestProcessor = new DefaultZapNodeRequestProcessor(
                                                                                                                       logger);
  private boolean                                         isStopped               = false;
  private Stage                                           hydrateStage;
  private Stage                                           receiveGroupMessageStage;

  /*
   * Setup a communication manager which can establish channel from either sides.
   */
  public TCGroupManagerImpl(L2TVSConfigurationSetupManager configSetupManager, ConnectionPolicy connectionPolicy,
                            TCThreadGroup threadGroup) throws IOException {
    super(threadGroup);
    this.configSetupManager = configSetupManager;
    this.connectionPolicy = connectionPolicy;

    l2Properties = TCPropertiesImpl.getProperties().getPropertiesFor("l2");

    // members = new TCGroupMemberDiscoveryStatic(configSetupManager);

    this.configSetupManager.commonl2Config().changesInItemIgnored(configSetupManager.commonl2Config().dataPath());
    NewL2DSOConfig l2DSOConfig = configSetupManager.dsoL2Config();

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.l2GroupPort());
    int groupPort = l2DSOConfig.l2GroupPort().getInt();

    thisNodeID = init(l2DSOConfig.host().getString(), groupPort, l2Properties.getInt("tccom.workerthreads"));

    setDiscover(new TCGroupMemberDiscoveryStatic(configSetupManager));
    start(new HashSet());
  }

  /*
   * for testing purpose only
   */
  public TCGroupManagerImpl(ConnectionPolicy connectionPolicy, String hostname, int groupPort, int workerThreads,
                            TCThreadGroup threadGroup) throws IOException {
    super(threadGroup);
    this.configSetupManager = null;
    this.connectionPolicy = connectionPolicy;
    thisNodeID = init(hostname, groupPort, workerThreads);
  }

  private NodeID init(String hostname, int groupPort, int workerThreads) throws IOException {

    String nodeName = hostname + ":" + groupPort;
    logger.info("Creating group node: " + nodeName);
    NodeID aNodeID = new NodeIdUuidImpl(nodeName);

    final NetworkStackHarnessFactory networkStackHarnessFactory = new TCGroupNetworkStackHarnessFactory();
    communicationsManager = new TCGroupCommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                                 null, this.connectionPolicy, workerThreads, aNodeID);

    groupListener = communicationsManager.createListener(new NullSessionManager(),
                                                         new TCSocketAddress(TCSocketAddress.WILDCARD_ADDR, groupPort),
                                                         true, new DefaultConnectionIdFactory());
    // Listen to channel creation/removal
    groupListener.getChannelManager().addEventListener(this);

    int maxStageSize = 5000;
    StageManager stageManager = getStageManager();
    hydrateStage = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK, new HydrateHandler(), 1,
                                            maxStageSize);
    receiveGroupMessageStage = stageManager.createStage(ServerConfigurationContext.RECEIVE_GROUP_MESSAGE_STAGE,
                                                        new ReceiveGroupMessageHandler(this), 1, maxStageSize);

    groupListener.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);

    groupListener.routeMessageType(TCMessageType.GROUP_WRAPPER_MESSAGE, receiveGroupMessageStage.getSink(),
                                   hydrateStage.getSink());

    ConfigurationContext context = new ConfigurationContextImpl(stageManager);

    stageManager.startAll(context);

    registerForMessages(GroupZapNodeMessage.class, new ZapNodeRequestRouter());

    return (aNodeID);
  }

  public NodeID getLocalNodeID() throws GroupException {
    if (this.thisNodeID == null) { throw new GroupException("Node hasnt joined the group yet !"); }
    return this.thisNodeID;
  }

  public void start(Set initialConnectionIDs) throws IOException {
    groupListener.start(initialConnectionIDs);
    isStopped = false;
  }

  public void stop(long timeout) throws TCTimeoutException {
    isStopped = true;
    getStageManager().stopAll();
    discover.stop();
    groupListener.stop(timeout);
  }

  public void registerForGroupEvents(GroupEventsListener listener) {
    groupListeners.add(listener);
  }

  private void fireNodeEvent(NodeID newNode, boolean joined) {
    if (debug) {
      logger.info("fireNodeEvent: joined = " + joined + ", node = " + newNode);
    }
    Iterator<GroupEventsListener> i = groupListeners.iterator();
    while (i.hasNext()) {
      GroupEventsListener listener = i.next();
      if (joined) {
        listener.nodeJoined(newNode);
      } else {
        listener.nodeLeft(newNode);
      }
    }
  }

  public void memberAdded(TCGroupMember member) {
    if (isStopped) return;

    // Keep only one connection between two nodes. Close the redundant one.
    for (int i = 0; i < members.size(); ++i) {
      TCGroupMember m = members.get(i);
      if (member.getSrcNodeID().equals(m.getDstNodeID()) && member.getDstNodeID().equals(m.getSrcNodeID())) {
        // already one connection established, choose one to keep
        int order = member.getSrcNodeID().compareTo(member.getDstNodeID());
        if (order > 0) {
          // choose new connection
          m.close();
          // members.remove(m);
        } else if (order < 0) {
          // keep original one
          member.close();
          return;
        } else {
          throw new RuntimeException("SrcNodeID equals DstNodeID");
        }
      }
    }
    member.setTCGroupManager(this);
    members.add(member);
    fireNodeEvent(member.getNodeID(), true);
  }

  public boolean isExist(TCGroupMember member) {
    return (members.contains(member));
  }

  public NodeID join(Node thisNode, Node[] allNodes) throws GroupException {
    discover.setLocalNode(thisNode);
    discover.start();

    return (getNodeID());
  }

  public void memberDisappeared(TCGroupMember member) {
    if (isStopped || (member == null)) return;
    members.remove(member);
    fireNodeEvent(member.getNodeID(), false);
  }

  public void remove(MessageChannel channel) {
    members.remove(getMember(channel));
  }

  public void sendAll(GroupMessage msg) {
    for (int i = 0; i < members.size(); ++i) {
      TCGroupMember member = members.get(i);
      member.send(msg);
    }
  }

  public void sendTo(NodeID node, GroupMessage msg) {
    // find the member
    TCGroupMember member = getMember(node);
    Assert.assertTrue(member != null);
    member.send(msg);
  }

  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException {
    if (debug) {
      logger.info(getNodeID() + " : Sending to " + nodeID + " and Waiting for Response : " + msg.getMessageID());
    }
    GroupResponseImpl groupResponse = new GroupResponseImpl();
    MessageID msgID = msg.getMessageID();
    TCGroupMember m = (TCGroupMember) getMember(nodeID);
    if (m != null) {
      GroupResponse old = pendingRequests.put(msgID, groupResponse);
      Assert.assertNull(old);
      groupResponse.sendTo(m, msg);
      groupResponse.waitForResponses();
      pendingRequests.remove(msgID);
    } else {
      String errorMsg = "Node " + nodeID + " not present in the group. Ignoring Message : " + msg;
      logger.error(errorMsg);
      throw new GroupException(errorMsg);
    }
    return groupResponse.getResponse(nodeID);

  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException {
    if (debug) {
      logger.info(getNodeID() + " : Sending to ALL and Waiting for Response : " + msg.getMessageID());
    }
    GroupResponseImpl groupResponse = new GroupResponseImpl();
    MessageID msgID = msg.getMessageID();
    GroupResponse old = pendingRequests.put(msgID, groupResponse);
    Assert.assertNull(old);
    groupResponse.sendAll(this, msg);
    groupResponse.waitForResponses();
    pendingRequests.remove(msgID);
    return groupResponse;
  }

  public TCGroupMember openChannel(ConnectionAddressProvider addrProvider) throws TCTimeoutException,
      UnknownHostException, MaxConnectionsExceededException, IOException {
    ClientMessageChannel channel = communicationsManager
        .createClientChannel(new SessionManagerImpl(new SimpleSequence()), -1, null, -1, 10000, addrProvider);

    channel.open();
    channel.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);
    channel.routeMessageType(TCMessageType.GROUP_WRAPPER_MESSAGE, receiveGroupMessageStage.getSink(), hydrateStage
        .getSink());

    logger.debug("Channel setup to " + channel.getChannelID().getNodeID());
    TCGroupMember member = new TCGroupMemberImpl(getNodeID(), channel);
    memberAdded(member);
    return member;
  }

  public TCGroupMember openChannel(String hostname, int groupPort) throws TCTimeoutException, UnknownHostException,
      MaxConnectionsExceededException, IOException {
    return openChannel(new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(hostname, groupPort) }));
  }

  public void closeChannel(TCGroupMember member) {
    member.close();
  }

  public void closeChannel(MessageChannel channel) {
    closeChannel(getMember(channel));
  }

  /*
   * Event notification when a new connection setup by channelManager
   */
  public void channelCreated(MessageChannel channel) {
    if (isStopped) return;
    logger.debug("Channel established from " + channel.getChannelID().getNodeID());
    memberAdded(new TCGroupMemberImpl(channel, getNodeID()));
  }

  /*
   * Event notification when a connection removed by DSOChannelManager
   */
  public void channelRemoved(MessageChannel channel) {
    if (isStopped) return;
    logger.debug("Channel removed from " + channel.getChannelID().getNodeID());
    memberDisappeared(getMember(channel));
  }

  public void closeAllChannels() {
    ArrayList<TCGroupMember> tmpList = new ArrayList<TCGroupMember>(members);
    for (int i = 0; i < tmpList.size(); ++i) {
      closeChannel(tmpList.get(i));
    }
  }

  public NodeID getNodeID() {
    return thisNodeID;
  }

  public TCGroupMember getMember(MessageChannel channel) {
    TCGroupMember member = null;
    for (int i = 0; i < members.size(); ++i) {
      TCGroupMember m = members.get(i);
      if (m.getChannel() == channel) {
        member = m;
        break;
      }
    }
    return (member);
  }

  public TCGroupMember getMember(NodeID aNodeID) {
    TCGroupMember member = null;
    for (int i = 0; i < members.size(); ++i) {
      TCGroupMember m = members.get(i);
      if (m.getSrcNodeID() == getNodeID()) {
        if (m.getDstNodeID().equals(aNodeID)) {
          member = m;
          break;
        }
      } else {
        if (m.getSrcNodeID().equals(aNodeID)) {
          member = m;
          break;
        }
      }
    }
    return (member);
  }

  public List<TCGroupMember> getMembers() {
    return members;
  }

  public void setDiscover(TCGroupMemberDiscovery discover) {
    this.discover = discover;
    this.discover.setTCGroupManager(this);
  }

  public TCGroupMemberDiscovery getDiscover() {
    return discover;
  }

  public void shutdown() {
    try {
      stop(1000);
      closeAllChannels();
    } catch (TCTimeoutException e) {
      logger.warn("Timeout at shutting down " + e);
    }
  }

  public int size() {
    return members.size();
  }

  public void messageReceived(GroupMessage message, MessageChannel channel) {

    if (debug) {
      logger.info(getNodeID() + " recd msg " + message.getMessageID() + " From " + channel + " Msg : " + message);
    }

    TCGroupMember m = getMember(channel);
    if (m == null) {
      logger.warn("Message from non-existing member with channel: " + channel);
      // XXX? drop message
      return;
    }

    MessageID requestID = message.inResponseTo();
    NodeID from = m.getNodeID();

    message.setMessageOrginator(from);
    if (requestID.isNull() || !notifyPendingRequests(requestID, message, m)) {
      fireMessageReceivedEvent(from, message);
    }
  }

  private boolean notifyPendingRequests(MessageID requestID, GroupMessage gmsg, TCGroupMember sender) {
    GroupResponseImpl response = (GroupResponseImpl) pendingRequests.get(requestID);
    if (response != null) {
      response.addResponseFrom(sender, gmsg);
      return true;
    }
    return false;
  }

  private static void validateExternalizableClass(Class<AbstractGroupMessage> clazz) {
    String name = clazz.getName();
    try {
      Constructor<AbstractGroupMessage> cons = clazz.getDeclaredConstructor(new Class[0]);
      if ((cons.getModifiers() & Modifier.PUBLIC) == 0) { throw new AssertionError(
                                                                                   name
                                                                                       + " : public no arg constructor not found"); }
    } catch (NoSuchMethodException ex) {
      throw new AssertionError(name + " : public no arg constructor not found");
    }
  }

  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    validateExternalizableClass(msgClass);
    GroupMessageListener prev = messageListeners.put(msgClass.getName(), listener);
    if (prev != null) {
      logger.warn("Previous listener removed : " + prev);
    }
  }

  public void routeMessages(Class msgClass, Sink sink) {
    registerForMessages(msgClass, new RouteGroupMessagesToSink(msgClass.getName(), sink));
  }

  private void fireMessageReceivedEvent(NodeID from, GroupMessage msg) {
    GroupMessageListener listener = messageListeners.get(msg.getClass().getName());
    if (listener != null) {
      listener.messageReceived(from, msg);
    } else {
      String errorMsg = "No Route for " + msg + " from " + from;
      logger.error(errorMsg);
      throw new AssertionError(errorMsg);
    }
  }

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    this.zapNodeRequestProcessor = processor;
  }

  public void zapNode(NodeID nodeID, int type, String reason) {
    TCGroupMember m = getMember(nodeID);
    if (m == null) {
      logger.warn("Ignoring Zap node request since Member is null");
    } else if (!zapNodeRequestProcessor.acceptOutgoingZapNodeRequest(nodeID, type, reason)) {
      logger.warn("Ignoreing Zap node request since " + zapNodeRequestProcessor + " asked us to : " + nodeID
                  + " type = " + type + " reason = " + reason);
    } else {
      long weights[] = zapNodeRequestProcessor.getCurrentNodeWeights();
      logger.warn("Zapping node : " + nodeID + " type = " + type + " reason = " + reason + " my weight = "
                  + Arrays.toString(weights));
      GroupMessage msg = GroupZapNodeMessageFactory.createGroupZapNodeMessage(type, reason, weights);
      sendTo(nodeID, msg);
      logger.warn("Removing member " + m + " from group");
      memberDisappeared(m);
    }
  }

  private static class GroupResponseImpl implements GroupResponse {

    HashSet<NodeID>    waitFor   = new HashSet<NodeID>();
    List<GroupMessage> responses = new ArrayList<GroupMessage>();

    public synchronized List<GroupMessage> getResponses() {
      Assert.assertTrue(waitFor.isEmpty());
      return responses;
    }

    public synchronized GroupMessage getResponse(NodeID nodeID) {
      Assert.assertTrue(waitFor.isEmpty());
      for (Iterator<GroupMessage> i = responses.iterator(); i.hasNext();) {
        GroupMessage msg = i.next();
        if (nodeID.equals(msg.messageFrom())) return msg;
      }
      return null;
    }

    public void sendTo(TCGroupMember member, GroupMessage msg) {
      waitFor.add(member.getNodeID());
      member.send(msg);
    }

    public void sendAll(TCGroupManager manager, GroupMessage msg) throws GroupException {
      List<TCGroupMember> m = manager.getMembers();
      if (m.size() > 0) {
        setUpWaitFor(m);
        manager.sendAll(msg);
      }
    }

    private synchronized void setUpWaitFor(List<TCGroupMember> members) {
      Iterator it = members.iterator();
      while (it.hasNext()) {
        TCGroupMember member = (TCGroupMember) it.next();
        waitFor.add(member.getNodeID());
      }
    }

    public synchronized void addResponseFrom(TCGroupMember sender, GroupMessage gmsg) {
      if (!waitFor.remove(sender.getNodeID())) {
        String message = "Recd response from a member not in list : " + sender + " : waiting For : " + waitFor
                         + " msg : " + gmsg;
        logger.error(message);
        throw new AssertionError(message);
      }
      responses.add(gmsg);
      notifyAll();
    }

    public synchronized void notifyMemberDead(TCGroupMember member) {
      waitFor.remove(member.getNodeID());
      notifyAll();
    }

    public synchronized void waitForResponses() throws GroupException {
      int count = 0;
      while (!waitFor.isEmpty()) {
        try {
          this.wait(5000);
          if (++count > 1) {
            logger.warn("Still waiting for response from " + waitFor + ". Count = " + count);
          }
        } catch (InterruptedException e) {
          throw new GroupException(e);
        }
      }
    }
  }

  private final class ZapNodeRequestRouter implements GroupMessageListener {

    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      GroupZapNodeMessage zapMsg = (GroupZapNodeMessage) msg;
      zapNodeRequestProcessor.incomingZapNodeRequest(msg.messageFrom(), zapMsg.getZapNodeType(), zapMsg.getReason(),
                                                     zapMsg.getWeights());
    }
  }
}