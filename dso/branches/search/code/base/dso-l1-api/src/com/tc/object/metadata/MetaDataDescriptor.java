/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;

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


  private Map<String, MetaData> metaDatas;

  /**
   * default constructor
   */
  public MetaDataDescriptor() {
    metaDatas = null;
  }

  /**
   * 
   */
  public MetaDataDescriptor(final ObjectID sharedObjectID) {
    this.metaDatas = new HashMap();
  }

  /**
   * 
   */
  public Map<String, MetaData> getMetaDatas() {
    return metaDatas;
  }

  /**
   * 
   */
  public MetaData getMetaData(String category) {
    return this.metaDatas.get(category);
  }

  /**
   * 
   */
  public void addMetaData(String category, MetaData value) {
    if (metaDatas == null) {
      metaDatas = new HashMap<String, MetaData>();
    }

    metaDatas.put(category, value);
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    final int size = in.readInt();
    metaDatas = new HashMap<String, MetaData>();
    for (int i = 0; i < size; i++) {
      final String property = in.readString();
      MetaData obj = new MetaData();
      obj.deserializeFrom(in);
      metaDatas.put(property, obj);
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeInt(metaDatas.size());
    for (Iterator<Map.Entry<String, MetaData>> iter = metaDatas.entrySet().iterator(); iter.hasNext();) {
      Map.Entry<String, MetaData> entry = iter.next();
      out.writeString(entry.getKey());
      entry.getValue().serializeTo(out);
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
