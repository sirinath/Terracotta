/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class L2StateChangeHandler extends AbstractEventHandler {

  private StateManager stateManager;

  public void handleEvent(EventContext context) {
    StateChangedEvent sce = (StateChangedEvent) context;
    stateManager.fireStateChangedEvent(sce);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.stateManager = oscc.getL2Coordinator().getStateManager();
  }

}
