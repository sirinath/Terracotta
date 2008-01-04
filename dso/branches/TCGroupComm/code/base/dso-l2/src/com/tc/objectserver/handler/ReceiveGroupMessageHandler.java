/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.net.groups.TCGroupMembership;
import com.tc.net.groups.TCGroupMessageWrapper;

public class ReceiveGroupMessageHandler extends AbstractEventHandler {
  private final TCGroupMembership membership;
  
  public ReceiveGroupMessageHandler(TCGroupMembership membership) {
    this.membership = membership;
  }
  
  public void handleEvent(EventContext context) {
    TCGroupMessageWrapper wrapper = (TCGroupMessageWrapper) context;
    membership.messageReceived(wrapper.getGroupMessage(), wrapper.getChannel());
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
  }

}