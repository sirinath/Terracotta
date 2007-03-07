/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.handler.L2ObjectSyncDehydrateHandler;
import com.tc.l2.handler.L2ObjectSyncHandler;
import com.tc.l2.handler.L2ObjectSyncSendHandler;
import com.tc.l2.handler.L2StateChangeHandler;
import com.tc.l2.handler.ServerTransactionAckHandler;
import com.tc.l2.handler.TransactionRelayHandler;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.ServerTxnAckMessage;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.L2ObjectStateManagerImpl;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.l2.objectserver.ReplicatedObjectManagerImpl;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupManagerFactory;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.impl.DistributedObjectServer;

import java.io.IOException;

public class L2HACoordinator implements L2Coordinator, StateChangeListener {

  private static final TCLogger         logger = TCLogging.getLogger(L2HACoordinator.class);

  private final TCLogger                consoleLogger;
  private final DistributedObjectServer server;
  private final StageManager            stageManager;

  private GroupManager                  groupManager;
  private StateManager                  stateManager;
  private ReplicatedObjectManager       rObjectManager;
  private L2ObjectStateManager          l2ObjectStateManager;

  public L2HACoordinator(TCLogger consoleLogger, DistributedObjectServer server, StageManager stageManager) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.stageManager = stageManager;
  }

  public void start() {
    try {
      basicStart();
    } catch (GroupException e) {
      logger.error(e);
      throw new AssertionError(e);
    }
  }

  private void basicStart() throws GroupException {

    final Sink stateChangeSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE,
                                                          new L2StateChangeHandler(), 1, Integer.MAX_VALUE).getSink();
    this.groupManager = GroupManagerFactory.createGroupManager();
    this.stateManager = new StateManagerImpl(consoleLogger, groupManager, stateChangeSink);
    this.stateManager.registerForStateChangeEvents(this);

    this.l2ObjectStateManager = new L2ObjectStateManagerImpl();

    final Sink objectsSyncSink = stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_STAGE,
                                                          new L2ObjectSyncHandler(this.l2ObjectStateManager), 1,
                                                          Integer.MAX_VALUE).getSink();
    stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_DEHYDRATE_STAGE,
                             new L2ObjectSyncDehydrateHandler(), 1, Integer.MAX_VALUE);
    stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE,
                             new L2ObjectSyncSendHandler(this.l2ObjectStateManager), 1, Integer.MAX_VALUE);
    stageManager.createStage(ServerConfigurationContext.TRANSACTION_RELAY_STAGE,
                             new TransactionRelayHandler(this.l2ObjectStateManager), 1, Integer.MAX_VALUE);
    final Sink ackProcessingStage = stageManager
        .createStage(ServerConfigurationContext.SERVER_TRANSACTION_ACK_PROCESSING_STAGE,
                     new ServerTransactionAckHandler(), 1, Integer.MAX_VALUE).getSink();
    this.rObjectManager = new ReplicatedObjectManagerImpl(groupManager, this.stateManager, this.l2ObjectStateManager,
                                                          this.server.getContext().getObjectManager(), objectsSyncSink);

    this.groupManager.routeMessages(ObjectSyncMessage.class, objectsSyncSink);
    this.groupManager.routeMessages(RelayedCommitTransactionMessage.class, objectsSyncSink);
    this.groupManager.routeMessages(ServerTxnAckMessage.class, ackProcessingStage);

    stateManager.start();
  }

  public StateManager getStateManager() {
    return stateManager;
  }

  public ReplicatedObjectManager getReplicatedObjectManager() {
    return rObjectManager;
  }

  public GroupManager getGroupManager() {
    return groupManager;
  }

  public void l2StateChanged(StateChangedEvent sce) {
    if (sce.movedToActive()) {
      rObjectManager.sync();
      try {
        server.startActiveMode();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    } else {
      // TODO:// handle
      logger.info("Recd. " + sce + " ! Ignoring for now !!!!");
    }
  }

}
