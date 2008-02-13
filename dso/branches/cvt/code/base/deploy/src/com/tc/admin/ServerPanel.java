/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.Button;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.ScrollPane;
import org.dijon.TextArea;
import org.dijon.ToggleButton;

import com.tc.admin.common.StatusRenderer;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.ClientsHelper;
import com.tc.admin.dso.DSOClient;
import com.tc.config.schema.L2Info;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.util.Assert;

import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableColumnModel;

public class ServerPanel extends XContainer {
  private AdminClientContext      m_acc;
  private ServerNode              m_serverNode;
  private JTextField              m_hostField;
  private JTextField              m_portField;
  private JButton                 m_connectButton;
  static private ImageIcon        m_connectIcon;
  static private ImageIcon        m_disconnectIcon;
  private Container               m_runtimeInfoPanel;
  private StatusView              m_statusView;
  private JButton                 m_shutdownButton;
  private ProductInfoPanel        m_productInfoPanel;
  private ProductInfoPanel        m_altProductInfoPanel;    // Displayed if the RuntimeInfoPanel is not.
  private XObjectTable            m_clusterMemberTable;
  private ClusterMemberTableModel m_clusterMemberTableModel;

  private TextArea                m_environmentTextArea;
  private TextArea                m_configTextArea;
  
  private Button                  m_threadDumpButton;
  private XTree                   m_threadDumpTree;
  private XTreeModel              m_threadDumpTreeModel;
  private TextArea                m_threadDumpTextArea;
  private ScrollPane              m_threadDumpTextScroller;
  private ThreadDumpTreeNode      m_lastSelectedThreadDumpTreeNode;
  
  private ToggleButton            m_startGatheringStatsButton;
  private ToggleButton            m_stopGatheringStatsButton;
  private StatisticsManagerMBean  m_statisticsManagerMBean;
  private StatisticsEmitterMBean  m_statisticsEmitterMBean;
  private long                    m_currentStatsSessionId;
  private StatsEmitterListener    m_statsEmitterListener;
  private TextArea                m_statisticsLog;
  
  static {
    m_connectIcon = new ImageIcon(ServerPanel.class.getResource("/com/tc/admin/icons/disconnect_co.gif"));
    m_disconnectIcon = new ImageIcon(ServerPanel.class.getResource("/com/tc/admin/icons/newex_wiz.gif"));
  }

