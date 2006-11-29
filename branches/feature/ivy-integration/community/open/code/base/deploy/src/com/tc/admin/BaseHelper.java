package com.tc.admin;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class BaseHelper {
  protected Icon m_refreshIcon;

  protected static final String ICONS_PATH = "/com/tc/admin/icons/";

  public Icon getRefreshIcon() {
    if(m_refreshIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"refresh.gif");
      m_refreshIcon = new ImageIcon(url);
    }

    return m_refreshIcon;
  }
}
