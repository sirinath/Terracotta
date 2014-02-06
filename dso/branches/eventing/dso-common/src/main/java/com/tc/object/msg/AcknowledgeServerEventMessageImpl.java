/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.google.common.collect.Sets;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Set;

/**
 * @author manish
 */
public class AcknowledgeServerEventMessageImpl extends DSOMessageBase implements AcknowledgeServerEventMessage {
  private final static DNAEncoding  encoder      = new SerializerDNAEncodingImpl();
  private final static DNAEncoding  decoder      = new SerializerDNAEncodingImpl();
  private final static byte         ACKED_GTXIDS = 1;

  private Set<GlobalTransactionID> acknowledgedGtxIds;


  public AcknowledgeServerEventMessageImpl(final SessionID sessionID, final MessageMonitor monitor, final TCByteBufferOutputStream out,
                                           final MessageChannel channel, final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public AcknowledgeServerEventMessageImpl(final SessionID sessionID, final MessageMonitor monitor, final MessageChannel channel,
                                           final TCMessageHeader header, final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(ACKED_GTXIDS, acknowledgedGtxIds.size());
    
    int txnCount = 0;
    final TCByteBufferOutputStream outStream = getOutputStream();

    for (GlobalTransactionID gtxId : acknowledgedGtxIds) {
      encoder.encode(gtxId.toLong(), outStream);
      txnCount++;
    }
    Assert.assertEquals(acknowledgedGtxIds.size(), txnCount);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case ACKED_GTXIDS:
        try {
          int txnCount = getIntValue();
          acknowledgedGtxIds = Sets.newHashSet();
          final TCByteBufferInputStream inputStream = getInputStream();
          while (txnCount-- > 0) {
            acknowledgedGtxIds.add(new GlobalTransactionID((Long) decoder.decode(inputStream)));
          }
        } catch (ClassNotFoundException e) {
          throw new AssertionError(e);
        }
        return true;
      default:
        return false;
    }
  }

  @Override
  public void initialize(Set<GlobalTransactionID> acknowledgedGtxIds) {
    this.acknowledgedGtxIds = acknowledgedGtxIds;

  }

  @Override
  public Set<GlobalTransactionID> getAcknowledgedGtxIds() {
    return this.acknowledgedGtxIds;
  }

}