  public ServerPanel(ServerNode serverNode) {
    super(serverNode);

    m_serverNode = serverNode;
    m_acc = AdminClient.getContext();

    load((ContainerResource) m_acc.topRes.getComponent("ServerPanel"));

    m_hostField = (JTextField) findComponent("HostField");
    m_portField = (JTextField) findComponent("PortField");
    m_connectButton = (JButton) findComponent("ConnectButton");
    m_runtimeInfoPanel = (Container) findComponent("RuntimeInfoPanel");
    m_statusView = (StatusView) findComponent("StatusIndicator");
    m_shutdownButton = (JButton) findComponent("ShutdownButton");
    m_productInfoPanel = (ProductInfoPanel) findComponent("ProductInfoPanel");
    m_clusterMemberTable = (XObjectTable) findComponent("ClusterMembersTable");
    m_clusterMemberTableModel = new ClusterMemberTableModel();
    m_clusterMemberTable.setModel(m_clusterMemberTableModel);
    TableColumnModel colModel = m_clusterMemberTable.getColumnModel();
    colModel.getColumn(0).setCellRenderer(new ClusterMemberStatusRenderer());
    colModel.getColumn(2).setCellRenderer(new XObjectTable.PortNumberRenderer());

    m_statusView.setLabel("Not connected");
    m_runtimeInfoPanel.setVisible(false);

    m_hostField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String host = m_hostField.getText().trim();

        m_serverNode.setHost(host);
        m_acc.controller.nodeChanged(m_serverNode);
        m_acc.controller.updateServerPrefs();
      }
    });

    m_portField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String port = m_portField.getText().trim();

        try {
          m_serverNode.setPort(Integer.parseInt(port));
          m_acc.controller.nodeChanged(m_serverNode);
          m_acc.controller.updateServerPrefs();
        } catch (Exception e) {
          Toolkit.getDefaultToolkit().beep();
          m_acc.controller.log("'" + port + "' not a number");
          m_portField.setText(Integer.toString(m_serverNode.getPort()));
        }
      }
    });

    m_connectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if (m_serverNode.isConnected()) {
          disconnect();
        } else {
          connect();
        }
      }
    });

    m_hostField.setText(m_serverNode.getHost());
    m_portField.setText(Integer.toString(m_serverNode.getPort()));

    m_shutdownButton.setAction(m_serverNode.getShutdownAction());

    setupConnectButton();

    m_environmentTextArea = (TextArea) findComponent("EnvironmentTextArea");
    m_configTextArea = (TextArea) findComponent("ConfigTextArea");
    
    m_threadDumpButton = (Button) findComponent("TakeThreadDumpButton");
    m_threadDumpButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        try {
          ConnectionContext cc = m_serverNode.getConnectionContext();
          DSOClient[] clients = ClientsHelper.getHelper().getClients(cc);
          ThreadDumpEntry tde = new ThreadDumpEntry();

          tde.add(m_serverNode.toString(), ServerHelper.getHelper().takeThreadDump(cc));
          for (DSOClient client : clients) {
            tde.add(client.getRemoteAddress(), client.getL1InfoMBean().takeThreadDump());
          }
          XTreeNode root = (XTreeNode) m_threadDumpTreeModel.getRoot();
          int index = root.getChildCount();
          root.add(tde);
          if(index == 0) m_threadDumpTreeModel.nodeStructureChanged(root);
          else m_threadDumpTreeModel.nodesWereInserted(root, new int[] {index});
          m_threadDumpTreeModel.nodeStructureChanged(root);
        } catch (Exception e) {
          m_acc.log(e);
        }
      }
    });

    m_threadDumpTree = (XTree) findComponent("ThreadDumpTree");
    m_threadDumpTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        if(m_lastSelectedThreadDumpTreeNode != null) {
          m_lastSelectedThreadDumpTreeNode.setViewPosition(m_threadDumpTextScroller.getViewport().getViewPosition());
        }
        ThreadDumpTreeNode tdtn = (ThreadDumpTreeNode)m_threadDumpTree.getLastSelectedPathComponent();
        if(tdtn != null) {
          m_threadDumpTextArea.setText(tdtn.getContent());
          final Point viewPosition = tdtn.getViewPosition();
          if(viewPosition != null) {
            SwingUtilities.invokeLater(new Runnable () {
              public void run() {
                m_threadDumpTextScroller.getViewport().setViewPosition(viewPosition);
              }
            });
          }
        }
        m_lastSelectedThreadDumpTreeNode = tdtn;
      }
    });

    m_threadDumpTree.setModel(m_threadDumpTreeModel = new XTreeModel());
    m_threadDumpTree.setShowsRootHandles(true);

    m_threadDumpTextArea = (TextArea) findComponent("ThreadDumpTextArea");
    m_threadDumpTextScroller = (ScrollPane) findComponent("ThreadDumpTextScroller");
    
    m_startGatheringStatsButton = (ToggleButton) findComponent("StartGatheringStatsButton");
    m_startGatheringStatsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_stopGatheringStatsButton.setSelected(false);
        if(m_statisticsManagerMBean == null) {
          m_statisticsManagerMBean = m_serverNode.getStatisticsManagerMBean();
          m_statisticsEmitterMBean = m_serverNode.registerStatisticsEmitterListener(m_statsEmitterListener);
        }
        m_currentStatsSessionId = m_statisticsManagerMBean.createCaptureSession();
        for (String stat : m_statisticsManagerMBean.getSupportedStatistics()) {
          m_statisticsManagerMBean.enableStatistic(m_currentStatsSessionId, stat);
        }
        m_statisticsEmitterMBean.enable();
        m_statisticsManagerMBean.startCapturing(m_currentStatsSessionId);
      }
    });
    m_stopGatheringStatsButton = (ToggleButton) findComponent("StopGatheringStatsButton");
    m_stopGatheringStatsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_startGatheringStatsButton.setSelected(false);
        synchronized (m_statsEmitterListener) {
          m_statisticsManagerMBean.stopCapturing(m_currentStatsSessionId);
          while (!m_statsEmitterListener.getShutdown()) {
            try {
              m_statsEmitterListener.wait(2000);
            } catch(InterruptedException ie) {/**/}
          }
        }
        m_statisticsEmitterMBean.disable();
        m_currentStatsSessionId = -1;
      }
    });
    m_currentStatsSessionId = -1;
    
    m_statisticsLog = (TextArea) findComponent("StatisticsLog");
    m_statsEmitterListener = new StatsEmitterListener();
  }

  class StatsEmitterListener implements NotificationListener {
    boolean shutdown = false;

    public boolean getShutdown() {
      return shutdown;
    }

    public void handleNotification(Notification notification, Object o) {
      Assert.assertTrue("Expecting notification data to be a collection", o instanceof Collection);

      StatisticData data = (StatisticData) notification.getUserData();
      m_statisticsLog.append(data.toString());
      m_statisticsLog.append("\n");
      int length = m_statisticsLog.getDocument().getLength()-1;
      m_statisticsLog.select(length, length);
      if (SRAShutdownTimestamp.ACTION_NAME.equals(data.getName())) {
        shutdown = true;
        synchronized (this) {
          this.notifyAll();
        }
      }
    }
  }
  
  void setupConnectButton() {
    String label;
    Icon icon;
    boolean enabled;

    if (m_serverNode.isConnected()) {
      label = "Disconnect";
      icon = m_disconnectIcon;
      enabled = true;
    } else {
      label = "Connect...";
      icon = m_connectIcon;
      enabled = !m_serverNode.isAutoConnect();
    }

    m_connectButton.setText(label);
    m_connectButton.setIcon(icon);
    m_connectButton.setEnabled(enabled);
  }

  JButton getConnectButton() {
    return m_connectButton;
  }

  private void connect() {
    m_serverNode.connect();
  }

  void activated() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);

    setupConnectButton();

    Date activateDate = new Date(m_serverNode.getActivateTime());
    String activateTime = activateDate.toString();

    setStatusLabel(m_acc.format("server.activated.label", new Object[]{activateTime}));
    m_acc.controller.addServerLog(m_serverNode.getConnectionContext());
    if (!isRuntimeInfoShowing()) {
      showRuntimeInfo();
    }

    testSetEnvironment();
    testSetConfig();

    m_acc.controller.setStatus(m_acc.format("server.activated.status", new Object[] {m_serverNode, activateTime}));
  }

  /**
   * The only differences between activated() and started() is the status message and the serverlog is only added in
   * activated() under the presumption that a non-active server won't be saying anything.
   */
  void started() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);

    Date startDate = new Date(m_serverNode.getStartTime());
    String startTime = startDate.toString();

    setupConnectButton();
    setStatusLabel(m_acc.format("server.started.label", new Object[] {startTime}));
    if (!isRuntimeInfoShowing()) {
      showRuntimeInfo();
    }

    testSetEnvironment();
    testSetConfig();
    
    m_acc.controller.setStatus(m_acc.format("server.started.status", new Object[] {m_serverNode, startTime}));
  }

  private void testSetEnvironment() {
    if(m_environmentTextArea.getDocument().getLength() > 0) return;
    m_environmentTextArea.setText(m_serverNode.getEnvironment());
  }

  private void testSetConfig() {
    if(m_configTextArea.getDocument().getLength() > 0) return;
    m_configTextArea.setText(m_serverNode.getConfig());
  }
  
  void passiveUninitialized() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);

    String startTime = new Date().toString();

    setupConnectButton();
    setStatusLabel(m_acc.format("server.initializing.label", new Object[] {startTime}));
    if (!isRuntimeInfoShowing()) {
      showRuntimeInfo();
    }

    testSetEnvironment();
    testSetConfig();

    m_acc.controller.setStatus(m_acc.format("server.initializing.status", new Object[] {m_serverNode, startTime}));
  }

  void passiveStandby() {
    m_hostField.setEditable(false);
    m_portField.setEditable(false);

    String startTime = new Date().toString();

    setupConnectButton();
    setStatusLabel(m_acc.format("server.standingby.label", new Object[] {startTime}));
    if (!isRuntimeInfoShowing()) {
      showRuntimeInfo();
    }

    testSetEnvironment();
    testSetConfig();

    m_acc.controller.setStatus(m_acc.format("server.standingby.status", new Object[] {m_serverNode, startTime}));
  }

  private void disconnect() {
    m_serverNode.disconnect();
  }

  void disconnected() {
    m_hostField.setEditable(true);
    m_portField.setEditable(true);

    String startTime = new Date().toString();

    setupConnectButton();
    setStatusLabel(m_acc.format("server.disconnected.label", new Object[] {startTime}));
    hideRuntimeInfo();

    m_acc.controller.removeServerLog(m_serverNode.getConnectionContext());
    m_acc.controller.setStatus(m_acc.format("server.disconnected.status", new Object[] {m_serverNode, startTime}));
  }

  void setStatusLabel(String msg) {
    m_statusView.setLabel(msg);
    m_statusView.setIndicator(m_serverNode.getServerStatusColor());
  }

  boolean isRuntimeInfoShowing() {
    return m_runtimeInfoPanel.isVisible() || (m_altProductInfoPanel != null && m_altProductInfoPanel.isVisible());
  }

  private void showRuntimeInfo() {
    L2Info[] clusterMembers = m_serverNode.getClusterMembers();

    m_clusterMemberTableModel.clear();

    if (clusterMembers.length > 1) {
      Container parent;

      if (m_altProductInfoPanel != null && (parent = (Container) m_altProductInfoPanel.getParent()) != null) {
        parent.replaceChild(m_altProductInfoPanel, m_runtimeInfoPanel);
      }

      m_productInfoPanel.init(m_serverNode.getProductInfo());
      m_runtimeInfoPanel.setVisible(true);

      for (int i = 0; i < clusterMembers.length; i++) {
        addClusterMember(clusterMembers[i]);
      }
      m_clusterMemberTableModel.fireTableDataChanged();
    } else {
      if (m_altProductInfoPanel == null) {
        m_altProductInfoPanel = new ProductInfoPanel();
      }

      Container parent;
      if ((parent = (Container) m_runtimeInfoPanel.getParent()) != null) {
        parent.replaceChild(m_runtimeInfoPanel, m_altProductInfoPanel);
      }

      m_altProductInfoPanel.init(m_serverNode.getProductInfo());
      m_altProductInfoPanel.setVisible(true);
    }

    revalidate();
    repaint();
  }

  private void hideRuntimeInfo() {
    m_clusterMemberTableModel.clear();
    m_runtimeInfoPanel.setVisible(false);
    if (m_altProductInfoPanel != null) {
      m_altProductInfoPanel.setVisible(false);
    }
    revalidate();
    repaint();
  }

  private class ClusterMemberStatusRenderer extends StatusRenderer {
    ClusterMemberStatusRenderer() {
      super();
    }

    public void setValue(JTable table, int row, int col) {
      ServerConnectionManager member = m_clusterMemberTableModel.getClusterMemberAt(row);

      m_label.setText(member.getName());
      m_indicator.setBackground(ServerNode.getServerStatusColor(member));
    }
  }

  private class ClusterMemberListener implements ConnectionListener {
    ServerConnectionManager m_scm;
    ConnectDialog           m_cd;

    void setServerConnectionManager(ServerConnectionManager scm) {
      m_scm = scm;
    }

    public void handleConnection() {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (m_clusterMemberTableModel != null) {
            int count = m_clusterMemberTableModel.getRowCount();
            m_clusterMemberTableModel.fireTableRowsUpdated(0, count - 1);
          }
        }
      });
    }

    class ConnectDialogListener implements ConnectionListener {
      public void handleConnection() {
        JMXConnector jmxc;
        if ((jmxc = m_cd.getConnector()) != null) {
          try {
            m_scm.setJMXConnector(jmxc);
            m_scm.setAutoConnect(true);
          } catch (IOException ioe) {/**/
          }
        }
      }

      public void handleException() {
        final Exception error = m_cd.getError();

        m_acc.log("Failed to connect to '" + m_scm + "' [" + error.getMessage() + "]");

        if (error instanceof SecurityException) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              AdminClientPanel topPanel = (AdminClientPanel) ServerPanel.this
                  .getAncestorOfClass(AdminClientPanel.class);
              Frame frame = (Frame) topPanel.getAncestorOfClass(java.awt.Frame.class);
              String msg = "Failed to connect to '" + m_scm + "'\n\n" + error.getMessage()
                           + "\n\nTry again to connect?";

              int result = JOptionPane.showConfirmDialog(frame, msg, frame.getTitle(), JOptionPane.YES_NO_OPTION);
              if (result == JOptionPane.OK_OPTION) {
                m_cd.center(frame);
                m_cd.setVisible(true);
              }
            }
          });
        }
      }
    }

    void connect() {
      AdminClientPanel topPanel = (AdminClientPanel) ServerPanel.this.getAncestorOfClass(AdminClientPanel.class);
      Frame frame = (Frame) topPanel.getAncestorOfClass(java.awt.Frame.class);

      m_cd = m_serverNode.getConnectDialog(new ConnectDialogListener());
      m_cd.setServerConnectionManager(m_scm);
      m_cd.center(frame);
      m_cd.setVisible(true);
    }

    public void handleException() {
      if (m_cd != null && m_cd.isVisible()) return;

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          Exception e = m_scm.getConnectionException();
          if (e instanceof SecurityException) {
            connect();
          }

          if (m_clusterMemberTableModel != null) {
            int count = m_clusterMemberTableModel.getRowCount();
            m_clusterMemberTableModel.fireTableRowsUpdated(0, count - 1);
          }
        }
      });
    }
  }

  void addClusterMember(L2Info clusterMember) {
    ClusterMemberListener cml = new ClusterMemberListener();
    ServerConnectionManager scm = new ServerConnectionManager(clusterMember, false, cml);
    String[] creds = ServerConnectionManager.getCachedCredentials(scm);

    if (creds == null) {
      creds = m_serverNode.getServerConnectionManager().getCredentials();
    }
    if (creds != null) {
      scm.setCredentials(creds[0], creds[1]);
    }
    cml.setServerConnectionManager(scm);
    scm.setAutoConnect(true);

    m_clusterMemberTableModel.addClusterMember(scm);
  }

  ServerConnectionManager[] getClusterMembers() {
    int count = m_clusterMemberTableModel.getRowCount();
    ServerConnectionManager[] result = new ServerConnectionManager[count];

    for (int i = 0; i < count; i++) {
      result[i] = m_clusterMemberTableModel.getClusterMemberAt(i);
    }

    return result;
  }

  public void tearDown() {
    super.tearDown();

    m_statusView.tearDown();
    m_productInfoPanel.tearDown();
    m_clusterMemberTableModel.tearDown();

    m_acc = null;
    m_serverNode = null;
    m_hostField = null;
    m_portField = null;
    m_connectButton = null;
    m_runtimeInfoPanel = null;
    m_statusView = null;
    m_productInfoPanel = null;
    m_clusterMemberTable = null;
    m_clusterMemberTableModel = null;
  }
}

