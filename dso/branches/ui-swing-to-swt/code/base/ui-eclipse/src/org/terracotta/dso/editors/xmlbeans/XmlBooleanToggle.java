/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.tc.admin.common.XAbstractAction;

import java.awt.event.ActionEvent;

public class XmlBooleanToggle extends Button implements XmlObjectHolder {
  private XmlObjectHolderHelper m_helper;
  private boolean               m_listening;

  public XmlBooleanToggle(Composite parent, Class parentClass, String elementName) {
    super(parent, SWT.CHECK);
    m_helper = new XmlObjectHolderHelper(parentClass, elementName);
//    getActionMap().put(RESET, new ResetAction());
//    getInputMap().put(RESET_STROKE, RESET);
  }

  protected void ensureXmlObject() {
//    TODO:
//    ConfigurationEditorPanel parent = (ConfigurationEditorPanel) SWTUtil.getAncestorOfClass(
//        ConfigurationEditorPanel.class, this);
//
//    if (parent != null) {
//      parent.ensureXmlObject();
//    }
  }

  public void init(XmlObject parent) {
    m_listening = false;
    m_helper.setup(parent);
    setSelection(booleanValue());
//    if (isSet()) {
//      m_helper.validateXmlObject(this);
//    }
    m_listening = true;
  }

  protected void fireActionPerformed(ActionEvent ae) {
    if (m_listening) {
      set();
    }
//    super.fireActionPerformed(ae);
  }

  public void tearDown() {
    m_helper.tearDown();
    m_listening = false;
    setSelection(false);
  }

  public boolean booleanValue() {
    return isSet() ? m_helper.getBoolean() : m_helper.defaultBoolean();
  }

  public boolean isRequired() {
    return m_helper.isRequired();
  }

  public boolean isSet() {
    return m_helper.isSet();
  }

  public void set() {
    boolean isSelected = getSelection();
    String s = Boolean.toString(isSelected);

    ensureXmlObject();
    m_helper.set(s);
    setSelection(isSelected);
//    m_helper.validateXmlObject(this);
  }

  public void unset() {
    if (!isRequired()) {
      m_listening = false;
      m_helper.unset();
      setSelection(m_helper.defaultBoolean());
      m_listening = true;
    }
  }

  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.addXmlObjectStructureListener(listener);
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.removeXmlObjectStructureListener(listener);
  }

  class ResetAction extends XAbstractAction {
    ResetAction() {
      super("Reset");
      setShortDescription("Reset to default value");
    }

    public void actionPerformed(ActionEvent ae) {
      unset();
    }
  }
}
