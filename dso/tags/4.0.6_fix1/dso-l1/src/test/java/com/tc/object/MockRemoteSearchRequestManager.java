/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.search.SearchQueryResults;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.aggregator.Aggregator;

import java.util.List;
import java.util.Set;

public class MockRemoteSearchRequestManager implements RemoteSearchRequestManager {

  @Override
  public void cleanup() {
    //
  }

  @Override
  public void addResponseForQuery(final SessionID sessionID, final SearchRequestID requestID,
                                  final GroupID groupIDFrom, final List<IndexQueryResult> queryResults,
                                  final List<Aggregator> aggregators, final NodeID nodeID,
                                  final boolean anyCriteriaMatched) {
    //
  }

  @Override
  public SearchQueryResults query(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, List<NVPair> sortAttributeMap, List<NVPair> aggregators,
                                  int maxResults, int batchSize) {
    return null;
  }

  @Override
  public SearchQueryResults query(String cachename, List queryStack, Set<String> attributeSet,
                                  Set<String> groupByAttributes, List<NVPair> sortAttributeMap,
                                  List<NVPair> aggregators, int maxResults, int batchSize) {
    return null;
  }

  @Override
  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    //
  }

  @Override
  public void pause(NodeID remoteNode, int disconnected) {
    //
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    //
  }

  @Override
  public void unpause(NodeID remoteNode, int disconnected) {
    //
  }

  @Override
  public void addErrorResponseForQuery(SessionID sessionID, SearchRequestID requestID, GroupID groupIDFrom,
                                       String errorMessage, NodeID nodeID) {
    //
  }

}
