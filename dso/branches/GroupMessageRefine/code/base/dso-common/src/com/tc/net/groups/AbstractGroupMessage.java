/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractGroupMessage implements GroupMessage {

  private static long      nextID           = 0;

  private int              type;
  private MessageID        id;
  private MessageID        requestID;

  private transient NodeID messageOrginator = ServerID.NULL_ID;

  protected AbstractGroupMessage(int type) {
    this.type = type;
    id = getNextID();
    requestID = MessageID.NULL_ID;
  }

  protected AbstractGroupMessage(int type, MessageID requestID) {
    this.type = type;
    id = getNextID();
    this.requestID = requestID;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(type);
    serialOutput.writeLong(id.toLong());
    serialOutput.writeLong(requestID.toLong());
    basicSerializeTo(serialOutput);
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    type = serialInput.readInt();
    id = new MessageID(serialInput.readLong());
    requestID = new MessageID(serialInput.readLong());
    basicDeserializeFrom(serialInput);
    return this;
  }
  
  abstract protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException;
  
  abstract protected void basicSerializeTo(TCByteBufferOutput out);

  private static final synchronized MessageID getNextID() {
    return new MessageID(nextID++);
  }

  public int getType() {
    return type;
  }

  public MessageID getMessageID() {
    return id;
  }

  public MessageID inResponseTo() {
    return requestID;
  }

  public void setMessageOrginator(NodeID n) {
    this.messageOrginator = n;
  }

  public NodeID messageFrom() {
    return messageOrginator;
  }

  protected void writeObjectIDS(TCByteBufferOutput out, Set oids) {
    out.writeInt(oids.size());
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      out.writeLong(oid.toLong());
    }
  }

  protected ObjectIDSet readObjectIDS(TCByteBufferInput in, ObjectIDSet oids) throws IOException {
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      oids.add(new ObjectID(in.readLong()));
    }
    return oids;
  }
}
