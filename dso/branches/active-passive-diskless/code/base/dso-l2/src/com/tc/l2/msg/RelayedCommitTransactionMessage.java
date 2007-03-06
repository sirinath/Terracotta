/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RelayedCommitTransactionMessage extends AbstractGroupMessage {

  public static final int        RELAYED_COMMIT_TXN_MSG_TYPE = 0;

  private TCByteBuffer[]         batchData;
  private ObjectStringSerializer serializer;

  // To make serialization happy
  public RelayedCommitTransactionMessage() {
    super(-1);
  }

  public RelayedCommitTransactionMessage(TCByteBuffer[] batchData, ObjectStringSerializer serializer) {
    super(RELAYED_COMMIT_TXN_MSG_TYPE);
    this.batchData = batchData;
    this.serializer = serializer;
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, msgType);
    this.serializer = readObjectStringSerializer(in);
    this.batchData = readByteBuffers(in);
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, msgType);
    writeObjectStringSerializer(out,serializer);
    writeByteBuffers(out, batchData);
  }

}
