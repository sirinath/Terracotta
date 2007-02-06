/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.util.Assert;

import java.io.IOException;

public class BufferedDmiDescriptor {

  private final ObjectID       receiverId;
  private final TCByteBuffer[] buff;
  private final int            buffLength;

  public BufferedDmiDescriptor(ObjectID receiverId, TCByteBuffer[] buffers, int buffLength) {
    Assert.pre(receiverId != null);
    Assert.pre(buffers != null);
    this.receiverId = receiverId;
    this.buff = buffers;
    this.buffLength = buffLength;
  }

  public ObjectID getReceiverId() {
    return receiverId;
  }

  public static BufferedDmiDescriptor readFrom(TCByteBufferInputStream in) throws IOException {
    final ObjectID receiverId = new ObjectID(in.readLong());
    final int length = in.readInt();
    final TCByteBufferInputStream buff = in.duplicateAndLimit(length);
    in.skip(length);
    return new BufferedDmiDescriptor(receiverId, buff.toArray(), length);
  }

  public void writeTo(TCByteBufferOutputStream out) {
    out.writeLong(receiverId.toLong());
    out.writeInt(buffLength);
    out.write(buff);
  }

}
