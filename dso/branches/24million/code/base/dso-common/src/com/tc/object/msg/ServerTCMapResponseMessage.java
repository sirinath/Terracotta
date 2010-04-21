/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;

public interface ServerTCMapResponseMessage extends TCMessage {

  public void initializeGetValueResponse(ObjectID mapID, ServerMapRequestID requestID, Object portableValue);

  public void initializeGetSizeResponse(ObjectID mapID, ServerMapRequestID requestID, Integer size);

  public ObjectID getMapID();

  public ServerMapRequestID getRequestID();

  public Object getPortableValue();

  public Integer getSize();

  public ServerMapRequestType getRequestType();

  public void send();

}
