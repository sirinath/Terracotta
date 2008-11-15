/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.object.msg.ClientHandshakeAckMessage;

public interface ClientHandshakeManager {

  public void initiateHandshake();

  public void pause();

  public void unpause();

  public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck);

  public boolean serverIsPersistent();

  public void waitForHandshake();

}
