/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.TcPlugin;
import org.terracotta.ui.util.SWTComponentModel;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public final class ServersPanel extends ConfigurationEditorPanel implements ConfigurationEditorRoot, SWTComponentModel {

  private static final String DEFAULT_NAME     = "dev";
  private static final String DEFAULT_HOST     = "localhost";
  private static final int    DEFAULT_DSO_PORT = 9510;
  private static final int    DEFAULT_JMX_PORT = 9520;
  private static final int    NAME_INDEX       = 0;
  private static final int    HOST_INDEX       = 1;
  private static final int    DSO_PORT_INDEX   = 2;
  private static final int    JMX_PORT_INDEX   = 3;

  private final Layout        m_layout;
  private final Object        m_stateLock      = new Object();
  private State               m_state;
  private boolean             m_isActive;
  private SelectionListener   m_addButtonListener;
  private SelectionListener   m_removeButtonListener;
  private SelectionListener   m_tableItemSelectListener;

  public ServersPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    createListeners();
  }

  public synchronized void addListener(UpdateEventListener listener, int type) {
  // not implemented
  }

  public synchronized void removeListener(UpdateEventListener listener, int type) {
  // not implemented
  }

  private void createListeners() {
    m_addButtonListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        synchronized (m_stateLock) {
          if (m_state.servers == null) {
            m_state.servers = m_state.config.addNewServers();
          }
          Server server = m_state.servers.addNewServer();
          server.setName(DEFAULT_NAME);
          server.setHost(DEFAULT_HOST);
          server.setDsoPort(DEFAULT_DSO_PORT);
          server.setJmxPort(DEFAULT_JMX_PORT);
          m_layout.m_serverTable.setSelection(createServerItem(server));
          m_layout.m_removeServerButton.setEnabled(true);
        }
      }
    };
    m_tableItemSelectListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        m_layout.m_removeServerButton.setEnabled(true);
        // TODO:
      }
    };
    m_removeButtonListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        int row = m_layout.m_serverTable.getSelectionIndex();
        synchronized (m_stateLock) {
          m_state.servers.getServerArray(row);
          m_state.servers.removeServer(row);
        }
        m_layout.m_serverTable.remove(row);
        m_layout.m_serverTable.deselectAll();
        if (m_layout.m_serverTable.getItemCount() == 0) m_layout.m_removeServerButton.setEnabled(false);
      }
    };
  }

  private void initTableItems() {
    m_layout.m_serverTable.removeAll();
    synchronized (m_stateLock) {
      Server[] servers = m_state.servers.getServerArray();
      for (int i = 0; i < servers.length; i++) {
        createServerItem(servers[i]);
      }
    }
  }

  private TableItem createServerItem(Server server) {
    TableItem item = new TableItem(m_layout.m_serverTable, SWT.NONE);
    item.setText(NAME_INDEX, (server.getName() == null) ? "" : server.getName());
    item.setText(HOST_INDEX, (server.getHost() == null) ? "" : server.getHost());
    item.setText(DSO_PORT_INDEX, "" + server.getDsoPort());
    item.setText(JMX_PORT_INDEX, "" + server.getJmxPort());
    return item;
  }

  public synchronized boolean isActive() {
    return m_isActive;
  }

  public synchronized void init(Object data) {
    synchronized (m_stateLock) {
      if (isActive() && m_state.project == (IProject) data) return;
      TcPlugin plugin = TcPlugin.getDefault();
      disable();
      m_state = new State();
      m_state.project = (IProject) data;
      m_state.config = plugin.getConfiguration(m_state.project);
      m_state.servers = m_state.config != null ? m_state.config.getServers() : null;
    }
    initTableItems();
    activate();
  }

  public synchronized void disable() {
    if (!isActive()) return;
    m_layout.m_addServerButton.removeSelectionListener(m_addButtonListener);
    m_layout.m_removeServerButton.removeSelectionListener(m_removeButtonListener);
    m_layout.m_serverTable.removeSelectionListener(m_tableItemSelectListener);
    m_layout.m_serverTable.deselectAll();
    m_isActive = false;
  }

  public synchronized void activate() {
    if (isActive()) return;
    m_layout.m_addServerButton.addSelectionListener(m_addButtonListener);
    m_layout.m_removeServerButton.addSelectionListener(m_removeButtonListener);
    m_layout.m_serverTable.addSelectionListener(m_tableItemSelectListener);
    m_isActive = true;
  }

  public IProject getProject() {
    synchronized (m_stateLock) {
      return m_state.project;
    }
  }

  public synchronized void clearState() {
    synchronized (m_stateLock) {
      m_state = null;
    }
    disable();
  }

  // --------------------------------------------------------------------------------

  private class State {
    private IProject project;
    private TcConfig config;
    private Servers  servers;
  }

  // --------------------------------------------------------------------------------

  private static class Layout {

    private static final int    WIDTH_HINT         = 500;
    private static final int    HEIGHT_HINT        = 120;
    private static final String BROWSE             = "Browse...";
    private static final String ADD                = "Add...";
    private static final String REMOVE             = "Remove";
    private static final String SERVERS            = "Servers";
    private static final String NAME               = "Name";
    private static final String HOST               = "Host";
    private static final String DSO_PORT           = "DSO Port";
    private static final String JMX_PORT           = "JMX Port";
    private static final String SERVER             = "Server";
    private static final String DATA               = "Data";
    private static final String LOGS               = "Logs";
    private static final String PERSISTENCE_MODE   = "Persistence Mode";
    private static final String GARBAGE_COLLECTION = "Garbage Collection";
    private static final String VERBOSE            = "Verbose";
    private static final String GC_INTERVAL        = "GC Interval";

    private Table               m_serverTable;
    private Button              m_addServerButton;
    private Button              m_removeServerButton;

    private Layout(Composite parent) {
      Composite panel = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      panel.setLayout(gridLayout);

      Composite comp = new Composite(panel, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;

      GridData gridData = new GridData();
      gridData.widthHint = WIDTH_HINT;
      gridData.horizontalAlignment = GridData.CENTER;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      gridData.verticalAlignment = GridData.BEGINNING;
      comp.setLayout(gridLayout);
      comp.setLayoutData(gridData);

      createServersGroup(comp);
      new Label(comp, SWT.NONE); // filler
      createServerGroup(comp);
    }

    private void createServersGroup(Composite parent) {
      Group serversGroup = new Group(parent, SWT.BORDER);
      serversGroup.setText(SERVERS);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      serversGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL);
      gridData.heightHint = HEIGHT_HINT;
      serversGroup.setLayoutData(gridData);

      Composite comp = new Composite(serversGroup, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 3;
      comp.setLayoutData(gridData);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      m_serverTable = new Table(tablePanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_serverTable.setHeaderVisible(true);
      m_serverTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(tablePanel, m_serverTable);

      TableColumn nameCol = new TableColumn(m_serverTable, SWT.NONE, NAME_INDEX);
      nameCol.setResizable(true);
      nameCol.setText(NAME);
      nameCol.pack();

      TableColumn hostCol = new TableColumn(m_serverTable, SWT.NONE, HOST_INDEX);
      hostCol.setResizable(true);
      hostCol.setText(HOST);
      hostCol.pack();

      TableColumn dsoPortCol = new TableColumn(m_serverTable, SWT.NONE, DSO_PORT_INDEX);
      dsoPortCol.setResizable(true);
      dsoPortCol.setText(DSO_PORT);
      dsoPortCol.pack();

      TableColumn jmxPortCol = new TableColumn(m_serverTable, SWT.NONE, JMX_PORT_INDEX);
      jmxPortCol.setResizable(true);
      jmxPortCol.setText(JMX_PORT);
      jmxPortCol.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_addServerButton = new Button(buttonPanel, SWT.PUSH);
      m_addServerButton.setText(ADD);
      m_addServerButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addServerButton);

      m_removeServerButton = new Button(buttonPanel, SWT.PUSH);
      m_removeServerButton.setText(REMOVE);
      m_removeServerButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeServerButton);
    }

    private void createServerGroup(Composite parent) {
      Group serverGroup = new Group(parent, SWT.BORDER);
      serverGroup.setText(SERVER);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 5;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      serverGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
      serverGroup.setLayoutData(gridData);

      Label nameLabel = new Label(serverGroup, SWT.NONE);
      nameLabel.setText(NAME);
      Text nameField = new Text(serverGroup, SWT.BORDER);
      nameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      new Label(serverGroup, SWT.NONE); // space

      Label dsoPortLabel = new Label(serverGroup, SWT.NONE);
      dsoPortLabel.setText(DSO_PORT);
      Text dsoPortField = new Text(serverGroup, SWT.BORDER);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.widthHint = SWTUtil.textColumnsToPixels(dsoPortField, 6);
      dsoPortField.setLayoutData(gridData);

      Label hostLabel = new Label(serverGroup, SWT.NONE);
      hostLabel.setText(HOST);
      Text hostField = new Text(serverGroup, SWT.BORDER);
      hostField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      new Label(serverGroup, SWT.NONE); // space

      Label jmxPortLabel = new Label(serverGroup, SWT.NONE);
      jmxPortLabel.setText(JMX_PORT);
      Text jmxPortField = new Text(serverGroup, SWT.BORDER);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.widthHint = SWTUtil.textColumnsToPixels(jmxPortField, 6);
      jmxPortField.setLayoutData(gridData);

      Label dataLabel = new Label(serverGroup, SWT.NONE);
      dataLabel.setText(DATA);

      Composite dataPanel = new Composite(serverGroup, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      dataPanel.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 4;
      dataPanel.setLayoutData(gridData);

      Text dataLocation = new Text(dataPanel, SWT.BORDER);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      dataLocation.setLayoutData(gridData);

      Button dataBrowse = new Button(dataPanel, SWT.PUSH);
      dataBrowse.setText(BROWSE);
      SWTUtil.applyDefaultButtonSize(dataBrowse);

      Label logsLabel = new Label(serverGroup, SWT.NONE);
      logsLabel.setText(LOGS);

      Composite logsPanel = new Composite(serverGroup, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      logsPanel.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 4;
      logsPanel.setLayoutData(gridData);

      Text logsLocation = new Text(logsPanel, SWT.BORDER);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      logsLocation.setLayoutData(gridData);

      Button logsBrowse = new Button(logsPanel, SWT.PUSH);
      logsBrowse.setText(BROWSE);
      SWTUtil.applyDefaultButtonSize(logsBrowse);

      Composite gcPanel = new Composite(serverGroup, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 3;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      gcPanel.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 5;
      gcPanel.setLayoutData(gridData);

      Label persistenceModeLabel = new Label(gcPanel, SWT.NONE);
      persistenceModeLabel.setText(PERSISTENCE_MODE);

      Combo persistenceModeCombo = new Combo(gcPanel, SWT.BORDER);
      gridData = new GridData(GridData.FILL_VERTICAL | GridData.GRAB_HORIZONTAL);
      gridData.widthHint = 200;
      persistenceModeCombo.setLayoutData(gridData);

      Button gcCheck = new Button(gcPanel, SWT.CHECK);
      gcCheck.setText(GARBAGE_COLLECTION);

      Label gcIntervalLabel = new Label(gcPanel, SWT.NONE);
      gcIntervalLabel.setText(GC_INTERVAL);

      Spinner gcIntervalSpinner = new Spinner(gcPanel, SWT.BORDER);
      gridData = new GridData(GridData.FILL_VERTICAL | GridData.GRAB_HORIZONTAL);
      gridData.widthHint = 100;
      gcIntervalSpinner.setLayoutData(gridData);

      Button verboseCheck = new Button(gcPanel, SWT.CHECK);
      verboseCheck.setText(VERBOSE);
    }
  }
}

// private static final String[] FIELDS = new String[] { "Name", "Host", "DsoPort", "JmxPort" };
// private static final String[] HEADERS = new String[] { "Name", "Host", "DSO port", "JMX port" };
//
// class ServerTableModel extends XObjectTableModel {
// public ServerTableModel() {
// super(Server.class, FIELDS, HEADERS);
// }
//
// public void setServers(Servers servers) {
// ServerTableModel.this.clear();
// set(servers.getServerArray());
// }
// }
//
// public void tableChanged(TableModelEvent e) {
// if (m_serverTableModel.getRowCount() == 0) {
// m_config.unsetServers();
// m_servers = null;
// m_serverPanel.setVisible(false);
// } else {
// m_serverPanel.setVisible(true);
// }
//
// setDirty();
// }
//
// public void valueChanged(ListSelectionEvent e) {
// if (!e.getValueIsAdjusting()) {
// int row = m_serverTable.getSelectedRow();
// boolean haveSel = row != -1;
//  
// if (haveSel) {
// m_serverPanel.tearDown();
// m_serverPanel.setup((Server) m_serverTableModel.getObjectAt(row));
// }
// m_removeServerAction.setEnabled(haveSel);
// }
// }

// private void addListeners() {
// m_serverTableModel.addTableModelListener(this);
// m_serverTable.getSelectionModel().addListSelectionListener(this);
//
// if (m_serverTableModel.getRowCount() > 0) {
// m_serverTable.setRowSelectionInterval(0, 0);
// }
// }
//
// private void removeListeners() {
// m_serverTableModel.removeTableModelListener(this);
// m_serverTable.getSelectionModel().removeListSelectionListener(this);
// }
