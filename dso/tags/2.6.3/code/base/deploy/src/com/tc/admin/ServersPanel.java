/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.ContainerResource;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

public class ServersPanel extends XContainer {
  protected AdminClientContext      m_acc;
  protected ServersNode             m_serversNode;
  protected ConnectionContext       m_connectionContext;
  protected XObjectTable            m_clusterMemberTable;
  protected ClusterMemberTableModel m_clusterMemberTableModel;

  public ServersPanel(ServersNode serversNode) {
    super();

    m_acc = AdminClient.getContext();
    m_serversNode = serversNode;
    m_connectionContext = serversNode.getConnectionContext();

    load((ContainerResource) m_acc.topRes.getComponent("ServersPanel"));

    m_clusterMemberTable = (XObjectTable) findComponent("ClusterMembersTable");
    m_clusterMemberTableModel = new ClusterMemberTableModel();
    m_clusterMemberTable.setModel(m_clusterMemberTableModel);
    TableColumnModel colModel = m_clusterMemberTable.getColumnModel();
    colModel.getColumn(0).setCellRenderer(new ClusterMemberStatusRenderer());
    colModel.getColumn(2).setCellRenderer(new XObjectTable.PortNumberRenderer());

    for (int i = 0; i < m_serversNode.getChildCount(); i++) {
      ServerNode serverNode = (ServerNode) m_serversNode.getChildAt(i);
      m_clusterMemberTableModel.addClusterMember(serverNode.getServerConnectionManager());
    }
  }

  void serverStateChanged(final ServerNode serverNode) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ServerConnectionManager scm = serverNode.getServerConnectionManager();
        if(scm != null) {
          int row = m_clusterMemberTableModel.getObjectIndex(scm);
          m_clusterMemberTableModel.fireTableCellUpdated(row, 0);
        } else {
          m_clusterMemberTableModel.fireTableDataChanged();
        }
      }
    });
  }

  public void tearDown() {
    super.tearDown();

    m_acc = null;
    m_serversNode = null;
    m_connectionContext = null;
    m_clusterMemberTable = null;
    m_clusterMemberTableModel = null;
  }
}
