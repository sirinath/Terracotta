/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.exception.*;

public class TestRemoteServerMapManager implements RemoteServerMapManager {

  public void addResponseForKeyValueMapping(SessionID localSessionID, ObjectID mapID, Object portableKey,
                                            Object portableValue, NodeID nodeID) {
    throw new ImplementMe();

  }

  public ObjectID getMappingForKey(ObjectID oid, Object portableKey) {
    throw new ImplementMe();
  }

  public long size(ObjectID oid) {
    throw new ImplementMe();
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    throw new ImplementMe();

  }

  public void pause(NodeID remoteNode, int disconnected) {
    throw new ImplementMe();

  }

  public void shutdown() {
    throw new ImplementMe();

  }

  public void unpause(NodeID remoteNode, int disconnected) {
    throw new ImplementMe();

  }

}
