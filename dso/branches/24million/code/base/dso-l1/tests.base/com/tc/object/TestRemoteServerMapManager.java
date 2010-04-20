/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;

public class TestRemoteServerMapManager implements RemoteServerMapManager {

  public ObjectID getMappingForKey(final ObjectID oid, final Object portableKey) {
    throw new ImplementMe();
  }

  public int getSize(final ObjectID oid) {
    throw new ImplementMe();
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    throw new ImplementMe();

  }

  public void pause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();

  }

  public void shutdown() {
    throw new ImplementMe();

  }

  public void unpause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();

  }

  public void addResponseForKeyValueMapping(final SessionID localSessionID, final ObjectID mapID,
                                            final ServerMapRequestID requestID, final Object portableValue,
                                            final NodeID nodeID) {
    throw new ImplementMe();
  }

}
