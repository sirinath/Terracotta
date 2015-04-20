/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Float
 */
public final class FloatSerializer implements Serializer {

  @Override
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeFloat(((Float)o).floatValue());
  }

  @Override
  public Object deserializeFrom(ObjectInput in) throws IOException {
    return Float.valueOf(in.readFloat());
  }

  @Override
  public byte getSerializerID() {
    return FLOAT;
  }

}
