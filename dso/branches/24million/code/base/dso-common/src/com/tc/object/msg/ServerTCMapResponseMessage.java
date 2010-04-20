/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;

public interface ServerTCMapResponseMessage extends TCMessage {

  public void initialize(ObjectID mapID, ServerMapRequestID requestID, Object portableValue);

  public ObjectID getMapID();

  public ServerMapRequestID getRequestID();

  public Object getPortableValue();

  public void send();

}
