/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigEvent;
import org.terracotta.ui.util.SWTComponentModel;
import org.terracotta.ui.util.SWTLayout;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

import java.util.HashMap;
import java.util.Map;

public final class ServersPanel extends ConfigurationEditorPanel implements SWTComponentModel {

  private static final int NAME_INDEX     = 0;
  private static final int HOST_INDEX     = 1;
  private static final int DSO_PORT_INDEX = 2;
  private static final int JMX_PORT_INDEX = 3;

  private final Layout     m_layout;
  private State            m_state;
  private volatile boolean m_isActive;

  public ServersPanel(Composite parent, int style) {
    super(parent, style);
    this.m_layout = new Layout(this);
    SWTUtil.setBackgroundRecursive(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
    createListeners();
  }

  public synchronized void addListener(UpdateEventListener listener, int type) {
  // not implemented
  }

  public synchronized void removeListener(UpdateEventListener listener, int type) {
  // not implemented
  }

  private void createContextListeners() {
    createServersElementListener(XmlConfigEvent.SERVER_NAME, NAME_INDEX, m_layout.m_nameField);
    createServersElementListener(XmlConfigEvent.SERVER_HOST, HOST_INDEX, m_layout.m_hostField);
    createServersElementListener(XmlConfigEvent.SERVER_DSO_PORT, DSO_PORT_INDEX, m_layout.m_dsoPortField);
    createServersElementListener(XmlConfigEvent.SERVER_JMX_PORT, JMX_PORT_INDEX, m_layout.m_jmxPortField);
    
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        Server server = (Server) castEvent(e).element;
        TableItem item = createServerItem(server);
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(XmlConfigContext.DEFAULT_NAME, null, server,
            XmlConfigEvent.ELEM_NAME, XmlConfigEvent.SERVER_NAME));
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(XmlConfigContext.DEFAULT_HOST, null, server,
            XmlConfigEvent.ELEM_HOST, XmlConfigEvent.SERVER_HOST));
        m_layout.m_serverTable.setSelection(item);
        updateServersElementListeners(server);
        m_layout.m_removeServerButton.setEnabled(true);
      }
    }, XmlConfigEvent.NEW_SERVER, this);
  }

  private void createListeners() {
    m_layout.m_addServerButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(XmlConfigEvent.CREATE_SERVER));
      }
    });

    m_layout.m_serverTable.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        System.out.println("row selected event");// XXX
        m_layout.m_removeServerButton.setEnabled(true);
        TableItem item = m_layout.m_serverTable.getItem(m_layout.m_serverTable.getSelectionIndex());
        Server server = (Server) item.getData();
        updateServersElementListeners(server);
      }
    });

    // XXX: remember serverIndices - create REMOVE_X event
    m_layout.m_removeServerButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        int row = m_layout.m_serverTable.getSelectionIndex();
        // m_state.servers.getServerArray(row);
        // m_state.servers.removeServer(row);
        m_layout.m_serverTable.remove(row);
        m_layout.m_serverTable.deselectAll();
        if (m_layout.m_serverTable.getItemCount() == 0) m_layout.m_removeServerButton.setEnabled(false);
      }
    });
  }

  private void createServersElementListener(int eventType, final int column, final Text field) {
    UpdateEventListener listener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        int index = m_state.serverIndices.get(event.element).intValue();
        TableItem item = m_layout.m_serverTable.getItem(index);
        if (event.data == null) event.data = "";
        item.setText(column, (String) event.data);
        if (m_layout.m_serverTable.getSelectionIndex() == index) {
          field.setText((String) event.data);
        }
      }
    };
    m_state.xmlContext.addListener(listener, eventType, this);
  }

  private void updateServersElementListeners(Server server) {
    updateListeners(XmlConfigEvent.SERVER_NAME, XmlConfigEvent.ELEM_NAME, server);
    updateListeners(XmlConfigEvent.SERVER_HOST, XmlConfigEvent.ELEM_HOST, server);
    updateListeners(XmlConfigEvent.SERVER_DSO_PORT, XmlConfigEvent.ELEM_DSO_PORT, server);
    updateListeners(XmlConfigEvent.SERVER_JMX_PORT, XmlConfigEvent.ELEM_JMX_PORT, server);
  }

  private void updateListeners(int event, String elementName, XmlObject element) {
    m_state.xmlContext.updateListeners(new XmlConfigEvent(element, elementName, event));
  }

  private XmlConfigEvent castEvent(UpdateEvent e) {
    return (XmlConfigEvent) e;
  }

  private TableItem createServerItem(XmlObject server) {
    TableItem item = new TableItem(m_layout.m_serverTable, SWT.NONE);
    item.setData(server);
    m_state.serverIndices.put((Server) server, new Integer(m_layout.m_serverTable.getItemCount() - 1));
    return item;
  }

  private void initTableItems(Servers servers) {
    m_layout.m_serverTable.setEnabled(false);
    Server[] serverElements = servers.getServerArray();
    for (int i = 0; i < serverElements.length; i++) {
      createServerItem(serverElements[i]);
      updateServersElementListeners(serverElements[i]);
    }
    m_layout.m_serverTable.setEnabled(true);
  }

  public synchronized boolean isActive() {
    return m_isActive;
  }

  public synchronized void init(Object data) {
    if (m_isActive && m_state.project == (IProject) data) return;
    setActive(false);
    m_state = new State((IProject) data);
    createContextListeners();
    Servers servers = TcPlugin.getDefault().getConfiguration(m_state.project).getServers();
    setActive(true);
    initTableItems(servers);
  }

  public synchronized void setActive(boolean active) {
    m_isActive = active;
  }

  public synchronized void clearState() {
    setActive(false);
    m_layout.reset();
    m_state.xmlContext.detachComponentModel(this);
    m_state = null;
  }

  // --------------------------------------------------------------------------------

  private class State {
    final IProject             project;
    final XmlConfigContext     xmlContext;
    final Map<Server, Integer> serverIndices;

    private State(IProject project) {
      this.project = project;
      this.xmlContext = XmlConfigContext.getInstance(project);
      this.serverIndices = new HashMap<Server, Integer>();
    }
  }

  // --------------------------------------------------------------------------------

  private static class Layout implements SWTLayout {

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
    private Text                m_nameField;
    private Text                m_hostField;
    private Text                m_dsoPortField;
    private Text                m_jmxPortField;

    public void reset() {
      m_serverTable.removeAll();
      m_nameField.setText("");
      m_hostField.setText("");
      m_dsoPortField.setText("");
      m_jmxPortField.setText("");
    }

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
      m_nameField = new Text(serverGroup, SWT.BORDER);
      m_nameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      new Label(serverGroup, SWT.NONE); // space

      Label dsoPortLabel = new Label(serverGroup, SWT.NONE);
      dsoPortLabel.setText(DSO_PORT);
      m_dsoPortField = new Text(serverGroup, SWT.BORDER);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.widthHint = SWTUtil.textColumnsToPixels(m_dsoPortField, 6);
      m_dsoPortField.setLayoutData(gridData);

      Label hostLabel = new Label(serverGroup, SWT.NONE);
      hostLabel.setText(HOST);
      m_hostField = new Text(serverGroup, SWT.BORDER);
      m_hostField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      new Label(serverGroup, SWT.NONE); // space

      Label jmxPortLabel = new Label(serverGroup, SWT.NONE);
      jmxPortLabel.setText(JMX_PORT);
      m_jmxPortField = new Text(serverGroup, SWT.BORDER);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.widthHint = SWTUtil.textColumnsToPixels(m_jmxPortField, 6);
      m_jmxPortField.setLayoutData(gridData);

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
