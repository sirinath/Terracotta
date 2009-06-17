/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.object.ObjectID;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class RequestValueMappingForKeyMessageImpl extends DSOMessageBase implements RequestValueMappingForKeyMessage {

  private final static byte PORTABLE_KEY  = 1;
  private final static byte MAP_OBJECT_ID = 2;

  private Object            portableKey;
  private ObjectID          mapID;

  public RequestValueMappingForKeyMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                              TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(ObjectID id, Object key) {
    this.mapID = id;
    this.portableKey = key;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MAP_OBJECT_ID, this.mapID.toLong());

    // TODO::XXX::FIXME:BROKEN dehydrate portable key
    putNVPair(PORTABLE_KEY, this.portableKey.toString());
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case MAP_OBJECT_ID:
        this.mapID = new ObjectID(getLongValue());
        return true;
      default:
        return false;
    }
  }

}
