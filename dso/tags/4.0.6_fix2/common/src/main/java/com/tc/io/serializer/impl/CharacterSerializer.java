/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Character
 */
public final class CharacterSerializer implements Serializer {

  @Override
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeChar(((Character)o).charValue());
  }

  @Override
  public Object deserializeFrom(ObjectInput in) throws IOException {
    return Character.valueOf(in.readChar());
  }

  @Override
  public byte getSerializerID() {
    return CHARACTER;
  }

}
