/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;

public interface GetValueServerMapRequestMessage extends ServerMapRequestMessage {

  public void initializeGetValueRequest(ServerMapRequestID requestID, ObjectID mapID, Object portableKey);

  public Object getPortableKey();

}
