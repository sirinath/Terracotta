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
 */
public class MetaDataDescriptor implements TCSerializable {
  
  private ObjectID sharedObjectID;
  private Map<String, MetaData> metaDatas;
  
  /**
   * default constructor
   */
  public MetaDataDescriptor() {
    sharedObjectID = null;
    metaDatas = null;
  }
  
  /**
   * 
   */
  public MetaDataDescriptor(final ObjectID sharedObjectID) {
    this.sharedObjectID = sharedObjectID;
    this.metaDatas = new HashMap();
  }

  /**
   * 
   */
  public ObjectID getSharedObjectID() {
    return sharedObjectID;
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
  public void addMetaData(String property, MetaData value) {
    if(metaDatas == null) {
      metaDatas = new HashMap<String, MetaData>();
    }
 
    metaDatas.put(property, value);
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    sharedObjectID = new ObjectID(in.readLong());
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
    out.writeLong(sharedObjectID.toLong());
    out.writeInt(metaDatas.size());
    for (Iterator<Map.Entry<String, MetaData>> iter = metaDatas.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry<String, MetaData> entry = iter.next();
      out.writeString(entry.getKey());
      entry.getValue().serializeTo(out);
    }
  }
  
  

}
