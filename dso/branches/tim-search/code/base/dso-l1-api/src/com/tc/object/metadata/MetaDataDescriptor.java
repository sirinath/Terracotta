/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class hold the Metadata information associated with a shared object.
 * 
 * @author Nabib
 */
public class MetaDataDescriptor implements TCSerializable {
  
  public static final MetaDataDescriptor[] EMPTY_ARRAY = new MetaDataDescriptor[0];

  private String category;
  private Map<String, String> metaDatas;

  /**
   * default constructor
   */
  public MetaDataDescriptor() {
    this.category = null;
    this.metaDatas = null;
  }

  /**
   * 
   */
  public MetaDataDescriptor(String category) {
    this.category = category;
    this.metaDatas = new HashMap<String, String>();
  }

  /**
   * 
   */
  public Map<String, String> getMetaDatas() {
    return metaDatas;
  }

  /**
   * 
   */
  public void addProperties(Map<String,String> properties) {
    this.metaDatas = properties;
  }

  /**
   * 
   */
  public String getCategory() {
    return this.category;
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    final int size = in.readInt();
    metaDatas = new HashMap<String, String>();
    for (int i = 0; i < size; i++) {
      final String property = in.readString();
      final String value = in.readString();
      metaDatas.put(property, value);
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeInt(metaDatas.size());
    for (Iterator<Map.Entry<String, String>> iter = metaDatas.entrySet().iterator(); iter.hasNext();) {
      Map.Entry<String, String> entry = iter.next();
      out.writeString(entry.getKey());
      out.writeString(entry.getValue());
    }
  }

  @Override
  public String toString() {
    if (metaDatas != null) {
      return metaDatas.toString();
    } else {
      return super.toString();
    }
  }

}
