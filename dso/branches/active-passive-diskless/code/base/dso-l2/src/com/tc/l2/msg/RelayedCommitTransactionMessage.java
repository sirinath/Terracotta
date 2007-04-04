/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionIDGenerator;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RelayedCommitTransactionMessage extends AbstractGroupMessage implements EventContext,
    GlobalTransactionIDGenerator {

  public static final int        RELAYED_COMMIT_TXN_MSG_TYPE = 0;

  private TCByteBuffer[]         batchData;
  private ObjectStringSerializer serializer;
  private Map                    sid2gid;
  private ChannelID              channelID;
  private GlobalTransactionID    lowWaterMark;

  // To make serialization happy
  public RelayedCommitTransactionMessage() {
    super(-1);
  }

  public RelayedCommitTransactionMessage(ChannelID channelID, TCByteBuffer[] batchData,
                                         ObjectStringSerializer serializer, Map sid2gid,
                                         GlobalTransactionID lowWaterMark) {
    super(RELAYED_COMMIT_TXN_MSG_TYPE);
    this.channelID = channelID;
    this.batchData = batchData;
    this.serializer = serializer;
    this.sid2gid = sid2gid;
    this.lowWaterMark = lowWaterMark;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public TCByteBuffer[] getBatchData() {
    return batchData;
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, msgType);
    this.channelID = new ChannelID(in.readLong());
    this.serializer = readObjectStringSerializer(in);
    this.batchData = readByteBuffers(in);
    this.sid2gid = readServerTxnIDglobalTxnIDMapping(in);
    this.lowWaterMark = new GlobalTransactionID(in.readLong());
  }

  private Map readServerTxnIDglobalTxnIDMapping(ObjectInput in) throws IOException {
    int size = in.readInt();
    Map mapping = new HashMap();
    ChannelID cid = channelID;
    for (int i = 0; i < size; i++) {
      TransactionID txnid = new TransactionID(in.readLong());
      GlobalTransactionID gid = new GlobalTransactionID(in.readLong());
      mapping.put(new ServerTransactionID(cid, txnid), gid);
    }
    return mapping;
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, msgType);
    out.writeLong(channelID.toLong());
    writeObjectStringSerializer(out, serializer);
    writeByteBuffers(out, batchData);
    writeServerTxnIDGlobalTxnIDMapping(out);
    out.writeLong(lowWaterMark.toLong());
  }

  private void writeServerTxnIDGlobalTxnIDMapping(ObjectOutput out) throws IOException {
    out.writeInt(sid2gid.size());
    ChannelID cid = channelID;
    for (Iterator i = sid2gid.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      ServerTransactionID sid = (ServerTransactionID) e.getKey();
      Assert.assertEquals(cid, sid.getChannelID());
      out.writeLong(sid.getClientTransactionID().toLong());
      GlobalTransactionID gid = (GlobalTransactionID) e.getValue();
      out.writeLong(gid.toLong());
    }
  }

  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID) {
    GlobalTransactionID gid = (GlobalTransactionID) this.sid2gid.get(serverTransactionID);
    if (gid == null) { throw new AssertionError("no Mapping found for : " + serverTransactionID); }
    return gid;
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return this.lowWaterMark;
  }

}
