/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

public class NodeID {

  private final String uid;

  public NodeID(String uid) {
    this.uid = uid;
  }

  public int hashCode() {
    return uid.hashCode();
  }

  public boolean equals(Object o) {
    if (o instanceof NodeID) {
      NodeID n = (NodeID) o;
      return this.uid.equals(n.uid);
    }
    return false;
  }

  public String toString() {
    return "NodeID[" + uid + "]";
  }
}
