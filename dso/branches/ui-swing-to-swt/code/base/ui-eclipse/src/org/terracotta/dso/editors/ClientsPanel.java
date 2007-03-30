/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigEvent;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTComponentModel;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Client;

public class ClientsPanel extends ConfigurationEditorPanel implements SWTComponentModel {

  private final Layout     m_layout;
  private State            m_state;
  private volatile boolean m_isActive;

  public ClientsPanel(Composite parent, int style) {
    super(parent, style);
    this.m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  // ================================================================================
  // INTERFACE
  // ================================================================================

  public synchronized void addListener(UpdateEventListener listener, int type) {
  // not implemented
  }

  public synchronized void removeListener(UpdateEventListener listener, int type) {
  // not implemented
  }

  public synchronized void clearState() {
    setActive(false);
    m_layout.reset();
    m_state.xmlContext.detachComponentModel(this);
    m_state = null;
  }

  public synchronized void init(Object data) {
    if (m_isActive && m_state.project == (IProject) data) return;
    setActive(false);
    m_state = new State((IProject) data);
    createContextListeners();
    Client client = TcPlugin.getDefault().getConfiguration(m_state.project).getClients();
    setActive(true);
    updateClientListeners(client);
    // initModuleRepositories(client);
    // initModules(client);
  }

  public synchronized boolean isActive() {
    return m_isActive;
  }

  public synchronized void setActive(boolean activate) {
    m_isActive = activate;
  }

  // ================================================================================
  // INIT LISTENERS
  // ================================================================================

  private void createContextListeners() {
    registerFieldBehavior(XmlConfigEvent.CLIENT_LOGS, m_layout.m_logsLocation);
  }

  // - field listeners
  private void registerFieldBehavior(int type, final Text text) {
    // registerFieldNotificationListener(type, text);
    UpdateEventListener textListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        if (event.data == null) event.data = "";
        text.setEnabled(true);
        text.setText((String) event.data);
      }
    };
    text.setData(textListener);
    m_state.xmlContext.addListener(textListener, type, this);
  }

  // - handle field notifications
  // private void registerFieldNotificationListener(final int type, final Text text) {
  // text.addFocusListener(new FocusAdapter() {
  // public void focusLost(FocusEvent e) {
  // handleFieldEvent(text, type);
  // }
  // });
  // text.addKeyListener(new KeyAdapter() {
  // public void keyPressed(KeyEvent e) {
  // if (e.keyCode == SWT.Selection) {
  // handleFieldEvent(text, type);
  // }
  // }
  // });
  // }

  // ================================================================================
  // HELPERS
  // ================================================================================

  private void updateClientListeners(Client client) {
    updateListeners(XmlConfigEvent.CLIENT_LOGS, client);
    updateListeners(XmlConfigEvent.CLIENT_DSO_REFLECTION_ENABLED, client);
    updateListeners(XmlConfigEvent.CLIENT_CLASS, client);
    updateListeners(XmlConfigEvent.CLIENT_HIERARCHY, client);
    updateListeners(XmlConfigEvent.CLIENT_LOCKS, client);
    updateListeners(XmlConfigEvent.CLIENT_TRANSIENT_ROOT, client);
    updateListeners(XmlConfigEvent.CLIENT_DISTRIBUTED_METHODS, client);
    updateListeners(XmlConfigEvent.CLIENT_ROOTS, client);
    updateListeners(XmlConfigEvent.CLIENT_LOCK_DEBUG, client);
    updateListeners(XmlConfigEvent.CLIENT_DISTRIBUTED_METHOD_DEBUG, client);
    updateListeners(XmlConfigEvent.CLIENT_FIELD_CHANGE_DEBUG, client);
    updateListeners(XmlConfigEvent.CLIENT_NON_PORTABLE_WARNING, client);
    updateListeners(XmlConfigEvent.CLIENT_PARTIAL_INSTRUMENTATION, client);
    updateListeners(XmlConfigEvent.CLIENT_WAIT_NOTIFY_DEBUG, client);
    updateListeners(XmlConfigEvent.CLIENT_NEW_OBJECT_DEBUG, client);
    updateListeners(XmlConfigEvent.CLIENT_AUTOLOCK_DETAILS, client);
    updateListeners(XmlConfigEvent.CLIENT_CALLER, client);
    updateListeners(XmlConfigEvent.CLIENT_FULL_STACK, client);
    updateListeners(XmlConfigEvent.CLIENT_FIND_NEEDED_INCLUDES, client);
    updateListeners(XmlConfigEvent.CLIENT_FAULT_COUNT, client);
  }

  private void updateListeners(int event, XmlObject element) {
    m_state.xmlContext.updateListeners(new XmlConfigEvent(element, event));
  }

  private XmlConfigEvent castEvent(UpdateEvent e) {
    return (XmlConfigEvent) e;
  }

  // ================================================================================
  // STATE
  // ================================================================================

  private class State {
    final IProject             project;
    final XmlConfigContext     xmlContext;
    final XmlConfigUndoContext xmlUndoContext;

    private State(IProject project) {
      this.project = project;
      this.xmlContext = XmlConfigContext.getInstance(project);
      this.xmlUndoContext = XmlConfigUndoContext.getInstance(project);
    }
  }

  // ================================================================================
  // LAYOUT
  // ================================================================================

  private static class Layout {

    private static final int    WIDTH_HINT               = 500;
    private static final int    HEIGHT_HINT              = 600;
    private static final String DSO_REFLECTION_ENABLED   = "DSO Reflection Enabled";
    private static final String LOGS_LOCATION            = "Logs Location";
    private static final String BROWSE                   = "Browse...";
    private static final String DSO_CLIENT_DATA          = "Dso Client Data";
    private static final String INSTRUMENTATION_LOGGING  = "Instrumentation Logging";
    private static final String RUNTIME_LOGGING          = "Runtime Logging";
    private static final String RUNTIME_OUTPUT_OPTIONS   = "Runtime Output Options";
    private static final String CLASS                    = "Class";
    private static final String HIERARCHY                = "Hierarchy";
    private static final String LOCKS                    = "Locks";
    private static final String TRANSIENT_ROOT           = "Transient Root";
    private static final String DISTRIBUTED_METHODS      = "Distributed Methods";
    private static final String ROOTS                    = "Roots";
    private static final String LOCK_DEBUG               = "Lock Debug";
    private static final String DISTRIBUTED_METHOD_DEBUG = "Distributed Method Debug";
    private static final String FIELD_CHANGE_DEBUG       = "Field Change Debug";
    private static final String NON_PORTABLE_WARNING     = "Non-portable Warning";
    private static final String PARTIAL_INSTRUMENTATION  = "Partial Instrumentation";
    private static final String WAIT_NOTIFY_DEBUG        = "Wait Notify Debug";
    private static final String NEW_OBJECT_DEBUG         = "New Object Debug";
    private static final String AUTOLOCK_DETAILS         = "Autolock Details";
    private static final String CALLER                   = "Caller";
    private static final String FULL_STACK               = "Full Stack";
    private static final String FIND_NEEDED_INCLUDES     = "Find Needed Includes";
    private static final String MODULE_REPOSITORIES      = "Module Repositories";
    private static final String MODULES                  = "Modules";
    private static final String LOCATION                 = "Location";
    private static final String NAME                     = "Name";
    private static final String VERSION                  = "Version";
    private static final String ADD                      = "Add...";
    private static final String REMOVE                   = "Remove";
    private static final String FAULT_COUNT              = "Fault Count";

    private Button              m_dsoReflectionEnabledCheck;
    private Button              m_logsBrowse;
    private Text                m_logsLocation;
    private Button              m_classCheck;
    private Button              m_hierarchyCheck;
    private Button              m_locksCheck;
    private Button              m_transientRootCheck;
    private Button              m_distributedMethodsCheck;
    private Button              m_rootsCheck;
    private Button              m_lockDebugCheck;
    private Button              m_distributedMethodDebugCheck;
    private Button              m_fieldChangeDebugCheck;
    private Button              m_nonPortableWarningCheck;
    private Button              m_partialInstrumentationCheck;
    private Button              m_waitNotifyDebugCheck;
    private Button              m_newObjectDebugCheck;
    private Button              m_autoLockDetailsCheck;
    private Button              m_callerCheck;
    private Button              m_fullStackCheck;
    private Button              m_findNeededIncludesCheck;
    private Spinner             m_faultCountSpinner;
    private Table               m_moduleRepoTable;
    private Button              m_addModuleRepo;
    private Button              m_removeModuleRepo;
    private Table               m_moduleTable;
    private Button              m_addModule;
    private Button              m_removeModule;

    public void reset() {
      m_dsoReflectionEnabledCheck.setSelection(false);
      m_dsoReflectionEnabledCheck.setEnabled(false);
      m_logsBrowse.setEnabled(false);
      m_logsLocation.setText("");
      m_logsLocation.setEnabled(false);
      m_classCheck.setSelection(false);
      m_classCheck.setEnabled(false);
      m_hierarchyCheck.setSelection(false);
      m_hierarchyCheck.setEnabled(false);
      m_locksCheck.setSelection(false);
      m_locksCheck.setEnabled(false);
      m_transientRootCheck.setSelection(false);
      m_transientRootCheck.setEnabled(false);
      m_distributedMethodsCheck.setSelection(false);
      m_distributedMethodsCheck.setEnabled(false);
      m_rootsCheck.setSelection(false);
      m_rootsCheck.setEnabled(false);
      m_lockDebugCheck.setSelection(false);
      m_lockDebugCheck.setEnabled(false);
      m_distributedMethodDebugCheck.setSelection(false);
      m_distributedMethodDebugCheck.setEnabled(false);
      m_fieldChangeDebugCheck.setSelection(false);
      m_fieldChangeDebugCheck.setEnabled(false);
      m_nonPortableWarningCheck.setSelection(false);
      m_nonPortableWarningCheck.setEnabled(false);
      m_partialInstrumentationCheck.setSelection(false);
      m_partialInstrumentationCheck.setEnabled(false);
      m_waitNotifyDebugCheck.setSelection(false);
      m_waitNotifyDebugCheck.setEnabled(false);
      m_newObjectDebugCheck.setSelection(false);
      m_newObjectDebugCheck.setEnabled(false);
      m_autoLockDetailsCheck.setSelection(false);
      m_autoLockDetailsCheck.setEnabled(false);
      m_callerCheck.setSelection(false);
      m_callerCheck.setEnabled(false);
      m_fullStackCheck.setSelection(false);
      m_fullStackCheck.setEnabled(false);
      m_findNeededIncludesCheck.setSelection(false);
      m_findNeededIncludesCheck.setEnabled(false);
      m_moduleRepoTable.removeAll();
      m_addModuleRepo.setEnabled(false);
      m_removeModuleRepo.setEnabled(false);
      m_moduleTable.removeAll();
      m_addModule.setEnabled(false);
      m_removeModule.setEnabled(false);
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
      gridLayout.numColumns = 4;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;

      GridData gridData = new GridData();
      gridData.widthHint = WIDTH_HINT;
      gridData.heightHint = HEIGHT_HINT;
      gridData.horizontalAlignment = GridData.CENTER;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      gridData.verticalAlignment = GridData.BEGINNING;
      comp.setLayout(gridLayout);
      comp.setLayoutData(gridData);

      Label logsLabel = new Label(comp, SWT.NONE);
      logsLabel.setText(LOGS_LOCATION);

      m_logsLocation = new Text(comp, SWT.BORDER);
      m_logsLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      m_logsBrowse = new Button(comp, SWT.PUSH);
      m_logsBrowse.setText(BROWSE);
      SWTUtil.applyDefaultButtonSize(m_logsBrowse);

      m_dsoReflectionEnabledCheck = new Button(comp, SWT.CHECK);
      m_dsoReflectionEnabledCheck.setText(DSO_REFLECTION_ENABLED);

      Group dsoClientDataGroup = new Group(comp, SWT.BORDER);
      dsoClientDataGroup.setText(DSO_CLIENT_DATA);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 3;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      dsoClientDataGroup.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 4;
      dsoClientDataGroup.setLayoutData(gridData);

      createInstrumentationLoggingGroup(dsoClientDataGroup);
      createRuntimeLoggingGroup(dsoClientDataGroup);
      createRuntimeOuputOptionsGroup(dsoClientDataGroup);
      createFaultCountPane(dsoClientDataGroup);
      createModuleRepositoriesPanel(dsoClientDataGroup);
      createModulesPanel(dsoClientDataGroup);
    }

    private void createInstrumentationLoggingGroup(Composite parent) {
      Group instrumentationLoggingGroup = new Group(parent, SWT.BORDER);
      instrumentationLoggingGroup.setText(INSTRUMENTATION_LOGGING);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      instrumentationLoggingGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
      gridData.verticalSpan = 2;
      instrumentationLoggingGroup.setLayoutData(gridData);

      m_classCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_classCheck.setText(CLASS);
      m_hierarchyCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_hierarchyCheck.setText(HIERARCHY);
      m_locksCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_locksCheck.setText(LOCKS);
      m_transientRootCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_transientRootCheck.setText(TRANSIENT_ROOT);
      m_distributedMethodsCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_distributedMethodsCheck.setText(DISTRIBUTED_METHODS);
      m_rootsCheck = new Button(instrumentationLoggingGroup, SWT.CHECK);
      m_rootsCheck.setText(ROOTS);
    }

    private void createRuntimeLoggingGroup(Composite parent) {
      Group runtimeLoggingGroup = new Group(parent, SWT.BORDER);
      runtimeLoggingGroup.setText(RUNTIME_LOGGING);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      runtimeLoggingGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
      gridData.verticalSpan = 2;
      runtimeLoggingGroup.setLayoutData(gridData);

      m_lockDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_lockDebugCheck.setText(LOCK_DEBUG);
      m_distributedMethodDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_distributedMethodDebugCheck.setText(DISTRIBUTED_METHOD_DEBUG);
      m_fieldChangeDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_fieldChangeDebugCheck.setText(FIELD_CHANGE_DEBUG);
      m_nonPortableWarningCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_nonPortableWarningCheck.setText(NON_PORTABLE_WARNING);
      m_partialInstrumentationCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_partialInstrumentationCheck.setText(PARTIAL_INSTRUMENTATION);
      m_waitNotifyDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_waitNotifyDebugCheck.setText(WAIT_NOTIFY_DEBUG);
      m_newObjectDebugCheck = new Button(runtimeLoggingGroup, SWT.CHECK);
      m_newObjectDebugCheck.setText(NEW_OBJECT_DEBUG);
    }

    private void createRuntimeOuputOptionsGroup(Composite parent) {
      Group runtimeOutputOptionsGroup = new Group(parent, SWT.BORDER);
      runtimeOutputOptionsGroup.setText(RUNTIME_OUTPUT_OPTIONS);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      runtimeOutputOptionsGroup.setLayout(gridLayout);
      runtimeOutputOptionsGroup
          .setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));

      m_autoLockDetailsCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_autoLockDetailsCheck.setText(AUTOLOCK_DETAILS);
      m_callerCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_callerCheck.setText(CALLER);
      m_fullStackCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_fullStackCheck.setText(FULL_STACK);
      m_findNeededIncludesCheck = new Button(runtimeOutputOptionsGroup, SWT.CHECK);
      m_findNeededIncludesCheck.setText(FIND_NEEDED_INCLUDES);
    }

    private void createFaultCountPane(Composite parent) {
      Composite faultCountPane = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      faultCountPane.setLayout(gridLayout);
      faultCountPane.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
      Label faultCountLabel = new Label(faultCountPane, SWT.NONE);
      faultCountLabel.setText(FAULT_COUNT);
      faultCountLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
      m_faultCountSpinner = new Spinner(faultCountPane, SWT.BORDER);
      m_faultCountSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
    }

    private void createModuleRepositoriesPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 3;
      comp.setLayoutData(gridData);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(MODULE_REPOSITORIES);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      m_moduleRepoTable = new Table(tablePanel, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_moduleRepoTable.setHeaderVisible(true);
      m_moduleRepoTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(tablePanel, m_moduleRepoTable);

      TableColumn column0 = new TableColumn(m_moduleRepoTable, SWT.NONE);
      column0.setResizable(true);
      column0.setText(LOCATION);
      column0.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler

      m_addModuleRepo = new Button(buttonPanel, SWT.PUSH);
      m_addModuleRepo.setText(ADD);
      m_addModuleRepo.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addModuleRepo);

      m_removeModuleRepo = new Button(buttonPanel, SWT.PUSH);
      m_removeModuleRepo.setText(REMOVE);
      m_removeModuleRepo.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeModuleRepo);
    }

    private void createModulesPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 3;
      comp.setLayoutData(gridData);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(MODULES);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      m_moduleTable = new Table(tablePanel, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_moduleTable.setHeaderVisible(true);
      m_moduleTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(tablePanel, m_moduleTable);

      TableColumn column0 = new TableColumn(m_moduleTable, SWT.NONE);
      column0.setResizable(true);
      column0.setText(NAME);
      column0.pack();

      TableColumn column1 = new TableColumn(m_moduleTable, SWT.NONE);
      column1.setResizable(true);
      column1.setText(VERSION);
      column1.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler

      m_addModule = new Button(buttonPanel, SWT.PUSH);
      m_addModule.setText(ADD);
      m_addModule.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addModule);

      m_removeModule = new Button(buttonPanel, SWT.PUSH);
      m_removeModule.setText(REMOVE);
      m_removeModule.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeModule);
    }
  }
}
