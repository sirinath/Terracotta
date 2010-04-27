/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.object.msg.ServerMapRequestMessage;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class ServerMapRequestHandler extends AbstractEventHandler implements EventHandler {

  private ServerMapRequestManager serverTCMapRequestManager;

  @Override
  public void handleEvent(final EventContext context) {
    final ServerMapRequestMessage smContext = (ServerMapRequestMessage) context;
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
