/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author EY
 */
public class TCGroupPingMessage extends DSOMessageBase {
  private final static byte PING_MESSAGE_ID = 1;
  private final int         PING_OK          = 1;
  private final int         PING_DENY        = 0;
  private int               message;

  public TCGroupPingMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out,
                            MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public TCGroupPingMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                            TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }
  
  public void okMessage() {
    this.message = PING_OK;
  }

  public void denyMessage() {
    this.message = PING_DENY;
  }
  
  public boolean isOkMessage() {
    return (this.message == PING_OK);
  }

  protected void dehydrateValues() {
    putNVPair(PING_MESSAGE_ID, message);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case PING_MESSAGE_ID:
        message = getIntValue();
        return true;
      default:
        return false;
    }
  }
}