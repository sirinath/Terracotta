/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.SearchRequestID;
import com.tc.object.session.SessionID;
import com.tc.search.SearchQueryResult;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * 
 */
public class SearchQueryResponseMessageImpl extends DSOMessageBase implements SearchQueryResponseMessage {

  private final static byte      SEARCH_REQUEST_ID = 0;
  private final static byte      RESULTS_SIZE      = 1;

  private SearchRequestID        requestID;
  private Set<SearchQueryResult> results;

  public SearchQueryResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                        MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SearchQueryResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                        TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  /**
   * {@inheritDoc}
   */
  public void initialSearchResponseMessage(SearchRequestID searchRequestID, Set<SearchQueryResult> searchResults) {
    this.requestID = searchRequestID;
    this.results = searchResults;
  }

  /**
   * {@inheritDoc}
   */
  public SearchRequestID getRequestID() {
    return this.requestID;
  }

  /**
   * {@inheritDoc}
   */
  public Set<SearchQueryResult> getResults() {
    return this.results;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(RESULTS_SIZE, this.results.size());
    int count = 0;

    final TCByteBufferOutputStream outStream = getOutputStream();
    for (SearchQueryResult result : this.results) {
      // TODO: does the key need to be encoded?
      result.serializeTo(outStream);
      count++;
    }
    Assert.assertEquals(this.results.size(), count);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case SEARCH_REQUEST_ID:
        this.requestID = new SearchRequestID(getLongValue());
        return true;

      case RESULTS_SIZE:
        int size = getIntValue();
        this.results = new HashSet((int) (size * 1.5));
        TCByteBufferInput input = getInputStream();
        while (size-- > 0) {
          // TODO: Do we need to decode?
          SearchQueryResult result = new SearchQueryResultImpl();
          result.deserializeFrom(input);
          this.results.add(result);
        }
        return true;
      default:
        return false;
    }

  }

}
