/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author steve
 */
public class ObjectIDBatchRequestResponseMessage extends DSOMessageBase {

  private final static byte REQUEST_ID  = 1;
  private final static byte BATCH_START = 2;
  private final static byte BATCH_END   = 3;

  private long              requestID;
  private long              batchStart;
  private long              batchEnd;

  public ObjectIDBatchRequestResponseMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ObjectIDBatchRequestResponseMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(REQUEST_ID, requestID);
    putNVPair(BATCH_START, batchStart);
    putNVPair(BATCH_END, batchEnd);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case REQUEST_ID:
        requestID = getLongValue();
        return true;
      case BATCH_START:
        batchStart = getLongValue();
        return true;
      case BATCH_END:
        batchEnd = getLongValue();
        return true;
      default:
        return false;
    }
  }

  public void initialize(long reqID, long start, long end) {
    this.requestID = reqID;
    this.batchStart = start;
    this.batchEnd = end;
  }

  public long getBatchStart() {
    return batchStart;
  }

  public long getBatchEnd() {
    return batchEnd;
  }

  public long getRequestID() {
    return requestID;
  }
}