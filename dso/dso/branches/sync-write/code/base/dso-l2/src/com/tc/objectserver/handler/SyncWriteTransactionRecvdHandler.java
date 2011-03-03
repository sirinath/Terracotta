/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.SyncWriteTransactionRecvdMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.context.SyncWriteTransactionRecvdContext;

/**
 * This class is responsible for acking back to the clients when it receives a sync write transaction
 */
public class SyncWriteTransactionRecvdHandler extends AbstractEventHandler {
  private DSOChannelManager     channelManager;
  private final static TCLogger logger = TCLogging.getLogger(SyncWriteTransactionRecvdHandler.class.getName());

  public SyncWriteTransactionRecvdHandler(DSOChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  public void handleEvent(final EventContext context) {
    // send the message to the client
    SyncWriteTransactionRecvdContext syncCxt = (SyncWriteTransactionRecvdContext) context;
    ClientID cid = syncCxt.getClientID();
    long batchId = syncCxt.getBatchID();

    MessageChannel channel;
    try {
      channel = this.channelManager.getActiveChannel(cid);
    } catch (NoSuchChannelException e) {
      // Dont do anything
      logger.info("Cannot find channel for client " + cid + ". It might already be dead");
      return;
    }
    SyncWriteTransactionRecvdMessage message = (SyncWriteTransactionRecvdMessage) channel
        .createMessage(TCMessageType.SYNC_WRITE_TRANSACTION_RECVD_MESSAGE);
    message.initialize(batchId);

    message.send();
  }
}
