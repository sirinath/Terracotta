/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTComponentModel;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Client;

public class LocksPanel extends ConfigurationEditorPanel implements SWTComponentModel {

  private final Layout     m_layout;
  private State            m_state;
  private volatile boolean m_isActive;

  public LocksPanel(Composite parent, int style) {
    super(parent, style);
    this.m_layout = new Layout(this);
  }

  // ================================================================================
  // INTERFACE
  // ================================================================================

  public synchronized void addListener(UpdateEventListener listener, int type) {
  // not implemented
  }

  public synchronized void removeListener(UpdateEventListener listener, int type) {
  // not implemented
  }

  public synchronized void clearState() {
    setActive(false);
    m_layout.reset();
    m_state.xmlContext.detachComponentModel(this);
    m_state = null;
  }

  public synchronized void init(Object data) {
    if (m_isActive && m_state.project == (IProject) data) return;
    setActive(false);
    m_state = new State((IProject) data);
    setActive(true);
  }

  public synchronized boolean isActive() {
    return m_isActive;
  }

  public synchronized void setActive(boolean activate) {
    m_isActive = activate;
  }

  // ================================================================================
  // STATE
  // ================================================================================

  private class State {
    final IProject             project;
    final XmlConfigContext     xmlContext;
    final XmlConfigUndoContext xmlUndoContext;
    final Client               client;

    private State(IProject project) {
      this.project = project;
      this.xmlContext = XmlConfigContext.getInstance(project);
      this.xmlUndoContext = XmlConfigUndoContext.getInstance(project);
      this.client = TcPlugin.getDefault().getConfiguration(project).getClients();
    }
  }

  // ================================================================================
  // LAYOUT
  // ================================================================================

  private static class Layout {

    private static final String AUTO_LOCKS        = "Auto Locks";
    private static final String NAMED_LOCKS       = "Named Locks";
    private static final String LOCK_NAME         = "Lock Name";
    private static final String METHOD_EXPRESSION = "Method Expression";
    private static final String LOCK_LEVEL        = "Lock Level";
    private static final String ADD               = "Add...";
    private static final String REMOVE            = "Remove";
    
    public void reset() {
      
    }

    private Layout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      comp.setLayout(gridLayout);

      createAutoLocksPanel(comp);
      createNamedLocksPanel(comp);
    }

    private void createAutoLocksPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(AUTO_LOCKS);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      Table table = new Table(tablePanel, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      table.setHeaderVisible(true);
      table.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(tablePanel, table);

      TableColumn column0 = new TableColumn(table, SWT.NONE);
      column0.setResizable(true);
      column0.setText(METHOD_EXPRESSION);
      column0.pack();

      TableColumn column1 = new TableColumn(table, SWT.NONE);
      column1.setResizable(true);
      column1.setText(LOCK_LEVEL);
      column1.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler
      
      Button addButton = new Button(buttonPanel, SWT.PUSH);
      addButton.setText(ADD);
      addButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(addButton);

      Button removeButton = new Button(buttonPanel, SWT.PUSH);
      removeButton.setText(REMOVE);
      removeButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(removeButton);
    }

    private void createNamedLocksPanel(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(NAMED_LOCKS);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      Table table = new Table(tablePanel, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
      table.setHeaderVisible(true);
      table.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(tablePanel, table);

      TableColumn column0 = new TableColumn(table, SWT.NONE);
      column0.setResizable(true);
      column0.setText(LOCK_NAME);
      column0.pack();

      TableColumn column1 = new TableColumn(table, SWT.NONE);
      column1.setResizable(true);
      column1.setText(METHOD_EXPRESSION);
      column1.pack();

      TableColumn column2 = new TableColumn(table, SWT.NONE);
      column2.setResizable(true);
      column2.setText(LOCK_LEVEL);
      column2.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler
      
      Button addButton = new Button(buttonPanel, SWT.PUSH);
      addButton.setText(ADD);
      addButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(addButton);

      Button removeButton = new Button(buttonPanel, SWT.PUSH);
      removeButton.setText(REMOVE);
      removeButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(removeButton);
    }
  }
}
