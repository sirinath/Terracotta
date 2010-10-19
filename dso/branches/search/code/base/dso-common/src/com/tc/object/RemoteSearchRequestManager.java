/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;
import com.tc.search.SearchQueryResult;

import java.util.Set;

/**
 * 
 * 
 */
public interface RemoteSearchRequestManager extends ClientHandshakeCallback {

  public Set<SearchQueryResult> query(String cachename, String query, boolean includeKeys, Set<String> attributeSet);

  public void addResponseForQuery(final SessionID sessionID, final SearchRequestID requestID,
                                  final Set<SearchQueryResult> results, final NodeID nodeID);

  public boolean hasRequestID(SearchRequestID requestID);

}
