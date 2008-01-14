/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.net.groups.TCGroupManager;
import com.tc.net.groups.TCGroupPingMessage;

public class TCGroupPingMessageHandler extends AbstractEventHandler {
  private final TCGroupManager manager;
  
  public TCGroupPingMessageHandler(TCGroupManager manager) {
    this.manager = manager;
  }
  
  public void handleEvent(EventContext context) {
    TCGroupPingMessage ping = (TCGroupPingMessage) context;
    manager.pingReceived(ping);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
  }

}