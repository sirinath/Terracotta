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
import com.tc.object.session.SessionID;

import java.io.IOException;

public class RespondToKeyValueMappingRequestMessageImpl extends DSOMessageBase implements
    RespondToKeyValueMappingRequestMessage {

  private final static byte PORTABLE_KEY   = 1;
  private final static byte PORTABLE_VALUE = 2;
  private final static byte MAP_OBJECT_ID  = 3;

  private ObjectID          mapID;
  private Object            portableKey;
  private Object            portableValue;

  public RespondToKeyValueMappingRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                                    final MessageChannel channel, final TCMessageHeader header,
                                                    final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public RespondToKeyValueMappingRequestMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                                    final TCByteBufferOutputStream out, final MessageChannel channel,
                                                    final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void initialize(final ObjectID mapObjectID, final Object key, final Object value) {
    this.mapID = mapObjectID;
    this.portableKey = key;
    // TODO::Null Value is not supported
    this.portableValue = (value == null ? ObjectID.NULL_ID : value);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());
    // TODO::XXX::FIXME:BROKEN dehydrate portable key
    if (this.portableKey instanceof String) {
      putNVPair(PORTABLE_KEY, this.portableKey.toString());
    } else {
      throw new AssertionError("Unsupported type key : " + this.portableKey);
    }
    if (this.portableValue instanceof ObjectID) {
      putNVPair(PORTABLE_VALUE, ((ObjectID) this.portableValue).toLong());
    } else {
      throw new AssertionError("Unsupported type value : " + this.portableValue);
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case MAP_OBJECT_ID:
        this.mapID = new ObjectID(getLongValue());
        return true;
      case PORTABLE_KEY:
        // TODO::XXX::FIXME:BROKEN hydrate portable key
        this.portableKey = getStringValue();
        return true;
      case PORTABLE_VALUE:
        // TODO::XXX::FIXME:BROKEN hydrate portable key
        this.portableValue = new ObjectID(getLongValue());
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
