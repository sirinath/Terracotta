/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ObjectID;

public interface ServerTCMapResponseMessage extends TCMessage {

  public void initialize(ObjectID mapID, Object portableKey, Object portableValue);

  public ObjectID getMapID();

  public Object getPortableKey();

  public Object getPortableValue();

  public void send();

}
