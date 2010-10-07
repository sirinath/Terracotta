/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;

import java.util.Collections;
import java.util.Set;

public class MockRemoteSearchRequestManager implements RemoteSearchRequestManager {

  public void addResponseForQuery(SessionID sessionID, SearchRequestID requestID, Set<String> keys, NodeID nodeID) {
    //
  }

  public boolean hasRequestID(SearchRequestID requestID) {
    return false;
  }

  public Set<String> query(String cachename, String query) {
    return Collections.EMPTY_SET;
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
   //
  }

  public void pause(NodeID remoteNode, int disconnected) {
    //
  }

  public void shutdown() {
    //
  }

  public void unpause(NodeID remoteNode, int disconnected) {
    //
  }

}
