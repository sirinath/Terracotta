/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.RemoteObjectManager;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.msg.RespondToKeyValueMappingRequestMessage;

public class ReceiveObjectHandler extends AbstractEventHandler {
  private RemoteObjectManager objectManager;

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof RequestManagedObjectResponseMessage) {
      RequestManagedObjectResponseMessage m = (RequestManagedObjectResponseMessage) context;
      this.objectManager.addAllObjects(m.getLocalSessionID(), m.getBatchID(), m.getObjects(), m.getSourceNodeID());
    } else if (context instanceof ObjectsNotFoundMessage) {
      ObjectsNotFoundMessage notFound = (ObjectsNotFoundMessage) context;
      this.objectManager.objectsNotFoundFor(notFound.getLocalSessionID(), notFound.getBatchID(), notFound
          .getMissingObjectIDs(), notFound.getSourceNodeID());
    } else if (context instanceof RespondToKeyValueMappingRequestMessage) {
      RespondToKeyValueMappingRequestMessage kvMsg = (RespondToKeyValueMappingRequestMessage) context;
      this.objectManager.addResponseForKeyValueMapping(kvMsg.getLocalSessionID(), kvMsg.getMapID(), kvMsg
          .getPortableKey(), kvMsg.getPortableValue(), kvMsg.getSourceNodeID());
    } else {
      throw new AssertionError("Unsupported type : " + context);
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.objectManager = ccc.getObjectManager();
  }

}