/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.msg.ClusterStateMessageFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.NodeID;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;

public class StateManagerImpl implements StateManager, GroupMessageListener, GroupEventsListener {

  private static final TCLogger         logger               = TCLogging.getLogger(StateManagerImpl.class);

  private static final State            ACTIVE_COORDINATOR   = new State("ACTIVE-COORDINATOR");
  private static final State            PASSIVE_UNINTIALIZED = new State("PASSIVE-UNINITIALIZED");
  private static final State            PASSIVE_STANDBY      = new State("PASSIVE-STANDBY");
  private static final State            START_STATE          = new State("START-STATE");

  private final TCLogger                consoleLogger;
  private final DistributedObjectServer server;
  private final GroupManager            groupManager;

  private final SetOnceFlag             started              = new SetOnceFlag(false);

  private NodeID                        myNodeId;
  private NodeID                        activeNode           = NodeID.NULL_ID;
  private State                         state                = START_STATE;
  private ElectionManager               electionMgr;

  public StateManagerImpl(TCLogger consoleLogger, DistributedObjectServer server, GroupManager groupManager) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.groupManager = groupManager;
    this.electionMgr = new ElectionManagerImpl(groupManager);
    this.groupManager.registerForMessages(ClusterStateMessage.class, this);
    this.groupManager.registerForGroupEvents(this);
  }

  public void start() {
    started.set();
    try {
      this.myNodeId = groupManager.join();
    } catch (GroupException e) {
      logger.error("Caught Exception :", e);
      throw new AssertionError(e);
    }
    logger.info("L2 Node ID = " + myNodeId);
    startElection();
  }

  /*
   * TODO:: If multiple nodes starts up at the same time and joins the cluster and the elected ACTIVE dies before any of
   * the passive moves to STANDBY mode, then the cluster might be hung. Fix it by sending more info in the ELECTION_WON
   * message for the first time
   */
  private void startElection() {
    validateElectionStart();
    runElection();
  }

  private synchronized void validateElectionStart() {
    if (state != START_STATE && state != PASSIVE_STANDBY) {
      // 
      throw new AssertionError("Cant initiate Election from current state : " + state);
    }
  }

  private void runElection() {
    NodeID winner = electionMgr.runElection(myNodeId);
    if (winner == myNodeId) {
      moveToActiveState();
    } else {
      // TODO:: Come back and validate if this is needed
      moveToPassiveState();
    }
  }

  private synchronized void moveToPassiveState() {
    if (state == START_STATE) {
      state = PASSIVE_UNINTIALIZED;
      electionMgr.reset();
      consoleLogger.info("Moved to " + state);
      // TODO:: Start initializing Passive Node
    } else if (state == ACTIVE_COORDINATOR) {
      // TODO:: Support this later
      throw new AssertionError("Cant move to " + PASSIVE_UNINTIALIZED + " from " + ACTIVE_COORDINATOR
                               + " at least for now");
    }
  }

  private synchronized void moveToActiveState() {
    if (state == START_STATE || state == PASSIVE_STANDBY) {
      // TODO :: If state == START_STATE publish cluster ID
      state = ACTIVE_COORDINATOR;
      this.activeNode = this.myNodeId;
      consoleLogger.info("Becoming " + state);
      electionMgr.declareWinner(this.myNodeId);
      try {
        server.startActiveMode();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    } else {
      throw new AssertionError("Cant move to " + ACTIVE_COORDINATOR + " from " + state);
    }
  }

  /**
   * Message Listener Interface, TODO::move to a stage
   */
  public synchronized void messageReceived(NodeID fromNode, GroupMessage msg) {
    if (!(msg instanceof ClusterStateMessage)) { throw new AssertionError(
                                                                          "StateManagerImpl : Received wrong message type :"
                                                                              + msg); }
    ClusterStateMessage clusterMsg = (ClusterStateMessage) msg;
    handleClusterStateMessage(clusterMsg);
  }

  private void handleClusterStateMessage(ClusterStateMessage clusterMsg) {
    try {
      switch (clusterMsg.getType()) {
        case ClusterStateMessage.START_ELECTION:
          handleStartElectionRequest(clusterMsg);
          break;
        case ClusterStateMessage.ABORT_ELECTION:
          handleElectionAbort(clusterMsg);
          break;
        case ClusterStateMessage.ELECTION_RESULT:
          handleElectionResultMessage(clusterMsg);
          break;
        case ClusterStateMessage.ELECTION_WON:
          handleElectionWonMessage(clusterMsg);
          break;
        default:
          throw new AssertionError("This message shouldn't have been routed here : " + clusterMsg);
      }
    } catch (GroupException ge) {
      logger.error("Caught Exception while handling Message : " + clusterMsg, ge);
      throw new AssertionError(ge);

    }
  }

  private void handleElectionWonMessage(ClusterStateMessage clusterMsg) {
    if (state == ACTIVE_COORDINATOR) {
      // Cant get Election Won from another node : Split brain
      // TODO:: Add some reconcile path
      logger.error(state + " Received Election Won Msg : " + clusterMsg + ". Possible split brain detected ");
      throw new AssertionError(state + " Received Abort Election Msg : " + clusterMsg
                               + ". Possible split brain detected ");
    }
    this.activeNode = clusterMsg.getEnrollment().getNodeID();
    moveToPassiveState();
  }

  private void handleElectionResultMessage(ClusterStateMessage msg) throws GroupException {
    if (activeNode.equals(msg.getEnrollment().getNodeID())) {
      Assert.assertFalse(NodeID.NULL_ID.equals(activeNode));
      // This wouldnt normally happen, but we agree - so ack
      GroupMessage resultAgreed = ClusterStateMessageFactory.createResultAgreedMessage(msg, msg.getEnrollment());
      logger.info("Agreed with Election Result from " + msg.messageFrom() + " : " + resultAgreed);
      groupManager.sendTo(msg.messageFrom(), resultAgreed);
    } else if (state == ACTIVE_COORDINATOR || !activeNode.isNull()) {
      // This shouldn't happen normally, but is possible when there is some weird network error where A sees B,
      // B sees A/C and C sees B and A is active and C is trying to run election
      // Force other node to rerun election so that we can abort
      GroupMessage resultConflict = ClusterStateMessageFactory.createResultConflictMessage(msg, EnrollmentFactory
          .createTrumpEnrollment(myNodeId));
      logger.warn("WARNING :: Active Node = " + activeNode + " , " + state
                  + " received Election WON message from another node : " + msg + " : Forcing re-election "
                  + resultConflict);
      groupManager.sendTo(msg.messageFrom(), resultConflict);
    } else {
      electionMgr.handleElectionResultMessage(msg);
    }
  }

  private void handleElectionAbort(ClusterStateMessage clusterMsg) {
    if (state == ACTIVE_COORDINATOR) {
      // Cant get Abort back to ACTIVE, if so then there is a split brain
      logger.error(state + " Received Abort Election  Msg : Possible split brain detected ");
      throw new AssertionError(state + " Received Abort Election  Msg : Possible split brain detected ");
    }
    electionMgr.handleElectionAbort(clusterMsg);
  }

  private void handleStartElectionRequest(ClusterStateMessage msg) throws GroupException {
    if (state == ACTIVE_COORDINATOR) {
      // This is either a new L2 joining a cluster or a renegade L2. Force it to abort
      GroupMessage abortMsg = ClusterStateMessageFactory.createAbortElectionMessage(msg, EnrollmentFactory
          .createTrumpEnrollment(myNodeId));
      logger.info("Forcing Abort Election for " + msg + " with " + abortMsg);
      groupManager.sendTo(msg.messageFrom(), abortMsg);
    } else {
      electionMgr.handleStartElectionRequest(msg);
    }
  }

  // TODO:: Make it a handler on a stage
  public synchronized void nodeJoined(NodeID nodeID) {
    logger.info("Node : " + nodeID + " joined the cluster");
    consoleLogger.info("Node : " + nodeID + " joined the cluster");
    if (state == ACTIVE_COORDINATOR) {
      // notify new node
      GroupMessage msg = ClusterStateMessageFactory.createElectionWonMessage(EnrollmentFactory
          .createTrumpEnrollment(this.myNodeId));
      try {
        groupManager.sendTo(nodeID, msg);
      } catch (GroupException e) {
        throw new AssertionError(e);
      }
    }
  }

  public void nodeLeft(NodeID nodeID) {
    logger.warn("Node : " + nodeID + " left the cluster");
    consoleLogger.warn("Node : " + nodeID + " left the cluster");
  }

}
