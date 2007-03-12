/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.terracotta.ui.util.SWTUtil;

public class SWTMethodChooser extends MessageDialog {

  private final Shell    m_parentShell;
  private final IProject m_project;

  public SWTMethodChooser(Shell shell, String title, String message, IProject project) {
    super(shell, title, null, message, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
    m_parentShell = shell;
    m_project = project;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setSize(400, 250);
    SWTUtil.placeDialogInCenter(m_parentShell, shell);
  }

  protected Control createCustomArea(Composite parent) {
    registerListeners(new MethodChooserLayout(parent));
    return parent;
  }

  private void registerListeners(MethodChooserLayout layout) {
    layout.m_selectButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        SWTMethodNavigator dialog = new SWTMethodNavigator(getShell(), "Select Method", m_project);
        dialog.open();
      }
    });
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

    final Text   m_selectField;
    final Button m_selectButton;
    final List   m_list;
    GridData     m_gridData;

    private MethodChooserLayout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);

      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      this.m_selectField = new Text(comp, SWT.SINGLE | SWT.BORDER);
      m_selectField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      this.m_selectButton = new Button(comp, SWT.PUSH);
      m_selectButton.setText("Select...");
      SWTUtil.applyDefaultButtonSize(m_selectButton);

      this.m_list = new List(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
      m_gridData = new GridData(GridData.FILL_BOTH);
      m_gridData.horizontalSpan = 2;
      m_list.setLayoutData(m_gridData);
    }
  }
}
