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
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.ui.JavaWorkbenchAdapter;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.internal.ui.packageview.WorkingSetAwareJavaElementSorter;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.terracotta.dso.PatternHelper;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SWTMethodNavigator extends MessageDialog {

  private final Shell            m_parentShell;
  private final IProject         m_project;
  private final EventMulticaster m_valueListener;
  private final List             m_selectedValues;

  public SWTMethodNavigator(Shell shell, String title, IProject project) {
    super(shell, title, null, null, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
    this.m_parentShell = shell;
    this.m_project = project;
    this.m_valueListener = new EventMulticaster();
    this.m_selectedValues = new ArrayList();
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
    getButton(getDefaultButtonIndex()).setEnabled(false);
    return comp;
  }

  protected Control createCustomArea(Composite parent) {
    initLayout(new MethodChooserLayout(parent));
    return parent;
  }

  private void initLayout(final MethodChooserLayout layout) {
    layout.m_viewer.setContentProvider(new JavaHierarchyContentProvider());
    layout.m_viewer.setLabelProvider(new JavaElementLabelProvider());
    layout.m_viewer.setSorter(new WorkingSetAwareJavaElementSorter());
    IJavaProject jproj = JavaCore.create(m_project);
    IJavaElement root = jproj.getJavaModel();
    layout.m_viewer.setInput(root);

    layout.m_viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        m_selectedValues.removeAll(m_selectedValues);
        StructuredSelection selection = (StructuredSelection) event.getSelection();
        List selectedMethods = new ArrayList();
        if (!selection.isEmpty()) {
          for (Iterator i = selection.iterator(); i.hasNext();) {
            Object element;
            if ((element = i.next()) instanceof IMethod) {
              IMethod method = (IMethod) element;
              m_selectedValues.add(PatternHelper.getExecutionPattern(method));
              selectedMethods.add(element);
            }
          }
          layout.m_viewer.removeSelectionChangedListener(this);
          event.getSelectionProvider().setSelection(new StructuredSelection(selectedMethods.toArray()));
          layout.m_viewer.addSelectionChangedListener(this);
        }
        if (selectedMethods.size() > 0) getButton(getDefaultButtonIndex()).setEnabled(true);
        else getButton(getDefaultButtonIndex()).setEnabled(false);
      }
    });

    layout.m_viewer.addFilter(new ViewerFilter() {
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof ClassPathContainer || element instanceof IJavaProject
            || element instanceof IPackageFragment || element instanceof ICompilationUnit || element instanceof IType
            || element instanceof IPackageFragmentRoot || element instanceof IClassFile || element instanceof IMethod) { return true; }
        return false;
      }
    });
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      m_valueListener.fireUpdateEvent(m_selectedValues.toArray(new String[0]));
    }
    super.buttonPressed(buttonId);
  }

  public void addValueListener(UpdateEventListener listener) {
    m_valueListener.addListener(listener);
  }

  // --------------------------------------------------------------------------------

  private class JavaHierarchyContentProvider extends PackageExplorerContentProvider {

    private final WorkbenchContentProvider m_workbench;

    public JavaHierarchyContentProvider() {
      super(true);
      this.m_workbench = new WorkbenchContentProvider() {
        protected IWorkbenchAdapter getAdapter(Object element) {
          return new JavaWorkbenchAdapter() {
            public Object[] getChildren(Object parentElement) {
              List subset = new ArrayList();
              Object[] children = super.getChildren(parentElement);
              for (int i = 0; i < children.length; i++) {
                if (!(children[i] instanceof IPackageFragment && super.getChildren(children[i]).length == 0)
                    && !(children[i] instanceof PackageFragmentRoot && !(children[i] instanceof JarPackageFragmentRoot))) {
                  subset.add(children[i]);
                }
              }
              return subset.toArray();
            }
          };
        }
      };
    }

    public Object[] getChildren(Object parentElement) {
      List subset = new ArrayList();
      Object[] children = super.getChildren(parentElement);
      for (int i = 0; i < children.length; i++) {
        if (children[i] instanceof JarPackageFragmentRoot) {
          Object[] workbenchElements = m_workbench.getChildren(((IJavaElement) children[i]).getParent());
          return workbenchElements;
        }
        if (children[i].getClass().getName().equals("org.eclipse.jdt.internal.core.JarPackageFragment")) {
          Object[] workbenchElements = m_workbench.getChildren(parentElement);
          return workbenchElements;
        }
        if (!(children[i] instanceof IPackageFragment && super.getChildren(children[i]).length == 0)) {
          subset.add(children[i]);
        }
      }
      return subset.toArray();
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
