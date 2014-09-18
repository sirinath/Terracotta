/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Integer
 */
public final class IntegerSerializer implements Serializer {

  @Override
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeInt(((Integer)o).intValue());
  }

  @Override
  public Object deserializeFrom(ObjectInput in) throws IOException {
    return Integer.valueOf(in.readInt());
  }

  @Override
  public byte getSerializerID() {
    return INTEGER;
  }

}
