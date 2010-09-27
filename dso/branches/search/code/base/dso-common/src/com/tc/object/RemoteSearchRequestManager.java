/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;

import java.util.Set;

/**
 * 
 * 
 */
public interface RemoteSearchRequestManager extends ClientHandshakeCallback {
  
  public Set<String> query(String cachename, String attributeName, String attributeValue);
  
  public void addResponseForQuery(final SessionID sessionID, final SearchRequestID requestID,
                                               final Set<String> keys, final NodeID nodeID);
  
  public boolean hasRequestID(SearchRequestID requestID);

}
