/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;

public interface ServerMapRequestMessage extends EventContext {

  public void send();

  public ObjectID getMapID();

  public ClientID getClientID();

  public ServerMapRequestID getRequestID();

  public ServerMapRequestType getRequestType();
}
