/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.abortable.AbortedOperationException;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;
import com.tc.search.SearchQueryResults;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.aggregator.Aggregator;

import java.util.List;
import java.util.Set;

/**
 *
 *
 */
public interface RemoteSearchRequestManager extends ClientHandshakeCallback {

  public SearchQueryResults query(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, List<NVPair> sortAttributeMap, List<NVPair> aggregators,
                                  int maxResults, int batchSize) throws AbortedOperationException;

  public SearchQueryResults query(String cachename, List queryStack, Set<String> attributeSet,
                                  Set<String> groupByAttributes, List<NVPair> sortAttributeMap,
                                  List<NVPair> aggregators, int maxResults, int batchSize)
      throws AbortedOperationException;

  public void addResponseForQuery(final SessionID sessionID, final SearchRequestID requestID, GroupID groupIDFrom,
                                  final List<IndexQueryResult> queryResults, final List<Aggregator> aggregators,
                                  final NodeID nodeID, boolean anyCriteriaMatched);

  public void addErrorResponseForQuery(final SessionID sessionID, final SearchRequestID requestID, GroupID groupIDFrom,
                                       final String errorMessage, final NodeID nodeID);

}
