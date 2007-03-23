/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;

public class XmlStringField extends Text implements XmlObjectHolder {
  private XmlObjectHolderHelper m_helper;
  private boolean               m_listening;

  public XmlStringField(Composite parent, int style, Class parentType, String elementName) {
    super(parent, style);
    m_helper = new XmlObjectHolderHelper(parentType, elementName);
    MenuItem item = new MenuItem(getMenu(), SWT.NONE);
    item.setText(RESET);
    item.setAccelerator(RESET_STROKE);
    item.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent selectionevent) {
        unset();
      }
    });
  }

  protected void ensureXmlObject() {
  // TODO:
  // ConfigurationEditorPanel parent = (ConfigurationEditorPanel)
  // getAncestorOfClass(ConfigurationEditorPanel.class, this);
  //    
  // if(parent != null) {
  // parent.ensureXmlObject();
  // }
  }
  
  public void init(XmlObject parent) {
    m_listening = false;
    m_helper.setup(parent);
    setText(stringValue());
//    if(isSet()) {
//      m_helper.validateXmlObject(this);
//    }
    m_listening = true;
  }

  protected void fireActionPerformed() {
    if (m_listening) set();
  }

  public void tearDown() {
    m_helper.tearDown();
    m_listening = false;
    setText(null);
  }

  public String stringValue() {
    return isSet() ? m_helper.getStringValue() : m_helper.defaultStringValue();
  }

  public boolean isRequired() {
    return m_helper.isRequired();
  }

  public boolean isSet() {
    return m_helper.isSet();
  }

  public void set() {
    String s = getText();

    ensureXmlObject();
    m_helper.set(s);
    setText(s);
    // m_helper.validateXmlObject(this);
  }

  public void unset() {
    if (!isRequired()) {
      m_listening = false;
      m_helper.unset();
      setText(m_helper.defaultStringValue());
      m_listening = true;
    }
  }

  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.addXmlObjectStructureListener(listener);
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.removeXmlObjectStructureListener(listener);
  }
}
