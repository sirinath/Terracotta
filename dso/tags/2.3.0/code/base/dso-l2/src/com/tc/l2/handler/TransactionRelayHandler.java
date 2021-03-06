/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.exception.ImplementMe;
import com.tc.l2.context.IncomingTransactionContext;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessageFactory;
import com.tc.l2.objectserver.L2ObjectState;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;

import java.util.Iterator;
import java.util.List;

public class TransactionRelayHandler extends AbstractEventHandler {
  private static final TCLogger      logger = TCLogging.getLogger(TransactionRelayHandler.class);
  

  private final L2ObjectStateManager l2ObjectStateMgr;
  private GroupManager groupManager;


  private ServerTransactionManager transactionManager;

  public TransactionRelayHandler(L2ObjectStateManager objectStateManager) {
    this.l2ObjectStateMgr = objectStateManager;
  }

  public void handleEvent(EventContext context) {
    IncomingTransactionContext ict = (IncomingTransactionContext) context;
    List states = l2ObjectStateMgr.getL2ObjectStates();
    for (Iterator i = states.iterator(); i.hasNext();) {
      L2ObjectState state = (L2ObjectState) i.next();
      NodeID nodeID = state.getNodeID();
      if (state.isInSync()) {
        // Just send the commitTransaction Message, no futher processing is needed
        sendCommitTransactionMessage(nodeID, ict);
      } else {
        //TODO::
        System.err.println("WARNING :: Unimplemented yet - " + state );
        throw new ImplementMe();
      }
    }
    transactionManager.transactionsRelayed(ict.getChannelID(), ict.getServerTransactionIDs());
  }

  private void sendCommitTransactionMessage(NodeID nodeID, IncomingTransactionContext ict) {
    addWaitForNotification(nodeID, ict);
    RelayedCommitTransactionMessage msg = RelayedCommitTransactionMessageFactory
        .createRelayedCommitTransactionMessage(ict.getCommitTransactionMessage());
    try {
      this.groupManager.sendTo(nodeID, msg);
    } catch (GroupException e) {
      logger.error("Removing " + nodeID + " from group because of Exception :", e);
      groupManager.zapNode(nodeID);
    }
  }

  private void addWaitForNotification(NodeID nodeID, IncomingTransactionContext ict) {
    ChannelID waitee = nodeID.toChannelID();
    //TODO::avoid this loop and thus N lookups in transactionManager
    for (Iterator i = ict.getServerTransactionIDs().iterator(); i.hasNext();) {
      ServerTransactionID stxnID = (ServerTransactionID) i.next();
      transactionManager.addWaitingForAcknowledgement(ict.getChannelID(), stxnID.getClientTransactionID(), waitee);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.groupManager = oscc.getL2Coordinator().getGroupManager();
    this.transactionManager = oscc.getTransactionManager();
  }
}
