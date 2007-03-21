/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.Label;
import org.eclipse.swt.widgets.Composite;
import org.terracotta.dso.editors.xmlbeans.XmlStringEnumCombo;

import com.terracottatech.config.DsoServerData;
import com.terracottatech.config.Persistence;

public class PersistencePanel extends ConfigurationEditorPanel
//  implements ActionListener,
//             ChangeListener,
//             XmlObjectStructureListener
{
  private DsoServerData      m_dsoServerData;
  private Persistence        m_persistence;
  private XmlStringEnumCombo m_persistenceCombo;
  private Label              m_persistenceComboLabel;

  public PersistencePanel(Composite parent, int style) {
    super(parent, style);
  }
  
//  public void load(ContainerResource containerRes) {
//    super.init(containerRes);
//    
//    m_persistenceCombo = (XmlStringEnumCombo)findComponent("PersistenceCombo");
//    m_persistenceCombo.init(Persistence.class, "mode");
//    
//    m_persistenceComboLabel = (Label)findComponent("PersistenceComboLabel");
//    m_persistenceComboLabel.addMouseListener(new MouseAdapter() {
//      public void mouseClicked(MouseEvent me) {
//        if(me.getClickCount() == 1) {
//          m_persistenceCombo.unset();
//        }
//      }
//    });
//  }
//
//  public void ensureXmlObject() {
//    super.ensureXmlObject();
//
//    if(m_persistence == null) {
//      removeListeners();
//      m_persistence = m_dsoServerData.addNewPersistence();
//      updateChildren();
//      addListeners();
//    }
//  }
//  
//  public boolean hasAnySet() {
//    return m_persistence != null &&
//           m_persistence.isSetMode();
//  }
//
//  public void actionPerformed(ActionEvent ae) {
//    setDirty();
//  }
//  
//  public void stateChanged(ChangeEvent e) {
//    setDirty();    
//  }
//  
//  public void structureChanged(XmlObjectStructureChangeEvent e) {
//    syncModel();
//  }
//  
//  private void syncModel() {
//    if(!hasAnySet() && m_dsoServerData.getPersistence() != null) {
//      m_dsoServerData.unsetPersistence();
//      m_persistence = null;
//      fireXmlObjectStructureChanged();
//      updateChildren();
//    }
//    else {
//      setDirty();
//    }
//  }
//
//  private void fireXmlObjectStructureChanged() {
//    fireXmlObjectStructureChanged(m_dsoServerData);    
//  }
//  
//  private void addListeners() {
//    m_persistenceCombo.addActionListener(this);
//    m_persistenceCombo.addXmlObjectStructureListener(this);
//
//  }
//  
//  private void removeListeners() {
//    m_persistenceCombo.removeActionListener(this);
//    m_persistenceCombo.removeXmlObjectStructureListener(this);
//
//  }
//  
//  private void updateChildren() {
//    m_persistenceCombo.setup(m_persistence);
//  }
//
//  public void setup(DsoServerData dsoServerData) {
//    setEnabled(true);
//    removeListeners();
//
//    m_dsoServerData = dsoServerData;
//    m_persistence   = m_dsoServerData != null ?
//                      m_dsoServerData.getPersistence() : null;
//    
//    updateChildren();
//    addListeners();
//  }
//  
//  public void tearDown() {
//    removeListeners();
//
//    m_dsoServerData = null;
//    m_persistence   = null;
//    
//    m_persistenceCombo.tearDown();
//
//    setEnabled(false);
//  }
}
