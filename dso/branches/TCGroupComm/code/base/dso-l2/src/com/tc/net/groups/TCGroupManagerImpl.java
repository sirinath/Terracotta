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
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handler.ReceiveGroupMessageHandler;
import com.tc.objectserver.handler.TCGroupHandshakeMessageHandler;
import com.tc.objectserver.handler.TCGroupPingMessageHandler;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;
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
import java.util.concurrent.LinkedBlockingQueue;

public class TCGroupManagerImpl extends SEDA implements GroupManager, ChannelManagerEventListener {
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
  private final CopyOnWriteArrayList<TCGroupMember>       members                 = new CopyOnWriteArrayList<TCGroupMember>();
  private final CopyOnWriteArrayList<GroupEventsListener> groupListeners          = new CopyOnWriteArrayList<GroupEventsListener>();
  private final Map<String, GroupMessageListener>         messageListeners        = new ConcurrentHashMap<String, GroupMessageListener>();
  private final Map<MessageID, GroupResponse>             pendingRequests         = new Hashtable<MessageID, GroupResponse>();
  private ZapNodeRequestProcessor                         zapNodeRequestProcessor = new DefaultZapNodeRequestProcessor(
                                                                                                                       logger);
  private boolean                                         isStopped               = false;
  private Stage                                           hydrateStage;
  private Stage                                           receiveGroupMessageStage;
  private Stage                                           receivePingMessageStage;
  private Stage                                           handshakeMessageStage;
  private LinkedBlockingQueue<TCGroupPingMessage>         pingQueue               = new LinkedBlockingQueue<TCGroupPingMessage>(
                                                                                                                                100);
  private LinkedBlockingQueue<TCGroupHandshakeMessage>    handshakeQueue          = new LinkedBlockingQueue<TCGroupHandshakeMessage>(
                                                                                                                                     100);
  private ConcurrentHashMap<MessageChannel, NodeID>       chToNodeID              = new ConcurrentHashMap<MessageChannel, NodeID>();

  /*
   * Setup a communication manager which can establish channel from either sides.
   */
  public TCGroupManagerImpl(L2TVSConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup)
      throws IOException {
    this(configSetupManager, new NullConnectionPolicy(), threadGroup);
  }

  public TCGroupManagerImpl(L2TVSConfigurationSetupManager configSetupManager, ConnectionPolicy connectionPolicy,
                            TCThreadGroup threadGroup) throws IOException {
    super(threadGroup);
    this.configSetupManager = configSetupManager;
    this.connectionPolicy = connectionPolicy;

    l2Properties = TCPropertiesImpl.getProperties().getPropertiesFor("l2");

    this.configSetupManager.commonl2Config().changesInItemIgnored(configSetupManager.commonl2Config().dataPath());
    NewL2DSOConfig l2DSOConfig = configSetupManager.dsoL2Config();

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.l2GroupPort());
    int groupPort = l2DSOConfig.l2GroupPort().getInt();

    thisNodeID = init(l2DSOConfig.host().getString(), groupPort, l2Properties.getInt("tccom.workerthreads"));

