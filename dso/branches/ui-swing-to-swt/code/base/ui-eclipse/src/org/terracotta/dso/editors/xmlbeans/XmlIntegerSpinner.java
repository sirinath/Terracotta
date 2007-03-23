/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;

import com.tc.admin.common.XAbstractAction;

import java.awt.event.ActionEvent;

import javax.swing.JFormattedTextField;
import javax.swing.SpinnerNumberModel;

public class XmlIntegerSpinner extends Spinner
  implements XmlObjectHolder
{
  private XmlObjectHolderHelper m_helper;
  private boolean               m_listening;

  public XmlIntegerSpinner(Composite parent, int style, Class parentClass, String elementName) {
    super(parent, style);
    m_helper = new XmlObjectHolderHelper(parentClass, elementName);
    // TODO:
//    setModel(new SpinnerNumberModel(m_helper.defaultIntegerValue(),
//        m_helper.minInclusive(),
//        m_helper.maxInclusive(),
//        new Integer(1)));
    
//    getActionMap().put(RESET, new ResetAction());
//    getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(RESET_STROKE, RESET);
  }

  private JFormattedTextField getTextField() {
//    return ((DefaultEditor)getEditor()).getTextField();
    return null; // XXX
  }
  
  protected void ensureXmlObject() {
//    TODO:
//    ConfigurationEditorPanel parent = (ConfigurationEditorPanel)
//      getAncestorOfClass(ConfigurationEditorPanel.class, this);
//    
//    if(parent != null) {
//      parent.ensureXmlObject();
//    }
  }
  
  private SpinnerNumberModel getSpinnerNumberModel() {
//    return (SpinnerNumberModel)getModel();
    return null; // XXX
  }
  
  private void setSpinnerValue(Number value) {
    getSpinnerNumberModel().setValue(value);
  }
  
  public Number getNumber() {
    return getSpinnerNumberModel().getNumber();    
  }
  
  public void setup(XmlObject parent) {
    m_listening = false;
    m_helper.setup(parent);
    setSpinnerValue(integerValue());
//    if(isSet()) {
//      m_helper.validateXmlObject(this);
//    }
    m_listening = true;
  }

  protected void fireStateChanged() {
    if(m_listening) {
      set();
    }
//    super.fireStateChanged();
  }
  
  public void tearDown() {
    m_helper.tearDown();
    m_listening = false;
    setSpinnerValue(m_helper.defaultIntegerValue());
  }
  
  public Integer integerValue() {
    return isSet() ? m_helper.getIntegerValue() : m_helper.defaultIntegerValue();
  }
  
  public boolean isRequired() {
    return m_helper.isRequired();
  }
  
  public boolean isSet() {
    return m_helper.isSet();
  }
  
  public void set() {
    int iVal = getNumber().intValue();
    
    ensureXmlObject();
    m_helper.set(Integer.toString(iVal));
    setSpinnerValue(new Integer(iVal));
//    m_helper.validateXmlObject(this);
  }
  
  public void unset() {
    if(!isRequired()) {
      m_listening = false;
      m_helper.unset();
      Integer iVal = m_helper.defaultIntegerValue();
      setSpinnerValue(iVal);
      getTextField().setValue(iVal);
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
