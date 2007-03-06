/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.context.InComingTransactionContext;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessageFactory;
import com.tc.l2.objectserver.L2ObjectState;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.net.groups.NodeID;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class TransactionRelayHandler extends AbstractEventHandler {

  private final L2ObjectStateManager l2ObjectStateMgr;

  public TransactionRelayHandler(L2ObjectStateManager objectStateManager) {
    this.l2ObjectStateMgr = objectStateManager;
  }

  public void handleEvent(EventContext context) {
    InComingTransactionContext ict = (InComingTransactionContext) context;
    L2ObjectState[] states = l2ObjectStateMgr.getL2ObjectStates();
    for (int i = 0; i < states.length; i++) {
      NodeID nodeID = states[i].getNodeID();
      if (states[i].isInSync()) {
        // Just send the commitTransaction Message, no futher processing is needed
        sendCommitTransactionMessage(nodeID, ict.getCommitTransactionMessage());
      }
    }
  }

  private void sendCommitTransactionMessage(NodeID nodeID, CommitTransactionMessage commitTransactionMessage) {
    RelayedCommitTransactionMessage msg = RelayedCommitTransactionMessageFactory
        .createRelayedCommitTransactionMessage(commitTransactionMessage);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
  }
}
