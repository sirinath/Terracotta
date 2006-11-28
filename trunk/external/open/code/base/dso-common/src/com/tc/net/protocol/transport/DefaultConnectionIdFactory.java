/*
 * Created on May 11, 2005
 */
package com.tc.net.protocol.transport;

import com.tc.util.UUID;


public class DefaultConnectionIdFactory implements ConnectionIdFactory {

  private long         sequence;

//  private final String serverID = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // This got to be 32 chars hex string
  private final String serverID = UUID.getUUID().toString();

  public synchronized ConnectionID nextConnectionId() {
    return new ConnectionID(sequence++, serverID);
  }
  
  public String getServerID() {
    return serverID;
  }

}
