/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;

public class ServerTCMapRequestMessageImpl extends DSOMessageBase implements ServerTCMapRequestMessage,
    EventContext {

  private final static byte        MAP_OBJECT_ID = 1;

  // TODO::Comeback and verify
  private static final DNAEncoding encoder       = new SerializerDNAEncodingImpl();
  private static final DNAEncoding decoder       = new StorageDNAEncodingImpl();

  private Object                   portableKey;
  private ObjectID                 mapID;

  public ServerTCMapRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                           final MessageChannel channel, final TCMessageHeader header,
                                           final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public ServerTCMapRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                           final TCByteBufferOutputStream out, final MessageChannel channel,
                                           final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void initialize(final ObjectID id, final Object key) {
    Assert.assertTrue(LiteralValues.isLiteralInstance(key));
    this.mapID = id;
    this.portableKey = key;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());

    // Directly encode the key
    // putNVPair(PORTABLE_KEY, this.portableKey.toString());
    encoder.encode(this.portableKey, getOutputStream());
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case MAP_OBJECT_ID:
        this.mapID = new ObjectID(getLongValue());
        // Directly encode the key
        try {
          this.portableKey = decoder.decode(getInputStream());
        } catch (final ClassNotFoundException e) {
          throw new AssertionError(e);
        }
        return true;
      default:
        return false;
    }
  }

  public ObjectID getMapID() {
    return this.mapID;
  }

  public Object getPortableKey() {
    return this.portableKey;
  }

  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }
}
