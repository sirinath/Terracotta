/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Objects
 */
public final class ObjectSerializer implements Serializer {
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeObject(o);
  }

  public Object deserializeFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    return in.readObject();
  }

  public byte getSerializerID() {
    return OBJECT;
  }
}