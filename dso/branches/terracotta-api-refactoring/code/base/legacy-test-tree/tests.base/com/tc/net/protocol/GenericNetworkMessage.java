/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.ITCByteBuffer;
import com.tc.net.core.TCConnection;

/**
 * A generic network messge. Not really useful except for testing
 * 
 * @author teck
 */
public class GenericNetworkMessage extends AbstractTCNetworkMessage {
  private final TCConnection source;
  private boolean            sent = false;

  public GenericNetworkMessage(TCConnection source, ITCByteBuffer data) {
    this(source, new ITCByteBuffer[] { data });
  }

  public GenericNetworkMessage(TCConnection source, ITCByteBuffer data[]) {
    super(new GenericNetworkHeader(), data);

    GenericNetworkHeader hdr = (GenericNetworkHeader) getHeader();

    int msgLength = 0;
    for (int i = 0; i < data.length; i++) {
      msgLength += data[i].limit();
    }

    hdr.setMessageDataLength(msgLength);
    this.source = source;
  }

  GenericNetworkMessage(TCConnection source, TCNetworkHeader header, ITCByteBuffer[] payload) {
    super(header, payload);
    this.source = source;
  }

  public void setSequence(int seq) {
    ((GenericNetworkHeader) getHeader()).setSequence(seq);
  }

  public int getSequence() {
    return ((GenericNetworkHeader) getHeader()).getSequence();
  }

  public void setClientNum(int num) {
    ((GenericNetworkHeader) getHeader()).setClientNum(num);
  }

  public int getClientNum() {
    return ((GenericNetworkHeader) getHeader()).getClientNum();
  }

  public TCConnection getSource() {
    return source;
  }

  public synchronized void waitUntilSent() throws InterruptedException {
    while (!sent) {
      wait();
    }
  }

  public synchronized void setSent() {
    this.sent = true;
    notifyAll();
  }
}