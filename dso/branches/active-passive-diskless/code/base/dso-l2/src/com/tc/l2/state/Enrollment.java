/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.net.groups.NodeID;
import com.tc.util.Assert;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Enrollment implements Externalizable {

  private NodeID nodeID;
  private int[]  weights;

  public Enrollment(NodeID nodeID) {
    this.nodeID = nodeID;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public void setWeight(int[] weights) {
    Assert.assertNotNull(weights);
    this.weights = weights;
  }

  public boolean wins(Enrollment other) {
    int myLength = weights.length;
    int otherLength = other.weights.length;
    if (myLength > otherLength) {
      return true;
    } else if (myLength < otherLength) {
      return false;
    } else {
      for (int i = 0; i < myLength; i++) {
        if (weights[i] > other.weights[i]) {
          return true;
        } else if (weights[i] < other.weights[i]) { return true; }
      }

      // XXX:: Both are the same weight. This should happen once we fix the weights to
      // be unique (based on hardware,ip,process id etc.) But now it is possible and we
      // handle it. If two nodes dont agree because of this there will be a re-election
      return false;
    }
  }

  public void readExternal(ObjectInput in) throws IOException {
    this.nodeID = new NodeID(in.readUTF());
    this.weights = new int[in.readInt()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = in.readInt();
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeUTF(nodeID.getID());
    out.writeInt(weights.length);
    for (int i = 0; i < weights.length; i++) {
      out.writeInt(weights[i]);
    }
  }
  
  public int hashCode() {
    return nodeID.hashCode();
  }
  
  public boolean equals(Object o) {
    if (o instanceof Enrollment) {
      Enrollment oe = (Enrollment) o;
      return nodeID.equals(oe.nodeID);
    }
    return false;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("Enrollment [ ");
    sb.append(nodeID).append(", weights = ");
    int length = weights.length;
    for (int i = 0; i < length; i++) {
      sb.append(weights[i]);
      if (i < length - 1) {
        sb.append(",");
      }
    }
    sb.append(" ]");
    return sb.toString();
  }

}
