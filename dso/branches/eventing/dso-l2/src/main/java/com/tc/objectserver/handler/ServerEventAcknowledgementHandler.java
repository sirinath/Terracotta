/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.msg.AcknowledgeServerEventMessage;
import com.tc.objectserver.event.InClusterServerEventNotifier;

public class ServerEventAcknowledgementHandler extends AbstractEventHandler {
  private final InClusterServerEventNotifier inClusterServerEventNotifier;

  public ServerEventAcknowledgementHandler(InClusterServerEventNotifier inClusterServerEventNotifier) {
    this.inClusterServerEventNotifier = inClusterServerEventNotifier;
  }

  @Override
  public void handleEvent(EventContext context) {
    AcknowledgeServerEventMessage ackedServerEventMessage = (AcknowledgeServerEventMessage) context;
    inClusterServerEventNotifier.acknowledgement(ackedServerEventMessage.getAcknowledgedGtxIds());
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
  }

}