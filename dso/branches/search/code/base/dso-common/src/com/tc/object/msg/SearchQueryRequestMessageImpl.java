/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.object.SearchRequestID;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author Nabib El-Rahman
 */
public class SearchQueryRequestMessageImpl extends DSOMessageBase implements SearchQueryRequestMessage {

  private final static byte SEARCH_REQUEST_ID = 0;
  private final static byte CACHENAME         = 1;
  private final static byte ATTR_NAME         = 2;
  private final static byte ATTR_VALUE        = 3;
  
  private SearchRequestID requestID;
  private String          cachename;
  private String          attributeName;
  private String          attributeValue;
  
  public SearchQueryRequestMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                       TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  
  public void initialSearchRequestMessage(final SearchRequestID searchRequestID, final String cacheName,
                                          final String attribute, final String value) {
    this.requestID = searchRequestID;
    this.cachename = cacheName;
    this.attributeName = attribute;
    this.attributeValue = attribute;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(CACHENAME, this.cachename);
    putNVPair(ATTR_NAME, this.attributeName);
    putNVPair(ATTR_VALUE, this.attributeValue);
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
      
      case ATTR_NAME:
        this.attributeName = getStringValue();
        return true;
      
      case ATTR_VALUE:
        this.attributeValue = getStringValue();
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
  public String getAttributeName() {
    return this.attributeName;
  }
  
  /**
   * {@inheritDoc}
   */
  public String getAttributeValue() {
    return this.attributeValue;
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

}
