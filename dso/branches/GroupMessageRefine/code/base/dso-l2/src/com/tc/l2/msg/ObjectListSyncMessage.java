/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.util.Set;

public class ObjectListSyncMessage extends AbstractGroupMessage {

  public static final int REQUEST         = 0;
  public static final int RESPONSE        = 1;
  public static final int FAILED_RESPONSE = 2;

  private Set             oids;

  // To make serialization happy
  public ObjectListSyncMessage() {
    super(-1);
  }

  public ObjectListSyncMessage(int type) {
    super(type);
  }

  public ObjectListSyncMessage(MessageID messageID, int type, Set oids) {
    super(type, messageID);
    this.oids = oids;
  }

  public ObjectListSyncMessage(MessageID messageID, int type) {
    super(type, messageID);
  }

  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    switch (getType()) {
      case REQUEST:
      case FAILED_RESPONSE:
        // Nothing to read
        break;
      case RESPONSE:
        oids = readObjectIDS(in, new ObjectIDSet());
        break;
      default:
        throw new AssertionError("Unknown Message Type : " + getType());
    }
  }

  protected void basicSerializeTo(TCByteBufferOutput out) {
    switch (getType()) {
      case REQUEST:
      case FAILED_RESPONSE:
        // Nothing to write
        break;
      case RESPONSE:
        Assert.assertNotNull(oids);
        // XXX::Directly serializing instead of using writeObjectIDs() to avoid HUGE messages. Since the (wrapped) set
        // is ObjectIDSet2 and since it has optimized externalization methods, this should result in far less data
        // written out.
        writeObjectIDS(out, oids);
        break;
      default:
        throw new AssertionError("Unknown Message Type : " + getType());
    }
  }

  public Set getObjectIDs() {
    Assert.assertNotNull(oids);
    return oids;
  }

  public String toString() {
    return "ObjectListSyncMessage [ " + messageFrom() + ", type = " + getTypeString() + ", " + oids + "]";
  }

  private String getTypeString() {
    switch (getType()) {
      case REQUEST:
        return "REQUEST";
      case FAILED_RESPONSE:
        return "FAILED_RESPONSE";
      case RESPONSE:
        return "RESPONSE";
      default:
        throw new AssertionError("Unknow Type ! : " + getType());
    }
  }

}
