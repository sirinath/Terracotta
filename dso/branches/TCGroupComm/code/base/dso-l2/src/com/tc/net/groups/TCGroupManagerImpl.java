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
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
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
import com.tc.util.UUID;
import com.tc.util.concurrent.TCFuture;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.SimpleSequence;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCGroupManagerImpl extends SEDA implements GroupManager, ChannelManagerEventListener {
  private static final TCLogger                                     logger                       = TCLogging
                                                                                                     .getLogger(TCGroupManagerImpl.class);
  private final NodeIdComparable                                    thisNodeID;

  private CommunicationsManager                                     communicationsManager;
  private NetworkListener                                           groupListener;
  private final ConnectionPolicy                                    connectionPolicy;
  private TCGroupMemberDiscovery                                    discover;
  private final CopyOnWriteArrayList<GroupEventsListener>           groupListeners               = new CopyOnWriteArrayList<GroupEventsListener>();
  private final Map<String, GroupMessageListener>                   messageListeners             = new ConcurrentHashMap<String, GroupMessageListener>();
  private final Map<MessageID, GroupResponse>                       pendingRequests              = new ConcurrentHashMap<MessageID, GroupResponse>();
  private ZapNodeRequestProcessor                                   zapNodeRequestProcessor      = new DefaultZapNodeRequestProcessor(
                                                                                                                                      logger);
  private final AtomicBoolean                                       isStopped                    = new AtomicBoolean(
                                                                                                                     false);
  private final AtomicBoolean                                       ready                        = new AtomicBoolean(
                                                                                                                     false);
  private Stage                                                     hydrateStage;
  private Stage                                                     receiveGroupMessageStage;
  private Stage                                                     receivePingMessageStage;
  private Stage                                                     handshakeMessageStage;

  private final ConcurrentHashMap<MessageChannel, TCFuture>         pingMessages                 = new ConcurrentHashMap<MessageChannel, TCFuture>();

  private final ConcurrentHashMap<MessageChannel, TCFuture>         handshakeResults             = new ConcurrentHashMap<MessageChannel, TCFuture>();

  private final ConcurrentHashMap<MessageChannel, NodeIdComparable> channelToNodeID              = new ConcurrentHashMap<MessageChannel, NodeIdComparable>();

  private final ConcurrentHashMap<NodeIdComparable, TCGroupMember>  members                      = new ConcurrentHashMap<NodeIdComparable, TCGroupMember>();

  public static final String                                        NHA_TCCOMM_HANDSHAKE_TIMEOUT = "l2.nha.tcgroupcomm.handshake.timeout";
  public static final String                                        NHA_TCCOMM_RESPONSE_TIMEOUT  = "l2.nha.tcgroupcomm.response.timelimit";
  private static final int                                          MAX_DEFAULT_COMM_THREADS     = 16;
  private final static long                                         handshakeTimeout;
  private final static long                                         responseTimelimit;
  static {
    handshakeTimeout = TCPropertiesImpl.getProperties().getLong(NHA_TCCOMM_HANDSHAKE_TIMEOUT);
    responseTimelimit = TCPropertiesImpl.getProperties().getLong(NHA_TCCOMM_RESPONSE_TIMEOUT);
  }

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
    this.connectionPolicy = connectionPolicy;

    TCProperties l2Properties = TCPropertiesImpl.getProperties().getPropertiesFor("l2");

    configSetupManager.commonl2Config().changesInItemIgnored(configSetupManager.commonl2Config().dataPath());
    NewL2DSOConfig l2DSOConfig = configSetupManager.dsoL2Config();

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.l2GroupPort());
    int groupPort = l2DSOConfig.l2GroupPort().getInt();

    thisNodeID = init(l2DSOConfig.host().getString(), groupPort, getCommWorkerCount(l2Properties));

    setDiscover(new TCGroupMemberDiscoveryStatic(configSetupManager));
    start(new HashSet());
  }

  private int getCommWorkerCount(TCProperties props) {
    int def = Math.min(Runtime.getRuntime().availableProcessors(), MAX_DEFAULT_COMM_THREADS);
    return props.getInt("tccom.workerthreads", def);
  }

  /*
   * for testing purpose only. Tester needs to do setDiscover() and start()
   */
  TCGroupManagerImpl(ConnectionPolicy connectionPolicy, String hostname, int groupPort, int workerThreads,
                     TCThreadGroup threadGroup) {
    super(threadGroup);
    this.connectionPolicy = connectionPolicy;
    thisNodeID = init(hostname, groupPort, workerThreads);
    ready.set(true);
  }

  private String makeGroupNodeName(String hostname, int groupPort) {
    return (hostname + ":" + groupPort);
  }

  private NodeIdComparable init(String hostname, int groupPort, int workerThreads) {

    String nodeName = makeGroupNodeName(hostname, groupPort);
    NodeIdComparable aNodeID = new NodeIdComparable(nodeName, UUID.getUUID().toString().getBytes());
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

  /*
   * monitor channel events while doing group member handshaking
   */
  private static class handshakeChannelEventListener implements ChannelEventListener {
    final private MessageChannel                              channel;
    final private ConcurrentHashMap<MessageChannel, TCFuture> handshakeResults;
    final private ConcurrentHashMap<MessageChannel, TCFuture> pingMessages;

    handshakeChannelEventListener(MessageChannel channel, ConcurrentHashMap<MessageChannel, TCFuture> handshakeResults,
                                  ConcurrentHashMap<MessageChannel, TCFuture> pingMessages) {
      this.channel = channel;
      this.handshakeResults = handshakeResults;
      this.pingMessages = pingMessages;
    }

    /*
     * cancel future result on both stages if disconnect/closed event happened
     */
    public void notifyChannelEvent(ChannelEvent event) {
      if (event.getChannel() == channel) {
        if ((event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT)
            || (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT)) {
          TCFuture hsresult = handshakeResults.get(channel);
          if (hsresult != null) hsresult.cancel();
          TCFuture pingresult = pingMessages.get(channel);
          if (pingresult != null) pingresult.cancel();
        }
      }
    }
  }

  /*
   * Once connected, both send NodeID to each other.
   */
  private TCGroupHandshakeMessage handshake(final MessageChannel channel) {

    TCFuture result = getOrCreateHandshakeResult(channel);
    channel.addListener(new handshakeChannelEventListener(channel, handshakeResults, pingMessages));

    writeHandshakeMessage(channel);
    TCGroupHandshakeMessage peermsg = readHandshakeMessage(channel, result, handshakeTimeout);
    handshakeResults.remove(channel);
    if (peermsg == null) { return null; }

    channelToNodeID.put(channel, peermsg.getNodeID());
    return peermsg;
  }

  private TCGroupHandshakeMessage readHandshakeMessage(MessageChannel channel, TCFuture result, long timeout) {
    TCGroupHandshakeMessage msg = null;

    try {
      msg = (TCGroupHandshakeMessage) result.get(timeout);
    } catch (TCTimeoutException e) {
      logger.warn("Handshake message timeout from peer " + channel);
      channel.close();
      return null;
    } catch (Exception e) {
      if (logger.isDebugEnabled()) logger.debug("readHandshakeMessage: " + e);
      return null;
    }

    return msg;
  }

  private void writeHandshakeMessage(MessageChannel channel) {
    TCGroupHandshakeMessage msg = (TCGroupHandshakeMessage) channel
        .createMessage(TCMessageType.GROUP_HANDSHAKE_MESSAGE);
    msg.initialize(getNodeID());
    msg.send();
    if (logger.isDebugEnabled()) logger.debug("Send group handshake message to " + channel);
  }

  public void handshakeReceived(TCGroupHandshakeMessage msg) {
    if (logger.isDebugEnabled()) logger.debug("Received handshake message from " + msg.getChannel());

    MessageChannel channel = msg.getChannel();
    Assert.assertNotNull(channel);
    TCFuture result = getOrCreateHandshakeResult(channel);
    result.set(msg);
  }

  /*
   * handshake message may arrive before local node is ready to receive it.
   * create it by whoever comes first, either receiver or sender.
   */
  private TCFuture getOrCreateHandshakeResult(MessageChannel channel) {
    synchronized (handshakeResults) {
      TCFuture result = handshakeResults.get(channel);
      if (result == null) {
        result = new TCFuture();
        handshakeResults.put(channel, result);
      }
      return result;
    }
  }

  public NodeID getLocalNodeID() throws GroupException {
    if (this.thisNodeID == null) { throw new GroupException("Node hasnt joined the group yet !"); }
    return getNodeID();
  }

  private NodeIdComparable getNodeID() {
    return thisNodeID;
  }

  public void start(Set initialConnectionIDs) throws IOException {
    groupListener.start(initialConnectionIDs);
    isStopped.set(false);
  }

  public void stop(long timeout) throws TCTimeoutException {
    isStopped.set(true);
    getStageManager().stopAll();
    discover.stop();
    groupListener.stop(timeout);
    communicationsManager.getConnectionManager().asynchCloseAllConnections();
    for (TCGroupMember m : members.values()) {
      notifyAnyPendingRequests(m);
    }
    members.clear();
    channelToNodeID.clear();
  }

  public boolean isStopped() {
    return (isStopped.get());
  }

  public void registerForGroupEvents(GroupEventsListener listener) {
    groupListeners.add(listener);
  }

  private void fireNodeEvent(TCGroupMember member, boolean joined) {
    NodeIdComparable newNode = member.getPeerNodeID();
    member.setReady(joined);
    if (logger.isDebugEnabled()) logger.debug("fireNodeEvent: joined = " + joined + ", node = " + newNode);
    for (GroupEventsListener listener : groupListeners) {
      if (joined) {
        listener.nodeJoined(newNode);
      } else {
        listener.nodeLeft(newNode);
      }
    }
  }

  private boolean tryAddMember(TCGroupMember member) {
    if (isStopped.get()) { return false; }

    // Keep only one connection between two nodes. Close the redundant one.
    synchronized (this) {
      if (null != members.get(member.getPeerNodeID())) {
        // there is one exist already
        return false;
      }
      member.setTCGroupManager(this);
      members.put(member.getPeerNodeID(), member);
    }
    logger.debug(getNodeID() + " added " + member);
    return true;
  }

  public NodeID join(Node thisNode, Node[] allNodes) throws GroupException {
    ready.set(true);
    discover.setLocalNode(thisNode);
    discover.start();
    return (getNodeID());
  }

  public void memberDisappeared(TCGroupMember member) {
    Assert.assertNotNull(member);
    if (isStopped.get()) return;
    synchronized (this) {
      // sync to prevent race from tryJoinGroup
      synchronized (member) {
        member.setTCGroupManager(null);
        TCGroupMember m = members.get(member.getPeerNodeID());
        if ((m != null) && (m.getChannel() == member.getChannel())) {
          members.remove(member.getPeerNodeID());
          if (member.isJoinedEventFired()) fireNodeEvent(member, false);
          member.setJoinedEventFired(false);
        }
      }
    }
    closeMember(member, false);
    logger.debug(getNodeID() + " removed " + member);
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
    for (TCGroupMember m : members.values()) {
      if (m.isReady()) m.send(msg);
    }
  }

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException {
    TCGroupMember member = getMember((NodeIdComparable) node);
    if (member != null && member.isReady()) {
      member.send(msg);
    } else {
      // member in zombie mode
      logger.warn("Send to non-exist member of " + node);
    }
  }

  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException {
    if (logger.isDebugEnabled()) logger.debug(getNodeID() + " : Sending to " + nodeID + " and Waiting for Response : "
                                              + msg.getMessageID());
    GroupResponseImpl groupResponse = new GroupResponseImpl(this);
    MessageID msgID = msg.getMessageID();
    TCGroupMember m = getMember((NodeIdComparable) nodeID);
    if (m != null && m.isReady()) {
      GroupResponse old = pendingRequests.put(msgID, groupResponse);
      Assert.assertNull(old);
      groupResponse.sendTo(m, msg);
      groupResponse.waitForResponses(getNodeID());
      pendingRequests.remove(msgID);
    } else {
      String errorMsg = "Node " + nodeID + " not present in the group. Ignoring Message : " + msg;
      logger.error(errorMsg);
      throw new GroupException(errorMsg);
    }
    return groupResponse.getResponse(nodeID);

  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException {
    if (logger.isDebugEnabled()) logger.debug(getNodeID() + " : Sending to ALL and Waiting for Response : "
                                              + msg.getMessageID());
    GroupResponseImpl groupResponse = new GroupResponseImpl(this);
    MessageID msgID = msg.getMessageID();
    GroupResponse old = pendingRequests.put(msgID, groupResponse);
    Assert.assertNull(old);
    groupResponse.sendAll(msg);
    groupResponse.waitForResponses(getNodeID());
    pendingRequests.remove(msgID);
    return groupResponse;
  }

  private void closeMember(TCGroupMember member, boolean isAdded) {
    stopMember(member, isAdded);
    member.close();
  }

  private void stopMember(TCGroupMember member, boolean isAdded) {
    member.setReady(false);
    channelToNodeID.remove(member.getChannel());
    if (isAdded) {
      members.remove(member.getPeerNodeID());
    }
  }

  /*
   * channel opening from src to dst
   */
  private TCGroupMember openChannel(ConnectionAddressProvider addrProvider) throws TCTimeoutException,
      UnknownHostException, MaxConnectionsExceededException, IOException {

    if (isStopped.get()) return null;

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

    try {
      channel.open();
    } catch (TCTimeoutException e) {
      channel.close();
      throw e;
    } catch (MaxConnectionsExceededException e) {
      channel.close();
      throw e;
    } catch (IOException e) {
      channel.close();
      throw e;
    }

    TCGroupHandshakeMessage peermsg = handshake(channel);
    if (peermsg == null) return null;

    if (logger.isDebugEnabled()) logger.debug("Channel setup to " + peermsg.getNodeID());
    TCGroupMember member = new TCGroupMemberImpl(getNodeID(), peermsg.getNodeID(), channel);

    if (!tryJoinGroup(member, true)) return null;

    return member;
  }

  public TCGroupMember openChannel(String hostname, int groupPort) throws TCTimeoutException, UnknownHostException,
      MaxConnectionsExceededException, IOException {
    return openChannel(new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(hostname, groupPort) }));
  }

  /*
   * Called by low priority link member.
   * Scan current active channels for one with same nodeID which is high priority link.
   */
  private boolean isMakingHighPriorityLink(TCGroupMember member) {
    Assert.assertFalse(member.highPriorityLink());
    for (Map.Entry<MessageChannel, NodeIdComparable> entry : channelToNodeID.entrySet()) {
      if ((member.getPeerNodeID().equals(entry.getValue())) && (member.getChannel() != entry.getKey())) { return true; }
    }
    return false;
  }

  private boolean tryJoinGroup(TCGroupMember member, boolean connInitiator) {
    // handshake on low priority link to check no progress of high priority link
    // to favor high priority link
    if (!member.highPriorityLink()) {

      boolean tryJoinLowPriority = false;
      if (connInitiator) {
        tryJoinLowPriority = !isMakingHighPriorityLink(member);
        signalToJoin(member, tryJoinLowPriority);
      }
      tryJoinLowPriority = receivedOkToJoin(member);
      if (!connInitiator) {
        if (tryJoinLowPriority) tryJoinLowPriority = !isMakingHighPriorityLink(member);
        signalToJoin(member, tryJoinLowPriority);
      }

      if (!tryJoinLowPriority) {
        // both sides agree not to join
        closeMember(member, false);
        return false;
      }
      logger.debug("Try joining low priority link " + member);
    }

    boolean doCloseMember = false;
    boolean isAdded = tryAddMember(member);
    synchronized (member) {
      // connection initiator signal status to peer
      if (connInitiator) signalToJoin(member, isAdded);
      if (receivedOkToJoin(member) && isAdded) {
        // target node replies ok to initiator
        if (!connInitiator) signalToJoin(member, true);
        fireNodeEvent(member, true);
        member.setJoinedEventFired(true);
        return (true);
      } else {
        // target node replies deny to initiator
        if (!connInitiator) {
          signalToJoin(member, false);
          stopMember(member, isAdded);
        } else {
          doCloseMember = true;
        }
      }
    }
    // close member outside of sync to prevent deadlock
    // deadlock when channel event triggers a channel close at same time.
    if (doCloseMember) {
      closeMember(member, isAdded);
    }
    return (false);
  }

  /*
   * Event notification when a new connection setup by channelManager channel opened from dst to src
   */
  public void channelCreated(MessageChannel aChannel) {
    final MessageChannel channel = aChannel;

    if (isStopped.get() || !ready.get()) {
      // !ready.get(): Accept channels only after fully initialized.
      // otherwise "java.lang.AssertionError: No Route" when receive messages
      channel.close();
      return;
    }

    // spawn a new thread to continue work, otherwise block select thread
    final Thread t = new Thread("creating channel " + channel.getChannelID()) {
      public void run() {

        TCGroupHandshakeMessage peermsg = handshake(channel);
        if (peermsg == null) return;

        if (logger.isDebugEnabled()) logger.debug("Channel established from " + peermsg.getNodeID());

        final TCGroupMember member = new TCGroupMemberImpl(channel, peermsg.getNodeID(), getNodeID());

        tryJoinGroup(member, false);

      }
    };
    t.setDaemon(true);
    t.start();
  }

  private void signalToJoin(TCGroupMember member, boolean ok) {
    TCGroupPingMessage ping = (TCGroupPingMessage) member.getChannel().createMessage(TCMessageType.GROUP_PING_MESSAGE);
    if (ok) {
      if (logger.isDebugEnabled()) logger.debug("Send ok message to " + member);
      ping.initializeOk();
    } else {
      if (logger.isDebugEnabled()) logger.debug("Send deny message to " + member);
      ping.initializeDeny();
    }
    ping.send();
  }

  private TCFuture getOrCreatePingResult(MessageChannel channel) {
    synchronized (pingMessages) {
      // check if message arrived already
      TCFuture result = pingMessages.get(channel);
      if (result == null) {
        result = new TCFuture();
        pingMessages.put(channel, result);
      }
      return (result);
    }
  }

  private boolean receivedOkToJoin(TCGroupMember member) {
    Assert.assertNotNull(member);
    MessageChannel channel = member.getChannel();
    Assert.assertNotNull(channel);
    TCFuture result = getOrCreatePingResult(channel);

    TCGroupPingMessage ping = null;
    try {
      ping = (TCGroupPingMessage) result.get(handshakeTimeout);
    } catch (Exception e) {
      logger.debug("Failed to receive ok message from " + member);
    }

    pingMessages.remove(channel);

    return ((ping != null) ? ping.isOkMessage() : false);
  }

  public void pingReceived(TCGroupPingMessage msg) {
    MessageChannel channel = msg.getChannel();
    TCFuture result = getOrCreatePingResult(channel);
    result.set(msg);
  }

  /*
   * Event notification when a connection removed by DSOChannelManager
   */
  public void channelRemoved(MessageChannel channel) {
    TCGroupMember m = getMember(channel);
    if (m != null) {
      logger.debug("Channel removed from " + m.getPeerNodeID());
      memberDisappeared(m);
    } else {
      channel.close();
    }
  }

  public synchronized TCGroupMember getMember(MessageChannel channel) {
    NodeIdComparable nodeID = channelToNodeID.get(channel);
    if (nodeID == null) return null;
    TCGroupMember m = members.get(nodeID);
    return ((m != null) && (m.getChannel() == channel)) ? m : null;
  }

  public synchronized TCGroupMember getMember(NodeIdComparable nodeID) {
    return (members.get(nodeID));
  }

  public NodeIdComparable findNodeID(Node node) {
    String nodeName = makeGroupNodeName(node.getHost(), node.getPort());
    NodeIdComparable nodeID = null;
    for (NodeIdComparable n : channelToNodeID.values()) {
      if (nodeName.equals(n.getName())) {
        nodeID = n;
        break;
      }
    }
    return nodeID;
  }

  public Collection<TCGroupMember> getMembers() {
    return Collections.unmodifiableCollection(members.values());
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

    if (isStopped.get()) return;

    if (logger.isDebugEnabled()) logger.debug(getNodeID() + " recd msg " + message.getMessageID() + " From " + channel
                                              + " Msg : " + message);

    TCGroupMember m = getMember(channel);

    if (channel.isClosed()) {
      logger
          .warn(getNodeID() + " recd msg " + message.getMessageID() + " From closed " + channel + " Msg : " + message);
      return;
    }

    if (m == null) { throw new RuntimeException("Received message to non-exist member from "
                                                + channel.getRemoteAddress() + " to " + channel.getLocalAddress()
                                                + " Node: " + channelToNodeID.get(channel)); }

    NodeIdComparable from = m.getPeerNodeID();
    MessageID requestID = message.inResponseTo();

    message.setMessageOrginator(from);
    synchronized (m) {
      // There is a race condition, peer notified upper layer and sent L2StateMessage
      // while this node still waiting handshake from peer.
      // exception: No Route for L2StateMessage <-- sync to resolve this issue
      if (requestID.isNull() || !notifyPendingRequests(requestID, message, from)) {
        fireMessageReceivedEvent(from, message);
      }
    }
  }

  private boolean notifyPendingRequests(MessageID requestID, GroupMessage gmsg, NodeIdComparable nodeID) {
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
      if ((cons.getModifiers() & Modifier.PUBLIC) == 0) {
        throw new AssertionError(name + " : public no arg constructor not found");
      }
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

  private void fireMessageReceivedEvent(NodeIdComparable from, GroupMessage msg) {
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
    TCGroupMember m = getMember((NodeIdComparable) nodeID);
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

    private final HashSet<NodeIdComparable> waitFor   = new HashSet<NodeIdComparable>();
    private final List<GroupMessage>        responses = new ArrayList<GroupMessage>();
    private final TCGroupManagerImpl        manager;

    GroupResponseImpl(TCGroupManagerImpl manager) {
      this.manager = manager;
    }

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
      if (member.isReady()) {
        waitFor.add(member.getPeerNodeID());
        member.send(msg);
      } else {
        throw new RuntimeException("Send to a not ready member " + member);
      }
    }

    public void sendAll(GroupMessage msg) throws GroupException {
      for (TCGroupMember m : manager.getMembers()) {
        if (m.isReady()) {
          waitFor.add(m.getPeerNodeID());
          m.send(msg);
        }
      }
    }

    public synchronized void addResponseFrom(NodeIdComparable nodeID, GroupMessage gmsg) {
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

    public synchronized void waitForResponses(NodeIdComparable sender) throws GroupException {
      int count = 0;
      long start = System.currentTimeMillis();
      while (!waitFor.isEmpty() && !manager.isStopped()) {
        try {
          this.wait(5000);
          if (++count > 1) {
            logger.warn(sender + " Still waiting for response from " + waitFor + ". Count = " + count);
            if (System.currentTimeMillis() > (start + responseTimelimit)) {
              // something wrong
              throw new RuntimeException("Still waiting for response from " + waitFor);
            }
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