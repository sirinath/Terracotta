/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.SearchRequestID;

import java.util.LinkedList;
import java.util.Set;

/**
 * The class represents a query request from the client. the cachename is to identify the index and the query string is
 * our client side query in string form.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchQueryRequestMessage extends TCMessage, MultiThreadedEventContext {

  /**
   * ClientID
   */
  public NodeID getClientID();

  /**
   * Search Identifier. return SearchRequestID requestID
   */
  public SearchRequestID getRequestID();

  /**
   * Initialize message.
   * 
   * @param SearchRequestID searchRequestID
   * @param String cacheName
   * @param LinkedList queryStack
   * @param boolean keys
   * @param Set<String> attributeSet
   */
  public void initialSearchRequestMessage(final SearchRequestID searchRequestID, final String cacheName,
                                          final LinkedList queryStack, final boolean keys,
                                          final Set<String> attributeSet);

  /**
   * Name of cache to query against.
   * 
   * @return String string.
   */
  public String getCachename();

  /**
   * Query stack to search
   * 
   * @return LinkedList liskedlist
   */
  public LinkedList getQueryStack();

  /**
   * Return map of attributes ask for.
   * 
   * @return Set<String>
   */
  public Set<String> getAttributes();

  /**
   * Result should include keys
   * 
   * @return boolean
   */
  public boolean includeKeys();

}
