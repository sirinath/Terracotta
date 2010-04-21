/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.object.msg.ServerTCMapRequestMessage;
import com.tc.objectserver.api.ServerTCMapRequestManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class ServerTCMapRequestHandler extends AbstractEventHandler implements EventHandler {

  private ServerTCMapRequestManager serverTCMapRequestManager;

  @Override
  public void handleEvent(final EventContext context) {
    final ServerTCMapRequestMessage smContext = (ServerTCMapRequestMessage) context;
    if (smContext.isGetSizeRequest()) {
      this.serverTCMapRequestManager.requestSize(smContext.getRequestID(), smContext.getClientID(), smContext
          .getMapID());
    } else {
      this.serverTCMapRequestManager.requestValues(smContext.getRequestID(), smContext.getClientID(), smContext
          .getMapID(), smContext.getPortableKey());
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.serverTCMapRequestManager = oscc.getServerTCMapRequestManager();
  }

}
