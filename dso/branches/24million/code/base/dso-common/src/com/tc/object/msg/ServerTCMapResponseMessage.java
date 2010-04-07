/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;

public interface ServerTCMapResponseMessage extends EventContext {

  public void initialize(ObjectID mapID, Object portableKey, Object portableValue);

  public ObjectID getMapID();

  public Object getPortableKey();

  public Object getPortableValue();

  public void send();

}
