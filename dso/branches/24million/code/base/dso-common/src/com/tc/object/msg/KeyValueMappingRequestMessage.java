/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;

public interface KeyValueMappingRequestMessage {

  public void initialize(ObjectID mapID, Object portableKey);

  public void send();

  public Object getPortableKey();

  public ObjectID getMapID();

  public ClientID getClientID();

}
