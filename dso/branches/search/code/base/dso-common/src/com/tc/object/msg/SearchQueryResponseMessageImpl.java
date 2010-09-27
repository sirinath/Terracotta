/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.object.SearchRequestID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 
 * 
 */
public class SearchQueryResponseMessageImpl extends DSOMessageBase implements SearchQueryResponseMessage {

  private final static byte SEARCH_REQUEST_ID = 0;
  private final static byte KEYS_SIZE         = 1;

  private SearchRequestID   requestID;
  private Set<String>       keys;

  public SearchQueryResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                        TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  /**
   * {@inheritDoc}
   */
  public void initialSearchResponseMessage(SearchRequestID searchRequestID, Set<String> keySet) {
    this.requestID = searchRequestID;
    this.keys = keySet;
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
  public Set<String> getKeys() {
    return this.keys;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(KEYS_SIZE, this.keys.size());
    int count = 0;

    final TCByteBufferOutputStream outStream = getOutputStream();
    for (Iterator<String> iter = this.keys.iterator(); iter.hasNext();) {
      // TODO: does the key need to be encoded?
      outStream.writeString(iter.next());
      count++;
    }
    Assert.assertEquals(this.keys.size(), count);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case SEARCH_REQUEST_ID:
        this.requestID = new SearchRequestID(getLongValue());
        return true;

      case KEYS_SIZE:
        int size = getIntValue();
        this.keys = new HashSet((int) (size * 1.5));
        while (size-- > 0) {
          // TODO: Do we need to decode?
          final String key = getStringValue();
          this.keys.add(key);
        }
        return true;

      default:
        return false;
    }

  }

}
