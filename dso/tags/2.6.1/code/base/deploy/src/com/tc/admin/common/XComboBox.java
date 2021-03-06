/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.ComboBox;

import javax.swing.JPopupMenu;

public class XComboBox extends ComboBox {
  protected XPopupListener m_popupListener;
  
  public XComboBox() {
    super();
    m_popupListener = new XPopupListener(this);
    setPopupMenu(createPopup());
  }
  
  protected JPopupMenu createPopup() {
    return null;
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    m_popupListener.setPopupMenu(popupMenu);
  }

  public JPopupMenu getPopupMenu() {
    return m_popupListener.getPopupMenu();
  }
}
