/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.util.Assert;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class NodeID implements Externalizable {

  public static final NodeID  NULL_ID       = new NodeID("NULL-ID", new byte[0]);

  private static final String UNINITIALIZED = "Uninitialized";

  private String              name;
  private byte[]              uid;

  public NodeID() {
    // satisfy serialization
    this.name = UNINITIALIZED;
  }

  public NodeID(String name, byte[] uid) {
    this.name = name;
    this.uid = uid;
  }

  public int hashCode() {
    int hash = 27;
    for (int i = uid.length - 1; i >= 0; i--) {
      hash = 31 * hash + uid[i];
    }
    return hash;
  }

  public boolean equals(Object o) {
    if (o instanceof NodeID) {
      NodeID n = (NodeID) o;
      if (n.uid.length != uid.length) return false;
      for (int i = uid.length - 1; i >= 0; i--) {
        if (uid[i] != n.uid[i]) return false;
      }
      return true;
    }
    return false;
  }

  public byte[] getUID() {
    return uid;
  }

  public String getName() {
    Assert.assertTrue(this.name != UNINITIALIZED);
    return name;
  }

  public String toString() {
    return "NodeID[" + getName() + "]";
  }

  public boolean isNull() {
    return NULL_ID.equals(this);
  }

  public void readExternal(ObjectInput in) throws IOException {
    this.name = in.readUTF();
    int length = in.readInt();
    this.uid = new byte[length];
    for (int i = length - 1; i >= 0; i--) {
      uid[i] = in.readByte();
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    Assert.assertTrue(this.name != UNINITIALIZED);
    out.writeUTF(this.name);
    int length = this.uid.length;
    out.writeInt(length);
    for (int i = length - 1; i >= 0; i--) {
      out.writeByte(this.uid[i]);
    }
  }
}
