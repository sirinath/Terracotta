/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;

public class TestSynAckMessage extends TestTransportHandshakeMessage implements SynAckMessage {

  public String getErrorContext() {
    throw new ImplementMe();
  }

  public boolean hasErrorContext() {
    throw new ImplementMe();
  }

  public boolean isSyn() {
    return false;
  }

  public boolean isSynAck() {
    return true;
  }

  public boolean isAck() {
    return false;
  }

  public boolean isPing() {
    return false;
  }

  public boolean isPingReply() {
    return false;
  }

  public void recycle() {
    return;
  }

  public TCSocketAddress getPeerHCInfo() {
    return null;
  }

}
