/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.cluster.Cluster;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ClientIDProvider;
import com.tc.object.context.PauseContext;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.State;
import com.tc.util.Util;
import com.tc.util.sequence.BatchSequenceReceiver;

import java.util.Collection;
import java.util.Iterator;

public class ClientHandshakeManagerImpl implements ClientHandshakeManager, ChannelEventListener {
  private static final State                  PAUSED             = new State("PAUSED");
  private static final State                  STARTING           = new State("STARTING");
  private static final State                  RUNNING            = new State("RUNNING");

  private final Collection                    callBacks;
  private final ClientIDProvider              cidp;
  private final ClientHandshakeMessageFactory chmf;
  private final TCLogger                      logger;
  private final Sink                          pauseSink;
  private final SessionManager                sessionManager;
  private final BatchSequenceReceiver         sequenceReceiver;
  private final Cluster                       cluster;
  private final String                        clientVersion;
  private volatile boolean                    serverIsPersistent = false;

  private State                               state              = PAUSED;

  public ClientHandshakeManagerImpl(TCLogger logger, ClientIDProvider clientIDProvider,
                                    ClientHandshakeMessageFactory chmf, Sink pauseSink, SessionManager sessionManager,
                                    BatchSequenceReceiver sequenceReceiver, Cluster cluster, String clientVersion,
                                    Collection callbacks) {
    this.logger = logger;
    this.cidp = clientIDProvider;
    this.chmf = chmf;
    this.pauseSink = pauseSink;
    this.sessionManager = sessionManager;
    this.sequenceReceiver = sequenceReceiver;
    this.cluster = cluster;
    this.clientVersion = clientVersion;
    this.callBacks = callbacks;
    pauseCallbacks(GroupID.ALL_GROUPS);
  }

  public void initiateHandshake(NodeID remoteNode) {
    logger.debug("Initiating handshake...");
    changeState(STARTING);

    ClientHandshakeMessage handshakeMessage = chmf.newClientHandshakeMessage();
    handshakeMessage.setClientVersion(clientVersion);
    handshakeMessage.setIsObjectIDsRequested(!sequenceReceiver.hasNext());

    notifyCallbackOnHandshake(remoteNode, handshakeMessage);

    logger.debug("Sending handshake message...");
    handshakeMessage.send();
  }

  public void notifyChannelEvent(ChannelEvent event) {
    if (event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT) {
      cluster.thisNodeDisconnected();
      pauseSink.add(new PauseContext(true, event.getChannel().getRemoteNodeID()));
    } else if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
      pauseSink.add(new PauseContext(false, event.getChannel().getRemoteNodeID()));
    } else if (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT) {
      cluster.thisNodeDisconnected();
    }
  }

  public void disconnected(NodeID remoteNode) {
    logger.info("Disconnected: Pausing from " + getState() + " RemoteNode : " + remoteNode);
    if (getState() == PAUSED) {
      logger.warn("Pause called while already PAUSED");
      // ClientMessageChannel moves to next SessionID, need to move to newSession here too.
    } else {
      pauseCallbacks(remoteNode);
      changeState(PAUSED);
      // all the activities paused then can switch to new session
      sessionManager.newSession();
      logger.info("ClientHandshakeManager moves to " + sessionManager);
    }
  }

  public void connected(NodeID remoteNode) {
    logger.info("Connected: Unpausing from " + getState() + " RemoteNode : " + remoteNode);
    if (getState() != PAUSED) {
      logger.warn("Unpause called while not PAUSED: " + getState());
      return;
    }
    initiateHandshake(remoteNode);
  }

  public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck) {
    acknowledgeHandshake(handshakeAck.getSourceNodeID(), handshakeAck.getPersistentServer(), handshakeAck
        .getThisNodeId(), handshakeAck.getAllNodes(), handshakeAck.getServerVersion(), handshakeAck.getChannel());
  }

  protected void acknowledgeHandshake(NodeID remoteID, boolean persistentServer, String thisNodeId,
                                      String[] clusterMembers, String serverVersion, MessageChannel channel) {
    logger.info("Received Handshake ack for this node :" + remoteID);
    if (getState() != STARTING) {
      logger.warn("Handshake acknowledged while not STARTING: " + getState());
      return;
    }

    checkClientServerVersionMatch(serverVersion);
    this.serverIsPersistent = persistentServer;
    cluster.thisNodeConnected(thisNodeId, clusterMembers);
    unpauseCallbacks(remoteID);
    changeState(RUNNING);
  }

  protected void checkClientServerVersionMatch(String serverVersion) {
    final boolean checkVersionMatches = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_CONNECT_VERSION_MATCH_CHECK);
    if (checkVersionMatches && !clientVersion.equals(serverVersion)) {
      final String msg = "Client/Server Version Mismatch Error: Client Version: " + clientVersion
                         + ", Server Version: " + serverVersion + ".  Terminating client now.";
      throw new RuntimeException(msg);
    }
  }

  private void pauseCallbacks(NodeID remote) {
    for (Iterator i = callBacks.iterator(); i.hasNext();) {
      ClientHandshakeCallback c = (ClientHandshakeCallback) i.next();
      c.pause(remote);
    }
  }

  private void notifyCallbackOnHandshake(NodeID remote, ClientHandshakeMessage handshakeMessage) {
    for (Iterator i = callBacks.iterator(); i.hasNext();) {
      ClientHandshakeCallback c = (ClientHandshakeCallback) i.next();
      c.initializeHandshake(cidp.getClientID(), remote, handshakeMessage);
    }
  }

  private void unpauseCallbacks(NodeID remote) {
    for (Iterator i = callBacks.iterator(); i.hasNext();) {
      ClientHandshakeCallback c = (ClientHandshakeCallback) i.next();
      c.unpause(remote);
    }
  }

  public boolean serverIsPersistent() {
    return this.serverIsPersistent;
  }

  public synchronized void waitForHandshake() {
    boolean isInterrupted = false;
    while (state != RUNNING) {
      try {
        wait();
      } catch (InterruptedException e) {
        logger.error("Interrupted while waiting for handshake");
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  private synchronized void changeState(State newState) {
    state = newState;
    notifyAll();
  }

  private synchronized State getState() {
    return state;
  }

}
