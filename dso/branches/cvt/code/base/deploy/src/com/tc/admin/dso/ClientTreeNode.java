/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.PollerNode;

import javax.management.ObjectName;

public class ClientTreeNode extends ComponentNode {
  private DSOClient        m_client;
  private ClientStatsPanel m_clientStats;

  public ClientTreeNode(ConnectionContext cc, DSOClient client) {
    super(client.getRemoteAddress());
    setComponent(new ClientPanel(m_client = client));

    AdminClientContext acc = AdminClient.getContext();
    ObjectName bean = client.getObjectName();
    ComponentNode node;

    node = new PollerNode(acc.getMessage("dso.all.statistics"), m_clientStats = new ClientStatsPanel(cc, bean));
    m_clientStats.setNode(node);
    add(node);
  }

  public DSOClient getClient() {
    return m_client;
  }

  public void tearDown() {
    m_clientStats.stop();
    super.tearDown();
    m_client = null;
    m_clientStats = null;
  }
}
