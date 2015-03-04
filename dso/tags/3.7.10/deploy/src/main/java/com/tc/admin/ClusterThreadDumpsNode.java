/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;

import java.awt.Component;

public class ClusterThreadDumpsNode extends ComponentNode {
  protected final ApplicationContext        appContext;
  protected final ClusterThreadDumpProvider threadDumpProvider;

  protected ClusterThreadDumpsPanel         clusterThreadDumpsPanel;

  public ClusterThreadDumpsNode(ApplicationContext appContext, ClusterThreadDumpProvider threadDumpProvider) {
    super();

    this.appContext = appContext;
    this.threadDumpProvider = threadDumpProvider;

    setLabel(appContext.getString("cluster.thread.dumps"));
    setIcon(ServerHelper.getHelper().getThreadDumpsIcon());
  }

  protected ClusterThreadDumpsPanel createClusterThreadDumpsPanel() {
    return new ClusterThreadDumpsPanel(appContext, threadDumpProvider);
  }

  @Override
  public Component getComponent() {
    if (clusterThreadDumpsPanel == null) {
      clusterThreadDumpsPanel = createClusterThreadDumpsPanel();
    }
    return clusterThreadDumpsPanel;
  }

  ClusterThreadDumpEntry takeThreadDump() {
    return threadDumpProvider.takeThreadDump();
  }

  @Override
  public void tearDown() {
    if (clusterThreadDumpsPanel != null) {
      clusterThreadDumpsPanel.tearDown();
    }
    super.tearDown();
  }
}
