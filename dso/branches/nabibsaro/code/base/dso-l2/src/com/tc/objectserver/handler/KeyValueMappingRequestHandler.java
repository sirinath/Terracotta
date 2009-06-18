/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.Sink;
import com.tc.object.msg.KeyValueMappingRequestMessage;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.RequestEntryForKeyContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class KeyValueMappingRequestHandler extends AbstractEventHandler implements EventHandler {

  private Sink          respondToPartialKeysStage;
  private ObjectManager objectManager;

  @Override
  public void handleEvent(final EventContext context) {
    KeyValueMappingRequestMessage kvmContext = (KeyValueMappingRequestMessage) context;
    RequestEntryForKeyContext requestContext = new RequestEntryForKeyContext(kvmContext, this.respondToPartialKeysStage);
    System.err.println("Server : Receive look up request for " + requestContext);
    this.objectManager.lookupObjectsAndSubObjectsFor(kvmContext.getClientID(), requestContext, -1);
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.objectManager = oscc.getObjectManager();
    this.respondToPartialKeysStage = oscc.getStage(ServerConfigurationContext.RESPOND_TO_PARTIAL_KEYS).getSink();
  }
}
