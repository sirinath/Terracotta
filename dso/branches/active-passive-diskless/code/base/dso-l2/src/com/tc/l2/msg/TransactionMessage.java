/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TransactionMessage extends AbstractGroupMessage {

  public static final int        MANAGED_OBJECT_SYNC_TYPE = 0;
  
  private Set                    oids;
  private int                    dnaCount;
  private TCByteBuffer[]         dnas;
  private ObjectStringSerializer serializer;

  public TransactionMessage() {
    // Make serialization happy
    super(-1);
  }

  public TransactionMessage(int type) {
    super(type);
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    Assert.assertEquals(MANAGED_OBJECT_SYNC_TYPE, msgType);
    readObjectIDS(in);
    dnaCount = in.readInt();
    readSerializer(in);
    this.dnas = readByteBuffers(in);
    throw new AssertionError("Summa");
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(MANAGED_OBJECT_SYNC_TYPE, msgType);
    writeObjectIDS(out);
    out.writeInt(dnaCount);
    writeSerializer(out);
    writeByteBuffers(out, dnas);
  }

  private void writeSerializer(ObjectOutput out) throws IOException {
    TCByteBufferOutputStream tcbo = new TCByteBufferOutputStream();
    serializer.serializeTo(tcbo);
    writeByteBuffers(out, tcbo.toArray());
  }

  private void readSerializer(ObjectInput in) throws IOException {
    TCByteBuffer buffers[]  = readByteBuffers(in);
    serializer = new ObjectStringSerializer();
    serializer.deserializeFrom(new TCByteBufferInputStream(buffers));
  }

  private TCByteBuffer[] readByteBuffers(ObjectInput in) throws IOException {
    int size = in.readInt();
    TCByteBuffer buffers[] = new TCByteBuffer[size];
    for (int i = 0; i < buffers.length; i++) {
      int length = in.readInt();
      byte bytes[] = new byte[length];
      int start = 0;
      while(length > 0) {
        int read = in.read(bytes, start, length);
        start+=read;
        length-=read;
      }
      buffers[i] = TCByteBufferFactory.wrap(bytes);
    }
    return buffers;
  }

  private void writeByteBuffers(ObjectOutput out, TCByteBuffer[] buffers) throws IOException {
    out.writeInt(buffers.length);
    for (int i = 0; i < buffers.length; i++) {
      TCByteBuffer buffer = buffers[i];
      int length = buffer.limit();
      out.writeInt(length);
      out.write(buffer.array(), buffer.arrayOffset(), length);
    }
  }

  private void writeObjectIDS(ObjectOutput out) throws IOException {
    out.writeInt(oids.size());
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      out.writeLong(oid.toLong());
    }
  }

  private void readObjectIDS(ObjectInput in) throws IOException {
    int size = in.readInt();
    oids = new HashSet(size);
    for (int i = 0; i < size; i++) {
      oids.add(new ObjectID(in.readLong()));
    }
  }

  public void initialize(Set dnaOids, int count, TCByteBuffer[] serializedDNAs, ObjectStringSerializer objectSerializer) {
    this.oids = dnaOids;
    this.dnaCount = count;
    this.dnas = serializedDNAs;
    this.serializer = objectSerializer;
  }

}
