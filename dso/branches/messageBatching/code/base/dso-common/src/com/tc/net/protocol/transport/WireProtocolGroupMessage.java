/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import java.util.Iterator;

public interface WireProtocolGroupMessage extends WireProtocolMessage {

  public Iterator<WireProtocolMessage> getMessageIterator();

  public int getTotalMessageCount();

}
