/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;

public interface ServerTCMapRequestMessage extends EventContext {

  public void initialize(ServerMapRequestID requestID, ObjectID mapID, Object portableKey);

  public void send();

  public Object getPortableKey();

  public ObjectID getMapID();

  public ClientID getClientID();
  
  public ServerMapRequestID getRequestID();

}
