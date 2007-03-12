/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.terracotta.ui.util.SWTUtil;

public class SWTMethodNavigator extends MessageDialog {

  private final Shell    m_parentShell;
  private final IProject m_project;

  public SWTMethodNavigator(Shell shell, String title, IProject project) {
    super(shell, title, null, null, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
    this.m_parentShell = shell;
    this.m_project = project;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setSize(400, 300);
    SWTUtil.placeDialogInCenter(m_parentShell, shell);
  }

  protected Control createDialogArea(Composite parent) {
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    parent.setLayout(gridLayout);
    return super.createDialogArea(parent);
  }

  protected Control createButtonBar(Composite parent) {
    Composite comp = (Composite) super.createButtonBar(parent);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    gridLayout.marginWidth = 5;
    gridLayout.marginHeight = 5;
    comp.setLayout(gridLayout);
    return comp;
  }

  protected Control createCustomArea(Composite parent) {
    registerListeners(new MethodChooserLayout(parent));
    return parent;
  }

  private void registerListeners(MethodChooserLayout layout) {
    layout.m_viewer.setContentProvider(new StandardJavaElementContentProvider());
    layout.m_viewer.setLabelProvider(new JavaElementLabelProvider());
    layout.m_viewer.setInput("root");
//    IJavaProject jproj = JavaCore.create(m_project);
//    try {
//      layout.m_viewer.setInput(jproj.getPackageFragmentRoots());
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
  }
  
  // XXX: use okPressed();
  // protected void buttonPressed(int buttonId) {
  // if (buttonId == IDialogConstants.OK_ID) {
  // if (m_valueListener != null) m_valueListener.setValues(m_moduleName.getText(), m_moduleVersion.getText());
  // }
  // super.buttonPressed(buttonId);
  // }
  //
  // public void addValueListener(ValueListener listener) {
  // this.m_valueListener = listener;
  // }
  //
  // // --------------------------------------------------------------------------------
  //
  // public static interface ValueListener {
  // void setValues(String name, String version);
  // }

  // --------------------------------------------------------------------------------

  private static class MethodChooserLayout {

    final TreeViewer m_viewer;
    GridData         m_gridData;

    private MethodChooserLayout(Composite parent) {
      // Composite comp = new Composite(parent, SWT.NONE);
      //
      // GridLayout gridLayout = new GridLayout();
      // gridLayout.numColumns = 0;
      // gridLayout.marginWidth = 0;
      // gridLayout.marginHeight = 0;
      // comp.setLayout(gridLayout);
      // comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      // this.m_list = new List(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
      //Tree tree = new Tree(parent, SWT.SINGLE | );
      this.m_viewer = new TreeViewer(parent);
      // IJavaProject project = TcPlugin.getDefault().
      
    }
  }
}
