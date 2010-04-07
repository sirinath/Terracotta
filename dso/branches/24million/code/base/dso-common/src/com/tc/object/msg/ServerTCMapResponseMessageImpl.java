/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class ServerTCMapResponseMessageImpl extends DSOMessageBase implements ServerTCMapResponseMessage {

  private final static byte        MAP_OBJECT_ID = 1;

  // TODO::Comeback and verify
  private static final DNAEncoding encoder       = new StorageDNAEncodingImpl();
  // Since ApplicatorDNAEncodingImpl is only available in the client, some tricker to get this reference set.
  private final DNAEncoding        decoder;

  private ObjectID                 mapID;
  private Object                   portableKey;
  private Object                   portableValue;

  public ServerTCMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                        final MessageChannel channel, final TCMessageHeader header,
                                        final TCByteBuffer[] data, final DNAEncoding decoder) {
    super(sessionID, monitor, channel, header, data);
    this.decoder = decoder;
  }

  public ServerTCMapResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                        final TCByteBufferOutputStream out, final MessageChannel channel,
                                        final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
    this.decoder = null; // shouldn't be used
  }

  public void initialize(final ObjectID mapObjectID, final Object key, final Object value) {
    this.mapID = mapObjectID;
    this.portableKey = key;
    // Null Value is not supported in CDSM
    this.portableValue = (value == null ? ObjectID.NULL_ID : value);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());
    encoder.encode(this.portableKey, getOutputStream());
    encoder.encode(this.portableValue, getOutputStream());
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case MAP_OBJECT_ID:
        this.mapID = new ObjectID(getLongValue());
        // Directly decode the key and value
        try {
          this.portableKey = this.decoder.decode(getInputStream());
          this.portableValue = this.decoder.decode(getInputStream());
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

  public Object getPortableValue() {
    return this.portableValue;
  }

}