    setDiscover(new TCGroupMemberDiscoveryStatic(configSetupManager));
    start(new HashSet());
  }

  /*
   * for testing purpose only. Tester needs to do setDiscover() and start()
   */
  public TCGroupManagerImpl(ConnectionPolicy connectionPolicy, String hostname, int groupPort, int workerThreads,
                            TCThreadGroup threadGroup) {
    super(threadGroup);
    this.configSetupManager = null;
    this.connectionPolicy = connectionPolicy;
    thisNodeID = init(hostname, groupPort, workerThreads);
  }

  public String makeGroupNodeName(String hostname, int groupPort) {
    return (hostname + ":" + groupPort);
  }

  private NodeID init(String hostname, int groupPort, int workerThreads) {

    String nodeName = makeGroupNodeName(hostname, groupPort);
    NodeID aNodeID = new NodeIdUuidImpl(nodeName);
    logger.info("Creating group node: " + aNodeID);

    int maxStageSize = 5000;
    StageManager stageManager = getStageManager();
    hydrateStage = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK, new HydrateHandler(), 1,
                                            maxStageSize);
    receiveGroupMessageStage = stageManager.createStage(ServerConfigurationContext.RECEIVE_GROUP_MESSAGE_STAGE,
                                                        new ReceiveGroupMessageHandler(this), 1, maxStageSize);
    receivePingMessageStage = stageManager.createStage(ServerConfigurationContext.GROUP_PING_MESSAGE_STAGE,
                                                       new TCGroupPingMessageHandler(this), 1, maxStageSize);
    handshakeMessageStage = stageManager.createStage(ServerConfigurationContext.GROUP_HANDSHAKE_MESSAGE_STAGE,
                                                     new TCGroupHandshakeMessageHandler(this), 1, maxStageSize);

    final NetworkStackHarnessFactory networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    communicationsManager = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory, null,
                                                          this.connectionPolicy, workerThreads);

    groupListener = communicationsManager.createListener(new NullSessionManager(),
                                                         new TCSocketAddress(TCSocketAddress.WILDCARD_ADDR, groupPort),
                                                         true, new DefaultConnectionIdFactory());
    // Listen to channel creation/removal
    groupListener.getChannelManager().addEventListener(this);

    groupListener.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);
    groupListener.routeMessageType(TCMessageType.GROUP_WRAPPER_MESSAGE, receiveGroupMessageStage.getSink(),
                                   hydrateStage.getSink());
    groupListener.addClassMapping(TCMessageType.GROUP_PING_MESSAGE, TCGroupPingMessage.class);
    groupListener.routeMessageType(TCMessageType.GROUP_PING_MESSAGE, receivePingMessageStage.getSink(), hydrateStage
        .getSink());
    groupListener.addClassMapping(TCMessageType.GROUP_HANDSHAKE_MESSAGE, TCGroupHandshakeMessage.class);
    groupListener.routeMessageType(TCMessageType.GROUP_HANDSHAKE_MESSAGE, handshakeMessageStage.getSink(), hydrateStage
        .getSink());

    ConfigurationContext context = new ConfigurationContextImpl(stageManager);

    stageManager.startAll(context);

    registerForMessages(GroupZapNodeMessage.class, new ZapNodeRequestRouter());

    return (aNodeID);
  }

  private TCGroupHandshakeMessage readHandshakeMessage(MessageChannel channel) {
    int loopCount = 1000;
    for (int i = 0; i < loopCount; ++i) {
      for (TCGroupHandshakeMessage msg : handshakeQueue) {
        if (msg.getChannel() == channel) {
          handshakeQueue.remove(msg);
          chToNodeID.put(channel, msg.getNodeID());
          return msg;
        }
      }
      ThreadUtil.reallySleep(5);
    }
    logger.warn("Failed to receive handshake message from peer " + channel);
    return null;
  }

  private void writeHandshakeMessage(MessageChannel channel) {
    TCGroupHandshakeMessage msg = (TCGroupHandshakeMessage) channel
        .createMessage(TCMessageType.GROUP_HANDSHAKE_MESSAGE);
    msg.setNodeID(getNodeID());
    msg.send();
    if (debug) {
      logger.debug("Send group handshake message to " + channel);
    }
  }

  public void handshakeReceived(TCGroupHandshakeMessage msg) {
    if (debug) {
      logger.debug("Received handshake message from " + msg.getChannel());
    }
    if (!handshakeQueue.offer(msg)) { throw new RuntimeException("No room for handshake messages"); }
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
    communicationsManager.getConnectionManager().asynchCloseAllConnections();
    members.clear();
    chToNodeID.clear();
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

  public boolean memberAdded(TCGroupMember member) {
    boolean added = addMember(member);
    if (added) fireNodeEvent(member.getPeerNodeID(), true);
    return (added);
  }

  private boolean addMember(TCGroupMember member) {
    if (isStopped) {
      closeMember(member);
      return false;
    }

    // Keep only one connection between two nodes. Close the redundant one.
    synchronized (this) {
      Iterator it = members.iterator();
      while (it.hasNext()) {
        TCGroupMember m = (TCGroupMember) it.next();

        // sanity check
        boolean dup = member.getSrcNodeID().equals(m.getSrcNodeID()) && member.getDstNodeID().equals(m.getDstNodeID());
        if (dup) { throw new RuntimeException("Drop duplicate channel to the same node " + member); }

        if (member.getPeerNodeID().equals(m.getPeerNodeID())) {
          // there is one exist already
          closeMember(member);
          return false;
        }
      }
      member.setTCGroupManager(this);
      members.add(member);
    }
    logger.debug(getNodeID() + " added " + member);
    return true;
  }

  public NodeID join(Node thisNode, Node[] allNodes) throws GroupException {
    discover.setLocalNode(thisNode);
    discover.start();
    return (getNodeID());
  }

  public void memberDisappeared(TCGroupMember member) {
    if (isStopped || (member == null)) return;
    synchronized (this) {
      member.setTCGroupManager(null);
      closeMember(member);
      if (!members.remove(member)) {
        logger.warn("Remove non-exist member " + member);
        return;
      }
    }
    logger.debug(getNodeID() + " removed " + member);
    fireNodeEvent(member.getPeerNodeID(), false);
    notifyAnyPendingRequests(member);
  }

  private void notifyAnyPendingRequests(TCGroupMember member) {
    synchronized (pendingRequests) {
      for (Iterator<GroupResponse> i = pendingRequests.values().iterator(); i.hasNext();) {
        GroupResponseImpl response = (GroupResponseImpl) i.next();
        response.notifyMemberDead(member);
      }
    }
  }

  public void sendAll(GroupMessage msg) throws GroupException {
    Iterator it = members.iterator();
    while (it.hasNext()) {
      TCGroupMember m = (TCGroupMember) it.next();
      m.send(msg);
    }
  }

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException {
    TCGroupMember member = getMember(node);
    if (member != null) {
      member.send(msg);
    } else {
      logger.error("Send to non-exist member of " + node);
    }
  }

  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException {
    if (debug) {
      logger.info(getNodeID() + " : Sending to " + nodeID + " and Waiting for Response : " + msg.getMessageID());
    }
    GroupResponseImpl groupResponse = new GroupResponseImpl();
    MessageID msgID = msg.getMessageID();
    TCGroupMember m = getMember(nodeID);
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

  private void closeMember(TCGroupMember member) {
    chToNodeID.remove(member.getChannel());
    member.close();
  }

  /*
   * channel opening from src to dst
   */
  public TCGroupMember openChannel(ConnectionAddressProvider addrProvider) throws TCTimeoutException,
      UnknownHostException, MaxConnectionsExceededException, IOException {
    ClientMessageChannel channel = communicationsManager
        .createClientChannel(new SessionManagerImpl(new SimpleSequence()), 0, null, -1, 10000, addrProvider);

    channel.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);
    channel.routeMessageType(TCMessageType.GROUP_WRAPPER_MESSAGE, receiveGroupMessageStage.getSink(), hydrateStage
        .getSink());
    channel.addClassMapping(TCMessageType.GROUP_PING_MESSAGE, TCGroupPingMessage.class);
    channel.routeMessageType(TCMessageType.GROUP_PING_MESSAGE, receivePingMessageStage.getSink(), hydrateStage
        .getSink());
    channel.addClassMapping(TCMessageType.GROUP_HANDSHAKE_MESSAGE, TCGroupHandshakeMessage.class);
    channel.routeMessageType(TCMessageType.GROUP_HANDSHAKE_MESSAGE, handshakeMessageStage.getSink(), hydrateStage
        .getSink());

    channel.open();
    writeHandshakeMessage(channel);
    TCGroupHandshakeMessage peermsg = readHandshakeMessage(channel);
    if (peermsg == null) {
      channel.close();
      return null;
    }

    if (debug) {
      logger.debug("Channel setup to " + peermsg.getNodeID());
    }

    TCGroupMember member = new TCGroupMemberImpl(getNodeID(), peermsg.getNodeID(), channel);
    // favor high priority link
    if (!member.highPriorityLink()) {
      ThreadUtil.reallySleep(50);
    }

    if (addMember(member)) {
      signalToJoin(member, true);
      if (receivedOkToJoin(member)) {
        fireNodeEvent(member.getPeerNodeID(), true);
        return member;
      } else {
        members.remove(member);
        closeMember(member);
        return null;
      }
    } else {
      signalToJoin(member, false);
      return null;
    }
  }

  public TCGroupMember openChannel(String hostname, int groupPort) throws TCTimeoutException, UnknownHostException,
      MaxConnectionsExceededException, IOException {
    return openChannel(new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(hostname, groupPort) }));
  }

  /*
   * Event notification when a new connection setup by channelManager channel opened from dst to src
   */
  public void channelCreated(MessageChannel aChannel) {
    final MessageChannel channel = aChannel;
    if (isStopped) {
      channel.close();
      return;
    }

    // spawn a new thread to contine work, avoid dead lock on discovery
    final Thread t = new Thread() {
      public void run() {

        writeHandshakeMessage(channel);
        TCGroupHandshakeMessage peermsg = readHandshakeMessage(channel);
        if (peermsg == null) {
          channel.close();
          return;
        }

        if (debug) {
          logger.debug("Channel established from " + peermsg.getNodeID());
        }

        final TCGroupMember member = new TCGroupMemberImpl(channel, peermsg.getNodeID(), getNodeID());
        // favor high priority link
        if (!member.highPriorityLink()) {
          ThreadUtil.reallySleep(50);
        }

        if (addMember(member)) {
          if (receivedOkToJoin(member)) {
            signalToJoin(member, true);
            fireNodeEvent(member.getPeerNodeID(), true);
            return;
          } else {
            members.remove(member);
            return;
          }
        } else {
          if (receivedOkToJoin(member)) {
            signalToJoin(member, false);
          }
          return;
        }
      }
    };
    t.start();

    return;
  }

  private void signalToJoin(TCGroupMember member, boolean ok) {
    TCGroupPingMessage ping = (TCGroupPingMessage) member.getChannel().createMessage(TCMessageType.GROUP_PING_MESSAGE);
    if (ok) {
      if (debug) {
        logger.info("Send ok message to " + member);
      }
      ping.okMessage();
    } else {
      if (debug) {
        logger.info("Send deny message to " + member);
      }

      ping.denyMessage();
    }
    ping.send();
  }

  private boolean receivedOkToJoin(TCGroupMember member) {
    MessageChannel channel = member.getChannel();

    int loopCount = 1000;
    for (int i = 0; i < loopCount; ++i) {
      for (TCGroupPingMessage ping : pingQueue) {
        if (ping.getChannel() == channel) {
          pingQueue.remove(ping);
          return (ping.isOkMessage());
        }
      }
      ThreadUtil.reallySleep(5);
    }

    if (debug) {
      logger.warn("Failed to receive ok message from " + member);
    }
    return false;
  }

  public void pingReceived(TCGroupPingMessage msg) {
    if (!pingQueue.offer(msg)) { throw new RuntimeException("No room for ping messages"); }
  }

  /*
   * for testing purpose only
   */
  public LinkedBlockingQueue<TCGroupPingMessage> getPingQueue() {
    return pingQueue;
  }

  /*
   * Event notification when a connection removed by DSOChannelManager
   */
  public void channelRemoved(MessageChannel channel) {
    TCGroupMember m = getMember(channel);
    if (m != null) {
      logger.debug("Channel removed from " + m.getPeerNodeID());
      memberDisappeared(m);
    }
  }

  public NodeID getNodeID() {
    return thisNodeID;
  }

  public synchronized TCGroupMember getMember(MessageChannel channel) {
    TCGroupMember member = null;
    Iterator it = members.iterator();
    while (it.hasNext()) {
      TCGroupMember m = (TCGroupMember) it.next();
      if (m.getChannel() == channel) {
        member = m;
        break;
      }
    }
    return (member);
  }

  public synchronized TCGroupMember getMember(NodeID aNodeID) {
    TCGroupMember member = null;
    Iterator it = members.iterator();
    while (it.hasNext()) {
      TCGroupMember m = (TCGroupMember) it.next();
      if (m.getPeerNodeID().equals(aNodeID)) {
        member = m;
        break;
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
    } catch (TCTimeoutException e) {
      logger.warn("Timeout at shutting down " + e);
    }
  }

  public synchronized int size() {
    return members.size();
  }

  public void messageReceived(GroupMessage message, MessageChannel channel) {

    if (debug) {
      logger.info(getNodeID() + " recd msg " + message.getMessageID() + " From " + channel + " Msg : " + message);
    }

    NodeID from = chToNodeID.get(channel);
    MessageID requestID = message.inResponseTo();

    message.setMessageOrginator(from);
    if (requestID.isNull() || !notifyPendingRequests(requestID, message, from)) {
      fireMessageReceivedEvent(from, message);
    }
  }

  private boolean notifyPendingRequests(MessageID requestID, GroupMessage gmsg, NodeID nodeID) {
    GroupResponseImpl response = (GroupResponseImpl) pendingRequests.get(requestID);
    if (response != null) {
      response.addResponseFrom(nodeID, gmsg);
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
      try {
        sendTo(nodeID, msg);
      } catch (GroupException e) {
        logger.error("Error sending ZapNode Request to " + nodeID + " msg = " + msg);
      }
      logger.warn("Removing member " + m + " from group");
      // wait a little bit, hope other end receives it
      ThreadUtil.reallySleep(100);
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

    public void sendTo(TCGroupMember member, GroupMessage msg) throws GroupException {
      waitFor.add(member.getPeerNodeID());
      member.send(msg);
    }

    public void sendAll(TCGroupManagerImpl manager, GroupMessage msg) throws GroupException {
      Iterator it = manager.getMembers().iterator();
      while (it.hasNext()) {
        TCGroupMember member = (TCGroupMember) it.next();
        waitFor.add(member.getPeerNodeID());
        member.send(msg);
      }
    }

    public synchronized void addResponseFrom(NodeID nodeID, GroupMessage gmsg) {
      if (!waitFor.remove(nodeID)) {
        String message = "Recd response from a member not in list : " + nodeID + " : waiting For : " + waitFor
                         + " msg : " + gmsg;
        logger.error(message);
        throw new AssertionError(message);
      }
      responses.add(gmsg);
      notifyAll();
    }

    public synchronized void notifyMemberDead(TCGroupMember member) {
      waitFor.remove(member.getPeerNodeID());
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