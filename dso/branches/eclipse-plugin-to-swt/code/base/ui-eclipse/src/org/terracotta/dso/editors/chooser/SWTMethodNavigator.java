/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaWorkbenchAdapter;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.terracotta.ui.util.SWTUtil;

import java.util.ArrayList;
import java.util.List;

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
    layout.m_viewer.setContentProvider(new JavaMethodContentProvider());
    layout.m_viewer.setLabelProvider(new JavaElementLabelProvider());
    IJavaProject jproj = JavaCore.create(m_project);
    try {
      IJavaElement root = jproj.getPackageFragmentRoots()[0].getParent();
      layout.m_viewer.setInput(root);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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

  private class JavaMethodContentProvider extends WorkbenchContentProvider {
    protected IWorkbenchAdapter getAdapter(Object element) {
      return new JavaWorkbenchAdapter() {
        public Object[] getChildren(Object javaElement) {
          List list = new ArrayList();
          Object[] children = super.getChildren(javaElement);
          for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof IPackageFragment || children[i] instanceof ICompilationUnit
                || children[i] instanceof IType || children[i] instanceof IPackageFragmentRoot
                || children[i] instanceof IClassFile || children[i] instanceof IMethod) {
              if (!(children[i] instanceof IPackageFragment && super.getChildren(children[i]).length == 0)) {
                list.add(children[i]);
              }
            }
          }
          return list.toArray();
        }
      };
    }
  }

  // --------------------------------------------------------------------------------

  private static class MethodChooserLayout {

    final TreeViewer m_viewer;
    GridData         m_gridData;

    private MethodChooserLayout(Composite parent) {
      this.m_viewer = new TreeViewer(parent);
      m_viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
    }
  }
}
