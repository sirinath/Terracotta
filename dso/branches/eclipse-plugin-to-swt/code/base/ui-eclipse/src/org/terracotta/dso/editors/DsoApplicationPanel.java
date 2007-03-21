/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Composite;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;

import com.terracottatech.config.Application;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class DsoApplicationPanel extends ConfigurationEditorPanel implements ConfigurationEditorRoot,
    XmlObjectStructureListener {
  private IProject                 m_project;
  private TcConfig                 m_config;
  private Application              m_application;
  private DsoApplication           m_dsoApp;
  private InstrumentedClassesPanel m_instrumentedClassesPanel;
  private TransientFieldsPanel     m_transientFieldsPanel;
  private LocksPanel               m_locksPanel;
  private RootsPanel               m_rootsPanel;
  private DistributedMethodsPanel  m_distributedMethodsPanel;
  private BootClassesPanel         m_bootClassesPanel;

  public DsoApplicationPanel(Composite parent, int style) {
    super(parent, style);
//    load();
  }
//
//  public void load() { // XXX add tabs here
//    m_instrumentedClassesPanel = new InstrumentedClassesPanel();
//    m_transientFieldsPanel = new TransientFieldsPanel();
//    m_locksPanel = new LocksPanel();
//    m_rootsPanel = new RootsPanel();
//    m_distributedMethodsPanel = new DistributedMethodsPanel();
//    m_bootClassesPanel = new BootClassesPanel();
//  }
//
//  public void structureChanged(XmlObjectStructureChangeEvent e) {/**/}
//
//  private void addListeners() {
//    m_instrumentedClassesPanel.addXmlObjectStructureListener(this);
//    m_transientFieldsPanel.addXmlObjectStructureListener(this);
//    m_locksPanel.addXmlObjectStructureListener(this);
//    m_rootsPanel.addXmlObjectStructureListener(this);
//    m_distributedMethodsPanel.addXmlObjectStructureListener(this);
//    m_bootClassesPanel.addXmlObjectStructureListener(this);
//  }
//
//  private void removeListeners() {
//    m_instrumentedClassesPanel.removeXmlObjectStructureListener(this);
//    m_transientFieldsPanel.removeXmlObjectStructureListener(this);
//    m_locksPanel.removeXmlObjectStructureListener(this);
//    m_rootsPanel.removeXmlObjectStructureListener(this);
//    m_distributedMethodsPanel.removeXmlObjectStructureListener(this);
//    m_bootClassesPanel.removeXmlObjectStructureListener(this);
//  }
//
//  public void ensureXmlObject() {
//    if (m_dsoApp == null) {
//      removeListeners();
//      if (m_application == null) {
//        m_application = m_config.addNewApplication();
//      }
//      m_dsoApp = m_application.addNewDso();
//      initPanels();
//      addListeners();
//    }
//  }
//
//  public void setupInternal() {
//    TcPlugin plugin = TcPlugin.getDefault();
//
//    m_config = plugin.getConfiguration(m_project);
//    m_application = m_config != null ? m_config.getApplication() : null;
//    m_dsoApp = m_application != null ? m_application.getDso() : null;
//
//    initPanels();
//  }
//
//  private void initPanels() {
//    m_instrumentedClassesPanel.setup(m_project, m_dsoApp);
//    m_transientFieldsPanel.setup(m_project, m_dsoApp);
//    m_locksPanel.setup(m_project, m_dsoApp);
//    m_rootsPanel.setup(m_project, m_dsoApp);
//    m_distributedMethodsPanel.setup(m_project, m_dsoApp);
//    m_bootClassesPanel.setup(m_project, m_dsoApp);
//  }
//
//  public void updateInstrumentedClassesPanel() {
//    m_instrumentedClassesPanel.updateModel();
//  }
//
//  public void updateTransientsPanel() {
//    m_transientFieldsPanel.updateModel();
//  }
//
//  public void updateLocksPanel() {
//    m_locksPanel.updateModel();
//  }
//
//  public void updateRootsPanel() {
//    m_rootsPanel.updateModel();
//  }
//
//  public void updateDistributedMethodsPanel() {
//    m_distributedMethodsPanel.updateModel();
//  }
//
//  public void updateBootClassesPanel() {
//    m_bootClassesPanel.updateModel();
//  }
//
//  public void setup(IProject project) {
//    m_project = project;
//
//    setEnabled(true);
//    removeListeners();
//    setupInternal();
//    addListeners();
//  }
//
//  public IProject getProject() {
//    return m_project;
//  }
//
//  public void tearDown() {
//    removeListeners();
//
//    m_instrumentedClassesPanel.tearDown();
//    m_transientFieldsPanel.tearDown();
//    m_locksPanel.tearDown();
//    m_rootsPanel.tearDown();
//    m_distributedMethodsPanel.tearDown();
//    m_bootClassesPanel.tearDown();
//
//    setEnabled(false);
//  }

  // TODO: remove these
  public IProject getProject() {
    return null;
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    
  }
}
