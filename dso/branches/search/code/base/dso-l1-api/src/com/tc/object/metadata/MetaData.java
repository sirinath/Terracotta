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
 * 
 */
public class MetaData implements TCSerializable {
  
  private Map<String, String> properties;
  
  public MetaData() {
    properties = new HashMap<String, String>();
  }
  
  public void addProperty(String property, String value) {
    if(properties == null) {
      properties = new HashMap();
    }
    properties.put(property, value);
  }
  
  public Map<String, String> getProperties() {
    return properties;
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    final int size = in.readInt();
    properties = new HashMap();
    for(int i = 0; i < size; i++) {
      final String property = in.readString();
      final String value = in.readString();
      properties.put(property, value);
    }   
    return this;
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeInt(properties.size());
    for(Iterator<Map.Entry<String,String>> iter = properties.entrySet().iterator(); iter.hasNext();) {
      Map.Entry<String, String> entry = iter.next();
      out.writeString(entry.getKey());
      out.writeString(entry.getValue());
    }
  }

  
}
