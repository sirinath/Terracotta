/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;

import java.util.Collection;
import java.util.Set;

/**
 * Wraps the generic ChannelManager adding slightly different channel visibility than DSO requires (we don't want
 * channels to be visible to other subsystems until they have fully handshaked)
 */
public interface DSOChannelManager {

  public void closeAll(Collection channelIDs);

  public MessageChannel getActiveChannel(ChannelID id) throws NoSuchChannelException;

  public MessageChannel[] getActiveChannels();

  public boolean isActiveID(ChannelID channelID);

  public String getChannelAddress(ChannelID channelID);

  public Set getAllActiveChannelIDs();

  public void addEventListener(DSOChannelManagerEventListener listener);

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID)
      throws NoSuchChannelException;

  public Set getRawChannelIDs();

  public void makeChannelActive(ChannelID channelID, long startIDs, long endIDs, boolean persistent);

  public void makeChannelActiveNoAck(MessageChannel channel);
}
