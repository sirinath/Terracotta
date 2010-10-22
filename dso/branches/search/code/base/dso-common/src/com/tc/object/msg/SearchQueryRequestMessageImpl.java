/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.NVPair;
import com.tc.object.session.SessionID;
import com.tc.search.StackOperations;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author Nabib El-Rahman
 */
public class SearchQueryRequestMessageImpl extends DSOMessageBase implements SearchQueryRequestMessage {

  private final static byte SEARCH_REQUEST_ID      = 0;
  private final static byte CACHENAME              = 1;
  private final static byte INCLUDE_KEYS           = 3;
  private final static byte STACK_OPERATION_MARKER = 4;
  private final static byte STACK_NVPAIR_MARKER    = 5;

  private SearchRequestID   requestID;
  private String            cachename;
  private LinkedList        queryStack;
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
                                          final LinkedList stack, boolean keys, Set<String> attributeSet) {
    this.requestID = searchRequestID;
    this.cachename = cacheName;
    this.queryStack = stack;
    this.includeKeys = keys;
    this.attributes = attributeSet;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(CACHENAME, this.cachename);
    putNVPair(INCLUDE_KEYS, this.includeKeys);
    final TCByteBufferOutputStream outStream = getOutputStream();

    outStream.writeInt(this.attributes.size());
    for (final String attribute : this.attributes) {
      outStream.writeString(attribute);
    }

    System.out.println("[queryStack size ] = " + queryStack.size());
    while (queryStack.size() > 0) {
      Object obj = queryStack.removeLast();
      System.out.println("[queryStack obj ] = " + obj);
      if (obj instanceof StackOperations) {
        StackOperations operation = (StackOperations) obj;
        System.out.println("[queryStack stackoperation ] = " + operation);
        System.out.println("[queryStack operation name] = " + operation.name());
        putNVPair(STACK_OPERATION_MARKER, operation.name());
      } else if (obj instanceof NVPair) {
        AbstractNVPair pair = (AbstractNVPair) obj;
        System.out.println("[queryStack pair ] = " + pair);
        putNVPair(STACK_NVPAIR_MARKER, pair);
      }
    }

  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    if (queryStack == null) {
      queryStack = new LinkedList();
    }
    final TCByteBufferInputStream inputStream = getInputStream();
    switch (name) {
      case SEARCH_REQUEST_ID:
        this.requestID = new SearchRequestID(getLongValue());
        return true;

      case CACHENAME:
        this.cachename = getStringValue();
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

      case STACK_OPERATION_MARKER:
        StackOperations operation = StackOperations.valueOf(getStringValue());
        System.out.println("[stackMarker] = " + operation);
        queryStack.addFirst(operation);
        return true;

      case STACK_NVPAIR_MARKER:
        NVPair pair = AbstractNVPair.deserializeInstance(inputStream);
        System.out.println("[stackPair] = " + pair);
        queryStack.addFirst(pair);
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
  public LinkedList getQueryStack() {
    return this.queryStack;
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
