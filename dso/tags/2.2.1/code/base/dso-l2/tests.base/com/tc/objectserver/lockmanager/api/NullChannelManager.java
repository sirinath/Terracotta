/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;

import java.util.Collection;
import java.util.Collections;

/**
 * @author steve
 */
public class NullChannelManager implements DSOChannelManager {

  public boolean isActiveID(ChannelID channelID) {
    return true;
  }

  public MessageChannel getActiveChannel(ChannelID id) {
    throw new UnsupportedOperationException();
  }

  public MessageChannel[] getActiveChannels() {
    return new MessageChannel[] {};
  }

  public void closeAll(Collection channelIDs) {
    return;
  }

  public String getChannelAddress(ChannelID channelID) {
    return "";
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID) {
    throw new UnsupportedOperationException();
  }

  public ClientHandshakeAckMessage newClientHandshakeAckMessage(ChannelID channelID) {
    throw new UnsupportedOperationException();
  }

  public Collection getAllActiveChannelIDs() {
    return Collections.EMPTY_LIST;
  }

  public void addEventListener(DSOChannelManagerEventListener listener) {
    //
  }

  public void makeChannelActive(MessageChannel channel, ClientHandshakeAckMessage ackMsg) {
    //
  }

  public Collection getRawChannelIDs() {
    return Collections.EMPTY_LIST;
  }

}