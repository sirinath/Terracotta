/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.metadata.NVPair;
import com.tc.object.metadata.ValueType;

import java.util.List;
import java.util.Map;

/**
 * Context holding search index creation information.
 *  
 * @author Nabib El-Rahman
 */
public class SearchUpsertContext implements MultiThreadedEventContext {
  
  private final String name;
  private final Map<String, ValueType> schema;
  private final List<NVPair> attributes;
  private final Object key;
  
  public SearchUpsertContext(String name, Object key, Map<String,ValueType> schema, List<NVPair> attributes ) {
    this.name = name;
    this.key = key;
    this.schema = schema;
    this.attributes = attributes;
  }

  /**
   * Name of index.
   * 
   * @return String name
   */
  public String getName() {
    return name;
  }

  /**
   * Key for cache entry.
   */
  public Object getKey() {
    return key;
  }
  
  
  /**
   * Index schema. Maps Attribute -> ValueType
   * 
   * @return Map schema.
   */
  public Map<String, ValueType> getSchema() {
    return schema;
  }
  
  
  /**
   * Return List of attributes-value associated with the key.
   * 
   */
  public List<NVPair> getAttributes() {
    return attributes;
  }
  
 
}