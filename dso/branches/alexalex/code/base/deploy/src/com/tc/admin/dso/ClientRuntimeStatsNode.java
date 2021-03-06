/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClient;

import java.awt.Component;

public class ClientRuntimeStatsNode extends ComponentNode {
  private ApplicationContext        appContext;
  private IClient                   client;
  protected ClientRuntimeStatsPanel runtimeStatsPanel;

  public ClientRuntimeStatsNode(ApplicationContext appContext, IClient client) {
    super("Runtime statistics");
    this.appContext = appContext;
    this.client = client;
    setIcon(ClientsHelper.getHelper().getRuntimeStatsIcon());
  }

  protected ClientRuntimeStatsPanel createRuntimeStatsPanel() {
    return new ClientRuntimeStatsPanel(appContext, client);
  }

  public Component getComponent() {
    if (runtimeStatsPanel == null) {
      runtimeStatsPanel = createRuntimeStatsPanel();
    }
    return runtimeStatsPanel;
  }

  public synchronized void tearDown() {
    super.tearDown();
    if (runtimeStatsPanel != null) {
      runtimeStatsPanel.tearDown();
      runtimeStatsPanel = null;
    }
    appContext = null;
    client = null;
  }
}
