/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package launch.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import refreshall.Activator;

public class WorkbenchOptionAction implements IWorkbenchWindowActionDelegate {

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

  public void run(IAction action) {
    Activator.getDefault().savePluginPreferences();
  }

  public void selectionChanged(IAction action, ISelection selection) {
  }

}
