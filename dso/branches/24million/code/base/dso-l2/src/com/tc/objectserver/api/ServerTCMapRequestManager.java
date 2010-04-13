/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.objectserver.core.api.ManagedObject;

public interface ServerTCMapRequestManager {

  public void requestValues(ServerMapRequestID serverMapRequestID, ClientID clientID, ObjectID mapID, Object portableKey);
  
  public void sendValues(ClientID clientID, ObjectID mapID, ManagedObject managedObject, Object portableKey);
  
}
