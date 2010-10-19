/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.SearchRequestID;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Nabib El-Rahman
 */
public class SearchQueryRequestMessageImpl extends DSOMessageBase implements SearchQueryRequestMessage {

  private final static byte SEARCH_REQUEST_ID = 0;
  private final static byte CACHENAME         = 1;
  private final static byte QUERY             = 2;
  private final static byte INCLUDE_KEYS      = 3;

  private SearchRequestID   requestID;
  private String            cachename;
  private String            query;
  private boolean           includeKeys;
  private Set<String>       attributes;

  public SearchQueryRequestMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                       MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SearchQueryRequestMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                       TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialSearchRequestMessage(final SearchRequestID searchRequestID, final String cacheName,
                                          final String queryString, boolean keys, Set<String> attributeSet) {
    this.requestID = searchRequestID;
    this.cachename = cacheName;
    this.query = queryString;
    this.includeKeys = keys;
    this.attributes = attributeSet;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(CACHENAME, this.cachename);
    putNVPair(QUERY, this.query);
    putNVPair(INCLUDE_KEYS, this.includeKeys);
    final TCByteBufferOutputStream outStream = getOutputStream();

    outStream.writeInt(this.attributes.size());
    for (final String attribute : this.attributes) {
      outStream.writeString(attribute);
    }

  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case SEARCH_REQUEST_ID:
        this.requestID = new SearchRequestID(getLongValue());
        return true;

      case CACHENAME:
        this.cachename = getStringValue();
        return true;

      case QUERY:
        this.query = getStringValue();
        return true;

      case INCLUDE_KEYS:
        this.includeKeys = getBooleanValue();
        this.attributes = new HashSet<String>();
        int count = getIntValue();
        // Directly decode the key
        while (count-- > 0) {
          String attribute = getStringValue();
          this.attributes.add(attribute);
        }
        return true;

      default:
        return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getCachename() {
    return this.cachename;
  }

  /**
   * {@inheritDoc}
   */
  public String getQuery() {
    return this.query;
  }

  /**
   * {@inheritDoc}
   */
  public SearchRequestID getRequestID() {
    return requestID;
  }

  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return getSourceNodeID();
  }

  /**
   * {@inheritDoc}
   */
  public NodeID getClientID() {
    return getSourceNodeID();
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getAttributes() {
    return this.attributes;
  }

  /**
   * {@inheritDoc}
   */
  public boolean includeKeys() {
    return this.includeKeys;
  }

}
