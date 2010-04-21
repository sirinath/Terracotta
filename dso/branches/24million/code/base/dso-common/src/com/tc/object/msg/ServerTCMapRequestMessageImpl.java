/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;

public class ServerTCMapRequestMessageImpl extends DSOMessageBase implements ServerTCMapRequestMessage {

  private final static byte        REQUEST_TYPE  = 0;
  private final static byte        MAP_OBJECT_ID = 1;
  private final static byte        REQUEST_ID    = 2;
  private final static byte        PORTABLE_KEY  = 3;

  private final static byte        DUMMY_BYTE    = 0x00;

  // TODO::Comeback and verify
  private final static DNAEncoding encoder       = new SerializerDNAEncodingImpl();
  private final static DNAEncoding decoder       = new StorageDNAEncodingImpl();

  private ServerMapRequestType     requestType;
  private ServerMapRequestID       requestID;
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

  public void initializeGetValueRequest(final ServerMapRequestID serverMapRequestID, final ObjectID id, final Object key) {
    Assert.assertTrue(LiteralValues.isLiteralInstance(key));
    this.requestType = ServerMapRequestType.GET_VALUE_FOR_KEY;
    this.requestID = serverMapRequestID;
    this.mapID = id;
    this.portableKey = key;
  }

  public void initializeGetSizeRequest(final ServerMapRequestID serverMapRequestID, final ObjectID id) {
    this.requestType = ServerMapRequestType.GET_SIZE;
    this.requestID = serverMapRequestID;
    this.mapID = id;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());
    putNVPair(REQUEST_ID, this.requestID.toLong());
    putNVPair(REQUEST_TYPE, this.requestType.ordinal());
    switch (this.requestType) {
      case GET_SIZE:
        break;
      case GET_VALUE_FOR_KEY:
        putNVPair(PORTABLE_KEY, DUMMY_BYTE);
        // Directly encode the key
        encoder.encode(this.portableKey, getOutputStream());
        break;
      default:
        throw new AssertionError("Dehydrating message before Request Type is set");
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case MAP_OBJECT_ID:
        this.mapID = new ObjectID(getLongValue());
        return true;

      case REQUEST_ID:
        this.requestID = new ServerMapRequestID(getLongValue());
        return true;

      case REQUEST_TYPE:
        this.requestType = ServerMapRequestType.fromOrdinal(getIntValue());
        return true;

      case PORTABLE_KEY:
        // Read dummy byte
        getByteValue();
        // Directly decode the key
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

  public boolean isGetSizeRequest() {
    return this.requestType == ServerMapRequestType.GET_SIZE;
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

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }
}
