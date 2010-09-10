/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class hold the Metadata information associated with a shared object.
 * 
 * @author Nabib
 */
public class MetaDataDescriptor implements TCSerializable {

  private static final MetaDataDescriptor  TEMPLATE    = new MetaDataDescriptor("template");
  public static final MetaDataDescriptor[] EMPTY_ARRAY = new MetaDataDescriptor[0];

  private final String                     category;
  private final List<NVPair>               metaDatas;

  public MetaDataDescriptor(String category) {
    this(category, new ArrayList<NVPair>());
  }

  private MetaDataDescriptor(String category, List<NVPair> metaDatas) {
    this.category = category;
    this.metaDatas = metaDatas;
  }

  public List<NVPair> getMetaDatas() {
    return metaDatas;
  }

  public void addNameValuePair(NVPair nvPair) {
    this.metaDatas.add(nvPair);
  }

  public String getCategory() {
    return this.category;
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    final String cat = in.readString();
    final int size = in.readInt();
    List<NVPair> data = new ArrayList<NVPair>(size);

    for (int i = 0; i < size; i++) {
      data.add(NVPair.deserializeInstance(in));
    }

    return new MetaDataDescriptor(cat, data);
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeString(category);
    out.writeInt(metaDatas.size());
    for (NVPair nvpair : metaDatas) {
      nvpair.serializeTo(out);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + category + "): " + metaDatas.toString();
  }

  public static MetaDataDescriptor deserializeInstance(TCByteBufferInputStream in) throws IOException {
    return (MetaDataDescriptor) TEMPLATE.deserializeFrom(in);
  }

}