abstract class ThreadDumpTreeNode extends XTreeNode {
  private Point m_viewPosition;
  
  ThreadDumpTreeNode(Object userObject) {
    super(userObject);
  }
  
  void setViewPosition(Point viewPosition) {
    m_viewPosition = viewPosition;
  }
  
  Point getViewPosition() {
    return m_viewPosition;
  }
  
  abstract String getContent();
}

class ThreadDumpEntry extends ThreadDumpTreeNode {
  private String m_content;
  
  ThreadDumpEntry() {
    super(new Date());
  }

  void add(String clientAddr, String threadDump) {
    add(new ThreadDumpElement(clientAddr, threadDump));
  }

  Date getTime() {
    return (Date) getUserObject();
  }
  
  String getContent() {
    if(m_content != null) return m_content;
    
    StringBuffer sb = new StringBuffer();
    String nl = System.getProperty("line.separator");
    for(int i = 0; i < getChildCount(); i++) {
      ThreadDumpElement tde = (ThreadDumpElement)getChildAt(i);
      sb.append("---------- ");
      sb.append(tde.getSource());
      sb.append(" ----------");
      sb.append(nl);
      sb.append(nl);
      
      sb.append(tde.getContent());
    }
    return m_content = sb.toString();
  }
}

class ThreadDumpElement extends ThreadDumpTreeNode {
  private String m_threadDump;

  ThreadDumpElement(String clientAddr, String threadDump) {
    super(clientAddr);
    m_threadDump = threadDump;
  }

  String getThreadDump() {
    return m_threadDump;
  }
  
  String getContent() {
    return getThreadDump();
  }
  
  String getSource() {
    return toString();
  }
}

