/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.SEDA;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.ImplementMe;
import com.tc.lang.TCThreadGroup;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
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
import com.tc.util.UUID;
import com.tc.util.sequence.SimpleSequence;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class TCGroupMembershipImpl extends SEDA implements TCGroupMembership, ChannelManagerEventListener {
  private final L2TVSConfigurationSetupManager configSetupManager;
  private TCProperties                         l2Properties;
  private CommunicationsManager                communicationsManager;
  private NetworkListener                      groupListener;
  private final ConnectionPolicy               connectionPolicy;
  //private HashMap<NodeID, TCGroupMember>       members;
  private final NodeID                         nodeID;
  private TCGroupMemberDiscovery               members;

  /*
   * Setup a communication manager which can establish channel from either sides.
   */
  public TCGroupMembershipImpl(L2TVSConfigurationSetupManager configSetupManager, ConnectionPolicy connectionPolicy,
                               TCThreadGroup threadGroup) {
    super(threadGroup);
    this.configSetupManager = configSetupManager;
    this.connectionPolicy = connectionPolicy;
    l2Properties = TCPropertiesImpl.getProperties().getPropertiesFor("l2");
    
    members = new TCGroupMemberDiscoveryStatic(configSetupManager);

    this.configSetupManager.commonl2Config().changesInItemIgnored(configSetupManager.commonl2Config().dataPath());
    NewL2DSOConfig l2DSOConfig = configSetupManager.dsoL2Config();

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.l2GroupPort());
    int groupPort = l2DSOConfig.l2GroupPort().getInt();

    String nodeName = l2DSOConfig.host().getString() + ":" + groupPort;
    nodeID = new NodeIDImpl(nodeName, UUID.getUUID().toString().getBytes());

    final NetworkStackHarnessFactory networkStackHarnessFactory = new TCGroupNetworkStackHarnessFactory();
    communicationsManager = new TCGroupCommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                                 null, this.connectionPolicy, l2Properties
                                                                     .getInt("tccom.workerthreads"), nodeID);

    groupListener = communicationsManager.createListener(new NullSessionManager(),
                                                         new TCSocketAddress(TCSocketAddress.WILDCARD_ADDR, groupPort),
                                                         true, new DefaultConnectionIdFactory());
    // Listen to channel creation/removal
    groupListener.getChannelManager().addEventListener(this);

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

  public void add(TCGroupMember member) {
    members.memberActivated(member);
  }

  public void clean() {
    throw new ImplementMe();
  }

  public boolean isExist(TCGroupMember member) {
    return (members.isMemberExist(member));
  }

  public NodeID join(Node thisNode, Node[] allNodes) {
    members.setupMembers(thisNode, allNodes);
    throw new ImplementMe();
  }

  public void remove(TCGroupMember member) {
    throw new ImplementMe();
  }

  public void remove(MessageChannel channel) {
    members.memberDeactivated(members.getMember(channel));
  }

  public void sendAll(GroupMessage msg) {
    ArrayList<TCGroupMember> nodes = members.getCurrentMembers();
    
    for(int i = 0; i < nodes.size(); ++i) {
      TCGroupMember member = nodes.get(i);
      member.send(msg);
    }
  }

  public void sendTo(NodeID node, GroupMessage msg) {
    // find thye member
    TCGroupMember member = members.getMember(node);
    Assert.assertTrue(member != null);
    member.send(msg);
  }

  public void openChannel(ConnectionAddressProvider addrProvider) {
    ClientMessageChannel channel = communicationsManager
        .createClientChannel(new SessionManagerImpl(new SimpleSequence()), -1, null, -1, 10000, addrProvider);

    try {
      channel.open();
    } catch (TCTimeoutException e) {
    } catch (UnknownHostException e) {
    } catch (MaxConnectionsExceededException e) {
    } catch (IOException e) {
    }

    add(new TCGroupMemberImpl(channel));
  }

  public void closeChannel(TCGroupMember member) {

  }

  public void closeChannel(MessageChannel channel) {

  }

  /*
   * Event notification when a new connection setup by channelManager
   */
  public void channelCreated(MessageChannel channel) {
    add(new TCGroupMemberImpl(channel));
  }

  /*
   * Event notification when a connection removed by DSOChannelManager
   */
  public void channelRemoved(MessageChannel channel) {
    remove(channel);
  }
}