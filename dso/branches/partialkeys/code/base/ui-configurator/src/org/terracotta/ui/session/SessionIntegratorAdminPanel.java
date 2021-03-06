/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.AdminClientPanel;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.XContainer;

import java.awt.BorderLayout;
import java.util.prefs.Preferences;

public class SessionIntegratorAdminPanel extends XContainer {
  private final SessionIntegratorContext sessionIntegratorContext;

  public SessionIntegratorAdminPanel(final SessionIntegratorContext sessionIntegratorContext) {
    super(new BorderLayout());
    this.sessionIntegratorContext = sessionIntegratorContext;
    add(new MonitorPanel(new AdminClientContext(new AdminClient()) {
      @Override
      public Preferences getPrefs() {
        return sessionIntegratorContext.getPrefs();
      }
    }));
  }

  protected boolean shouldAddAboutItem() {
    return false;
  }

  private class MonitorPanel extends AdminClientPanel {
    MonitorPanel(IAdminClientContext adminClientContext) {
      super(adminClientContext);
    }

    @Override
    protected Preferences getPreferences() {
      return sessionIntegratorContext.getPrefs().node("MonitorPanel");
    }

    @Override
    protected void storePreferences() {
      sessionIntegratorContext.storePrefs();
    }
  }
}
