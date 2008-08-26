/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.GroupID;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDImpl;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.TransportHandshakeMessage;
import com.tc.object.session.SessionProvider;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;

public class ClientGroupMessageChannelImpl extends ClientMessageChannelImpl implements ClientGroupMessageChannel {
  private static final TCLogger       logger            = TCLogging.getLogger(ClientGroupMessageChannel.class);
  private final TCMessageFactory      msgFactory;
  private final SessionProvider       sessionProvider;

  private final CommunicationsManager communicationsManager;
  private final GroupID[]             groupIDs;
  private final HashMap               groupToChannelMap = new HashMap();

  public ClientGroupMessageChannelImpl(TCMessageFactory msgFactory, SessionProvider sessionProvider,
                                       final int maxReconnectTries, CommunicationsManager communicationsManager,
                                       ConnectionAddressProvider[] addressProviders) {
    super(msgFactory, null, sessionProvider);
    this.msgFactory = msgFactory;
    this.sessionProvider = sessionProvider;

    this.communicationsManager = communicationsManager;
    this.groupIDs = new GroupID[addressProviders.length];

    logger.info("Create active channels");
    for (int i = 0; i < addressProviders.length; ++i) {
      ClientMessageChannel channel = this.communicationsManager
          .createClientChannel(this.sessionProvider, -1, null, 0, 10000, addressProviders[i],
                               TransportHandshakeMessage.NO_CALLBACK_PORT, null, this.msgFactory,
                               new TCMessageRouterImpl());
      GroupID groupID = new GroupID(addressProviders[i].getGroupId());
      groupIDs[i] = groupID;
      groupToChannelMap.put(groupID, channel);
      logger.info("Created sub-channel[" + i + "]:" + addressProviders[i]);
    }
    setClientID(ClientID.NULL_ID);
    setServerID(GroupID.NULL_ID);
  }

  private ClientMessageChannel getChannel(int id) {
    return (ClientMessageChannel) groupToChannelMap.get(groupIDs[id]);
  }

  private int chSize() {
    return groupIDs.length;
  }

  public ClientMessageChannel getActiveCoordinator() {
    return getChannel(0);
  }

  public ChannelID getActiveActiveChannelID() {
    return getActiveCoordinator().getChannelID();
  }

  public NodeID makeNodeMultiplexId(ChannelID cid, ConnectionAddressProvider addressProvider) {
    // XXX ....
    return (new NodeIDImpl(addressProvider + cid.toString(), addressProvider.toString().getBytes()));
  }

  public ClientMessageChannel[] getChannels() {
    return (ClientMessageChannel[]) groupToChannelMap.values().toArray();
  }

  public GroupID[] getGroupIDs() {
    return groupIDs;
  }

  public TCMessage createMessage(GroupID groupID, TCMessageType type) {
    ClientMessageChannel ch = (ClientMessageChannel) groupToChannelMap.get(groupID);
    Assert.assertNotNull(ch);
    TCMessage rv = msgFactory.createMessage(ch, type);
    return rv;
  }

  public TCMessage createMessage(TCMessageType type) {
    return createMessage(new GroupID(0), type);
  }

  private String connectionInfo(ClientMessageChannel ch) {
    return (ch.getLocalAddress() + " -> " + ch.getRemoteAddress());
  }

  public NetworkStackID open() throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException {
    NetworkStackID nid = null;
    for (int i = 0; i < chSize(); ++i) {
      ClientMessageChannel ch = getChannel(i);
      try {
        if (i != 0) {
          ch.setClientID(getClientID());
        }
        nid = ch.open();
        if (i == 0) {
          setClientID(new ClientID(getChannelID()));
        }
      } catch (TCTimeoutException e) {
        throw new TCTimeoutException(connectionInfo(ch) + " " + e);
      } catch (UnknownHostException e) {
        throw new UnknownHostException(connectionInfo(ch) + " " + e);
      } catch (MaxConnectionsExceededException e) {
        throw new MaxConnectionsExceededException(connectionInfo(ch) + " " + e);
      }
      logger.info("Opened sub-channel: " + connectionInfo(ch));
    }
    logger.info("all active sub-channels opened");
    return nid;
  }

