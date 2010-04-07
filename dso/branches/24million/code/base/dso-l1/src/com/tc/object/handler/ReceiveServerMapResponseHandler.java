/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.RemoteServerMapManager;
import com.tc.object.msg.ServerTCMapResponseMessage;

public class ReceiveServerMapResponseHandler extends AbstractEventHandler {

  private final RemoteServerMapManager remoteServerMapManager;

  public ReceiveServerMapResponseHandler(final RemoteServerMapManager remoteServerMapManager) {
    this.remoteServerMapManager = remoteServerMapManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final ServerTCMapResponseMessage kvMsg = (ServerTCMapResponseMessage) context;
    this.remoteServerMapManager.addResponseForKeyValueMapping(kvMsg.getLocalSessionID(), kvMsg.getMapID(), kvMsg
        .getPortableKey(), kvMsg.getPortableValue(), kvMsg.getSourceNodeID());
  }
}
