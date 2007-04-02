/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTComponentModel;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Client;

public class InstrumentedClassesPanel extends ConfigurationEditorPanel implements SWTComponentModel {

  private final Layout        m_layout;
  private State               m_state;
  private volatile boolean    m_isActive;

  public InstrumentedClassesPanel(Composite parent, int style) {
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
  
//================================================================================
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

    private static final String UP                    = "/com/tc/admin/icons/view_menu.gif";
    private static final String DOWN                  = "/com/tc/admin/icons/hide_menu.gif";
    private static final String INSTRUMENTATION_RULES = "Instrumentation Rules";
    private static final String RULE                  = "Rule";
    private static final String EXPRESSION            = "Expression";
    private static final String DETAILS               = "Details";
    private static final String ADD                   = "Add...";
    private static final String REMOVE                = "Remove";
    private static final String ORDER                 = "Order";
    private static final String HONOR_TRANSIENT       = "Honor Transient";
    private static final String ON_LOAD               = "On Load...";

    public void reset() {
      
    }
    
    private Layout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(sidePanel, SWT.NONE);
      label.setText(INSTRUMENTATION_RULES);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Table table = new Table(tablePanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.CHECK);
      table.setHeaderVisible(true);
      table.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeWeightedWidth(tablePanel, table, new int[] { 3, 2, 7 });
//      SWTUtil.makeTableColumnsEditable(table, new int[] { 1, 2 });

      TableColumn detailsColumn = new TableColumn(table, SWT.NONE);
      detailsColumn.setResizable(true);
      detailsColumn.setText(DETAILS);
      detailsColumn.pack();

      TableColumn ruleColumn = new TableColumn(table, SWT.NONE);
      ruleColumn.setResizable(true);
      ruleColumn.setText(RULE);
      ruleColumn.pack();

      TableColumn expressionColumn = new TableColumn(table, SWT.NONE);
      expressionColumn.setResizable(true);
      expressionColumn.setText(EXPRESSION);
      expressionColumn.pack();

      // XXX
      TableItem item1 = new TableItem(table, SWT.NONE);
      item1.setText(new String[] { HONOR_TRANSIENT, "Include", "tutorial.Slider" });
      TableItem item2 = new TableItem(table, SWT.NONE);
      item2.setText(new String[] { HONOR_TRANSIENT, "Include", "javax.swing.DefaultBoundedRangeModel" });

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
      addButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(addButton);

      Button removeButton = new Button(buttonPanel, SWT.PUSH);
      removeButton.setText(REMOVE);
      removeButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(removeButton);

      new Label(buttonPanel, SWT.NONE); // filler
      
      Button onLoadButton = new Button(buttonPanel, SWT.PUSH);
      onLoadButton.setText(ON_LOAD + "\r-behavior-");
      onLoadButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(onLoadButton);
      
      new Label(buttonPanel, SWT.NONE); // filler

      Button moveUpButton = new Button(buttonPanel, SWT.PUSH);
      moveUpButton.setText(ORDER);
      moveUpButton.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(UP)));
      moveUpButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(moveUpButton);

      Button moveDownButton = new Button(buttonPanel, SWT.PUSH);
      moveDownButton.setText(ORDER);
      moveDownButton.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(DOWN)));
      moveDownButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(moveDownButton);
    }
  }
}