  public ChannelID getChannelID() {
    // return one of active-coordinator, they are same for all channels
    return getActiveCoordinator().getChannelID();
  }

  public int getConnectCount() {
    // an aggregate of all channels
    int count = 0;
    for (int i = 0; i < chSize(); ++i)
      count += getChannel(i).getConnectCount();
    return count;
  }

  public int getConnectAttemptCount() {
    // an aggregate of all channels
    int count = 0;
    for (int i = 0; i < chSize(); ++i)
      count += getChannel(i).getConnectAttemptCount();
    return count;
  }

  public void routeMessageType(TCMessageType messageType, TCMessageSink dest) {
    for (int i = 0; i < chSize(); ++i)
      getChannel(i).routeMessageType(messageType, dest);
  }

  /*
   * broadcast messages
   */
  public void broadcast(final TCMessageImpl message) {
    message.dehydrate();
    for (int i = 0; i < chSize(); ++i) {
      TCMessageImpl tcMesg = (TCMessageImpl) getChannel(i).createMessage(message.getMessageType());
      tcMesg.cloneAndSend(message);
    }
    message.wasSent();
  }

  public void send(final TCNetworkMessage message) {
    getActiveCoordinator().send(message);
  }

  public TCSocketAddress getRemoteAddress() {
    return getActiveCoordinator().getRemoteAddress();
  }

  public void notifyTransportConnected(MessageTransport transport) {
    throw new AssertionError();
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    throw new AssertionError();
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    throw new AssertionError();
  }

  public void notifyTransportClosed(MessageTransport transport) {
    throw new AssertionError();
  }

  public ChannelIDProvider getChannelIDProvider() {
    // return one from active-coordinator
    return getActiveCoordinator().getChannelIDProvider();
  }

  public void close() {
    for (int i = 0; i < chSize(); ++i)
      getChannel(i).close();
  }

  public boolean isConnected() {
    if (chSize() == 0) return false;
    for (int i = 0; i < chSize(); ++i) {
      if (!getChannel(i).isConnected()) return false;
    }
    return true;
  }

  public boolean isOpen() {
    if (chSize() == 0) return false;
    for (int i = 0; i < chSize(); ++i) {
      if (!getChannel(i).isOpen()) return false;
    }
    return true;
  }

  public ClientMessageChannel channel() {
    // return the active-coordinator
    return getActiveCoordinator();
  }

  /*
   * As a middleman between ClientHandshakeManager and multiple ClientMessageChannels. Bookkeeping sub-channels' events
   * Notify connected only when all channel connected. Notify disconnected when any channel disconnected Notify closed
   * when any channel closed
   */
  private class ClientGroupMessageChannelEventListener implements ChannelEventListener {
    private final ChannelEventListener listener;
    private HashSet                    connectedSet = new HashSet();
    private final ClientMessageChannel channel;

    public ClientGroupMessageChannelEventListener(ChannelEventListener listener, ClientMessageChannel channel) {
      this.listener = listener;
      this.channel = channel;
    }

    public void notifyChannelEvent(ChannelEvent event) {
      if (event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT) {
        if (connectedSet.remove(event.getChannel())) {
          fireEvent(event);
        }
      } else if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
        connectedSet.add(event.getChannel());
        if (connectedSet.size() == chSize()) {
          fireEvent(event);
        }
      } else if (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT) {
        if (connectedSet.remove(event.getChannel())) {
          fireEvent(event);
        }
      }
    }

    private void fireEvent(ChannelEvent event) {
      listener.notifyChannelEvent(new ChannelEventImpl(event.getType(), channel));
    }
  }

  public void addListener(ChannelEventListener listener) {
    ClientGroupMessageChannelEventListener middleman = new ClientGroupMessageChannelEventListener(listener, this);
    for (int i = 0; i < chSize(); ++i)
      getChannel(i).addListener(middleman);
  }

}