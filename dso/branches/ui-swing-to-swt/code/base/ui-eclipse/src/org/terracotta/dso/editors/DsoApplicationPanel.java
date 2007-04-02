/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTComponentModel;

import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Client;

public class DsoApplicationPanel extends ConfigurationEditorPanel implements SWTComponentModel {

  private final Layout     m_layout;
  private State            m_state;
  private volatile boolean m_isActive;

  public DsoApplicationPanel(Composite parent, int style) {
    super(parent, style);
    this.m_layout = new Layout(this);
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
    m_state.xmlContext.detachComponentModel(this);
    m_state = null;
  }

  public synchronized void init(Object data) {
    if (m_isActive && m_state.project == (IProject) data) return;
    setActive(false);
    IProject project = (IProject) data;
    m_state = new State(project);
    m_layout.m_bootClasses.init(project);
    m_layout.m_distributedMethods.init(project);
    m_layout.m_instrumentedClasses.init(project);
    m_layout.m_locks.init(project);
    m_layout.m_roots.init(project);
    m_layout.m_transientFields.init(project);
    setActive(true);
  }

  public synchronized boolean isActive() {
    return m_isActive;
  }

  public synchronized void setActive(boolean activate) {
    m_isActive = activate;
  }

  // ================================================================================
  // STATE
  // ================================================================================

  private class State {
    final IProject             project;
    final XmlConfigContext     xmlContext;
    final XmlConfigUndoContext xmlUndoContext;
    final Client               client;

    private State(IProject project) {
      this.project = project;
      this.xmlContext = XmlConfigContext.getInstance(project);
      this.xmlUndoContext = XmlConfigUndoContext.getInstance(project);
      this.client = TcPlugin.getDefault().getConfiguration(project).getClients();
    }
  }

  // ================================================================================
  // LAYOUT
  // ================================================================================

  private static class Layout {

    private static final String      ROOTS_ICON                = "/com/tc/admin/icons/hierarchicalLayout.gif";
    private static final String      LOCKS_ICON                = "/com/tc/admin/icons/deadlock_view.gif";
    private static final String      TRANSIENT_FIELDS_ICON     = "/com/tc/admin/icons/transient.gif";
    private static final String      INSTRUMENTED_CLASSES_ICON = "/com/tc/admin/icons/class_obj.gif";
    private static final String      DISTRIBUTED_METHODS_ICON  = "/com/tc/admin/icons/jmeth_obj.gif";
    private static final String      BOOT_CLASSES_ICON         = "/com/tc/admin/icons/jar_obj.gif";
    private static final String      ROOTS                     = "Roots";
    private static final String      LOCKS                     = "Locks";
    private static final String      TRANSIENT_FIELDS          = "Transient Fields";
    private static final String      INSTRUMENTED_CLASSES      = "Instrumented Classes";
    private static final String      DISTRIBUTED_METHODS       = "Distributed Methods";
    private static final String      BOOT_CLASSES              = "Boot Classes";
    private RootsPanel               m_roots;
    private LocksPanel               m_locks;
    private TransientFieldsPanel     m_transientFields;
    private InstrumentedClassesPanel m_instrumentedClasses;
    private DistributedMethodsPanel  m_distributedMethods;
    private BootClassesPanel         m_bootClasses;

    public void reset() {
      // not implemented
    }

    private Layout(Composite parent) {
      final TabFolder tabFolder = new TabFolder(parent, SWT.BORDER);

      TabItem rootsTab = new TabItem(tabFolder, SWT.NONE);
      rootsTab.setText(ROOTS);
      rootsTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(ROOTS_ICON)));
      rootsTab.setControl(m_roots = new RootsPanel(tabFolder, SWT.NONE));

      TabItem locksTab = new TabItem(tabFolder, SWT.NONE);
      locksTab.setText(LOCKS);
      locksTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(LOCKS_ICON)));
      locksTab.setControl(m_locks = new LocksPanel(tabFolder, SWT.NONE));

      TabItem transientFieldsTab = new TabItem(tabFolder, SWT.NONE);
      transientFieldsTab.setText(TRANSIENT_FIELDS);
      transientFieldsTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(
          TRANSIENT_FIELDS_ICON)));
      transientFieldsTab.setControl(m_transientFields = new TransientFieldsPanel(tabFolder, SWT.NONE));

      TabItem instrumentedClassesTab = new TabItem(tabFolder, SWT.NONE);
      instrumentedClassesTab.setText(INSTRUMENTED_CLASSES);
      instrumentedClassesTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(
          INSTRUMENTED_CLASSES_ICON)));
      instrumentedClassesTab.setControl(m_instrumentedClasses = new InstrumentedClassesPanel(tabFolder, SWT.NONE));

      TabItem distributedMethodsTab = new TabItem(tabFolder, SWT.NONE);
      distributedMethodsTab.setText(DISTRIBUTED_METHODS);
      distributedMethodsTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(
          DISTRIBUTED_METHODS_ICON)));
      distributedMethodsTab.setControl(m_distributedMethods = new DistributedMethodsPanel(tabFolder, SWT.NONE));

      TabItem bootClassesTab = new TabItem(tabFolder, SWT.NONE);
      bootClassesTab.setText(BOOT_CLASSES);
      bootClassesTab.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(BOOT_CLASSES_ICON)));
      bootClassesTab.setControl(m_bootClasses = new BootClassesPanel(tabFolder, SWT.NONE));

      tabFolder.pack();
      tabFolder.setSelection(rootsTab);
    }
  }
}
