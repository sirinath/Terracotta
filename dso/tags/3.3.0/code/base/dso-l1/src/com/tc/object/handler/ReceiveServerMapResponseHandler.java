/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.RemoteServerMapManager;
import com.tc.object.msg.GetSizeServerMapResponseMessage;
import com.tc.object.msg.GetValueServerMapResponseMessage;
import com.tc.object.msg.ObjectNotFoundServerMapResponseMessage;

public class ReceiveServerMapResponseHandler extends AbstractEventHandler {

  private final RemoteServerMapManager remoteServerMapManager;

  public ReceiveServerMapResponseHandler(final RemoteServerMapManager remoteServerMapManager) {
    this.remoteServerMapManager = remoteServerMapManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof GetSizeServerMapResponseMessage) {
      final GetSizeServerMapResponseMessage responseMsg = (GetSizeServerMapResponseMessage) context;

      this.remoteServerMapManager.addResponseForGetSize(responseMsg.getLocalSessionID(), responseMsg.getMapID(),
                                                        responseMsg.getRequestID(), responseMsg.getSize(), responseMsg
                                                            .getSourceNodeID());
    } else if (context instanceof GetValueServerMapResponseMessage) {
      final GetValueServerMapResponseMessage responseMsg = (GetValueServerMapResponseMessage) context;
      this.remoteServerMapManager.addResponseForKeyValueMapping(responseMsg.getLocalSessionID(),
                                                                responseMsg.getMapID(), responseMsg
                                                                    .getGetValueResponses(), responseMsg
                                                                    .getSourceNodeID());
    } else {
      final ObjectNotFoundServerMapResponseMessage notFoundMsg = (ObjectNotFoundServerMapResponseMessage) context;
      this.remoteServerMapManager.objectNotFoundFor(notFoundMsg.getLocalSessionID(), notFoundMsg.getMapID(),
                                                     notFoundMsg.getRequestID(), notFoundMsg.getSourceNodeID());
    }
  }
}
