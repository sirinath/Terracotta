/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.Sink;
import com.tc.object.msg.ServerTCMapRequestMessage;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.RequestEntryForKeyContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class ServerTCMapRequestHandler extends AbstractEventHandler implements EventHandler {

  private Sink          respondToPartialKeysStage;
  private ObjectManager objectManager;

  @Override
  public void handleEvent(final EventContext context) {
    final ServerTCMapRequestMessage kvmContext = (ServerTCMapRequestMessage) context;
    final RequestEntryForKeyContext requestContext = new RequestEntryForKeyContext(kvmContext,
                                                                                   this.respondToPartialKeysStage);
    this.objectManager.lookupObjectsFor(kvmContext.getClientID(), requestContext);
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.objectManager = oscc.getObjectManager();
    this.respondToPartialKeysStage = oscc.getStage(ServerConfigurationContext.SERVER_MAP_RESPOND_STAGE).getSink();
  }
}
