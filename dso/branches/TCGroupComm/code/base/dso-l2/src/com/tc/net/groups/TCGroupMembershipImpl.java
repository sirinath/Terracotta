/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.SEDA;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
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
import com.tc.object.msg.TCGroupHandshakeMessageImpl;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handler.ReceiveGroupMessageHandler;
import com.tc.objectserver.handler.TCGroupHandshakeHandler;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.sequence.SimpleSequence;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TCGroupMembershipImpl extends SEDA implements TCGroupMembership, ChannelManagerEventListener {
  private static final TCLogger                logger  = TCLogging.getLogger(TCGroupMembershipImpl.class);

  private final L2TVSConfigurationSetupManager configSetupManager;
  private TCProperties                         l2Properties;
  private CommunicationsManager                communicationsManager;
  private NetworkListener                      groupListener;
  private final ConnectionPolicy               connectionPolicy;
  // private HashMap<NodeID, TCGroupMember> members;
  private final NodeID                         nodeID;
  private TCGroupMemberDiscovery               discover;
  private ArrayList<TCGroupMember>             members = new ArrayList<TCGroupMember>();

  /*
   * Setup a communication manager which can establish channel from either sides.
   */
  public TCGroupMembershipImpl(L2TVSConfigurationSetupManager configSetupManager, ConnectionPolicy connectionPolicy,
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

    nodeID = init(l2DSOConfig.host().getString(), groupPort, l2Properties.getInt("tccom.workerthreads"));

    int maxStageSize = 5000;
    StageManager stageManager = getStageManager();
    Stage hydrateStage = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK,
                                                  new HydrateHandler(), 1, maxStageSize);
    Stage clientHandshake = stageManager.createStage(ServerConfigurationContext.GROUP_HANDSHAKE_STAGE,
                                                     new TCGroupHandshakeHandler(), 1, maxStageSize);
    Stage receiveGroupMessageStage = stageManager.createStage(ServerConfigurationContext.RECEIVE_GROUP_MESSAGE_STAGE,
                                                              new ReceiveGroupMessageHandler(), 1, maxStageSize);

    groupListener.addClassMapping(TCMessageType.GROUP_HANDSHAKE_MESSAGE, TCGroupHandshakeMessageImpl.class);
    groupListener.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);

    groupListener.routeMessageType(TCMessageType.GROUP_HANDSHAKE_MESSAGE, clientHandshake.getSink(), hydrateStage
        .getSink());
    groupListener.routeMessageType(TCMessageType.GROUP_WRAPPER_MESSAGE, receiveGroupMessageStage.getSink(),
                                   hydrateStage.getSink());
  }

  /*
   * for testing purpose only
   */
  public TCGroupMembershipImpl(ConnectionPolicy connectionPolicy, String hostname, int groupPort, int workerThreads,
                               TCThreadGroup threadGroup) throws IOException {
    super(threadGroup);
    this.configSetupManager = null;
    this.connectionPolicy = connectionPolicy;
    nodeID = init(hostname, groupPort, workerThreads);
  }

  public NodeID init(String hostname, int groupPort, int workerThreads) throws IOException {

    String nodeName = hostname + ":" + groupPort;
    NodeID aNodeID = new NodeIdUuidImpl(nodeName);

    final NetworkStackHarnessFactory networkStackHarnessFactory = new TCGroupNetworkStackHarnessFactory();
    communicationsManager = new TCGroupCommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                                 null, this.connectionPolicy, workerThreads, aNodeID);

    groupListener = communicationsManager.createListener(new NullSessionManager(),
                                                         new TCSocketAddress(TCSocketAddress.WILDCARD_ADDR, groupPort),
                                                         true, new DefaultConnectionIdFactory());
    // Listen to channel creation/removal
    groupListener.getChannelManager().addEventListener(this);

    return (aNodeID);
  }

  public void start(Set initialConnectionIDs) throws IOException {
    groupListener.start(initialConnectionIDs);
  }
  
  public void stop(long timeout) throws TCTimeoutException {
    discover.stop();
    groupListener.stop(timeout);
  }

  public void add(TCGroupMember member) {
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
    member.setTCGroupMembership(this);
    members.add(member);
  }

  public boolean isExist(TCGroupMember member) {
    return (members.contains(member));
  }

  public NodeID join(Node thisNode, Node[] allNodes) throws GroupException {
    discover.setLocalNode(thisNode);
    discover.start();
    
    return (getNodeID());
  }

  public void remove(TCGroupMember member) {
    members.remove(member);
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

  public TCGroupMember openChannel(ConnectionAddressProvider addrProvider) throws TCTimeoutException,
      UnknownHostException, MaxConnectionsExceededException, IOException {
    ClientMessageChannel channel = communicationsManager
        .createClientChannel(new SessionManagerImpl(new SimpleSequence()), -1, null, -1, 10000, addrProvider);

    channel.open();
    logger.debug("Channel setup to " + channel.getChannelID().getNodeID());
    TCGroupMember member = new TCGroupMemberImpl(getNodeID(), channel);
    add(member);
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
    logger.debug("Channel established from " + channel.getChannelID().getNodeID());
    add(new TCGroupMemberImpl(channel, getNodeID()));
  }

  /*
   * Event notification when a connection removed by DSOChannelManager
   */
  public void channelRemoved(MessageChannel channel) {
    logger.debug("Channel removed from " + channel.getChannelID().getNodeID());
    remove(channel);
  }

  public NodeID getNodeID() {
    return nodeID;
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
    this.discover.setTCGroupMembership(this);
  }

  public TCGroupMemberDiscovery getDiscover() {
    return discover;
  }

  public void shutdown() {
    communicationsManager.shutdown();
  }

  public int size() {
    return members.size();
  }

}