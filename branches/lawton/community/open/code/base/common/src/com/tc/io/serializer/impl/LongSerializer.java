package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Long
 */
public final class LongSerializer implements Serializer {

  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeLong(((Long)o).longValue());
  }

  public Object deserializeFrom(ObjectInput in) throws IOException {
    return new Long(in.readLong());
  }

  public byte getSerializerID() {
    return LONG;
  }
  
}