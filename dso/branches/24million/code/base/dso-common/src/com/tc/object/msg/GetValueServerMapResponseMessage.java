/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;

public interface GetValueServerMapResponseMessage extends ServerMapResponseMessage {

  public void initializeGetValueResponse(ObjectID mapID, ServerMapRequestID requestID, Object portableValue);

  public Object getPortableValue();

}
