/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public abstract class AbstractGroupMessage implements GroupMessage {

  private static long      nextID           = 0;

  private int              type;
  private MessageID        id;
  private MessageID        requestID;

  private transient NodeID messageOrginator = NodeID.NULL_ID;

  // protected AbstractGroupMessage() {
  // // To make serialization happy
  // }

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

  public final void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    type = in.readInt();
    id = new MessageID(in.readLong());
    requestID = new MessageID(in.readLong());
    basicReadExternal(type, in);

  }

  public final void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(type);
    out.writeLong(id.toLong());
    out.writeLong(requestID.toLong());
    basicWriteExternal(type, out);
  }

  protected abstract void basicWriteExternal(int msgType, ObjectOutput out) throws IOException;

  protected abstract void basicReadExternal(int msgType, ObjectInput in) throws IOException, ClassNotFoundException;
}
