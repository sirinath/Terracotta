/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.util.UUID;

import java.util.Collections;
import java.util.Set;

public class DefaultConnectionIdFactory implements ConnectionIdFactory {

  private long         sequence;

  private final String serverID = UUID.getUUID().toString();

  public synchronized ConnectionID nextConnectionId() {
    return new ConnectionID(sequence++, serverID);
  }

  public String getServerID() {
    return serverID;
  }

  public Set loadConnectionIDs() {
    return Collections.EMPTY_SET;
  }

}
