/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;

public interface RemoteServerMapManager extends ClientHandshakeCallback {

  public Object getMappingForKey(ObjectID oid, Object portableKey);

  public void addResponseForKeyValueMapping(SessionID localSessionID, ObjectID mapID, Object portableKey,
                                            Object portableValue, NodeID nodeID);

  public long size(ObjectID oid);
}
