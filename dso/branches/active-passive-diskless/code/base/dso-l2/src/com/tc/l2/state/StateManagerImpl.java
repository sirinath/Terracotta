/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.msg.ClusterStateMessageFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.NodeID;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.util.State;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;

public class StateManagerImpl implements StateManager, Runnable, GroupMessageListener {

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
  }

  public void start() {
    try {
      started.set();
      this.myNodeId = groupManager.join();
      logger.info("L2 Node ID = " + myNodeId);
      runElection();
    } catch (GroupException e) {
      logger.error("Caught Exception :", e);
      throw new AssertionError(e);
    }
  }

  private void runElection() {
    NodeID winner = electionMgr.runElection(myNodeId);
    if (winner == myNodeId) {
      moveToActiveState();
    } else {
      moveToPassiveUnInitialized();
    }
  }

  private synchronized void moveToPassiveUnInitialized() {
    if (state == START_STATE) {
      state = PASSIVE_UNINTIALIZED;
      // TODO:: Start initializing
    } else {
      throw new AssertionError("Cant move to " + PASSIVE_UNINTIALIZED + " from " + state);
    }
  }

  private synchronized void moveToActiveState() {
    if (state == START_STATE || state == PASSIVE_STANDBY) {
      // TODO :: If state == START_STATE publish cluster ID
      state = ACTIVE_COORDINATOR;
      try {
        server.startActiveMode();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    } else {
      throw new AssertionError("Cant move to " + ACTIVE_COORDINATOR + " from " + state);
    }
  }

  public void run() {
    while (true) {
      ThreadUtil.reallySleep(15000);
      consoleLogger.info("Moving to Active state");
      try {
        server.startActiveMode();
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
      ThreadUtil.reallySleep(15000);
      consoleLogger.info("Moving to Passive state");
      try {
        server.stopActiveMode();
      } catch (TCTimeoutException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  /**
   * Message Listener Interface
   */
  public synchronized void messageReceived(NodeID fromNode, GroupMessage msg) {
    if (!(msg instanceof ClusterStateMessage)) { throw new AssertionError(
                                                                          "StateManagerImpl : Received wrong message type :"
                                                                              + msg); }
    ClusterStateMessage clusterMsg = (ClusterStateMessage) msg;
    switch (clusterMsg.getType()) {
      case ClusterStateMessage.START_ELECTION:
        handleStartElectionRequest(clusterMsg);
        break;
      case ClusterStateMessage.ABORT_ELECTION:
        handleElectionAbort(clusterMsg);
        break;
      case ClusterStateMessage.ELECTION_WON:
        handleElectionWonMessage(clusterMsg);
        break;
      default:
        throw new AssertionError("This message shouldn't have been routed here : " + clusterMsg);
    }
  }

  //TODO::COME BACK
  private void handleElectionWonMessage(ClusterStateMessage msg) {
    if (state == ACTIVE_COORDINATOR || !activeNode.isNull()) {
      // This shouldn't happen, force other node to rerun election so that we can abort
      GroupMessage resultConflict = ClusterStateMessageFactory.createResultConflictMessage(msg, EnrollmentFactory
          .createTrumpEnrollment(myNodeId));
      logger.warn("WARNING :: Active Node = " + activeNode + " , " + state
                  + " received Election WON message from another node : " + msg + " : Forcing re-election "
                  + resultConflict);
      try {
        groupManager.sendTo(msg.messageFrom(), resultConflict);
      } catch (GroupException e) {
        throw new AssertionError(e);
      }
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

  private void handleStartElectionRequest(ClusterStateMessage msg) {
    if (state == ACTIVE_COORDINATOR) {
      // This is either a new L2 joining a cluster or a renegade L2. Force it to abort
      GroupMessage abortMsg = ClusterStateMessageFactory.createAbortElectionMessage(msg, EnrollmentFactory
          .createTrumpEnrollment(myNodeId));
      logger.info("Forcing Abort Election for " + msg + " with " + abortMsg);
      try {
        groupManager.sendTo(msg.messageFrom(), abortMsg);
      } catch (GroupException e) {
        throw new AssertionError(e);
      }
    } else {
      electionMgr.handleStartElectionRequest(msg);
    }
  }

}
