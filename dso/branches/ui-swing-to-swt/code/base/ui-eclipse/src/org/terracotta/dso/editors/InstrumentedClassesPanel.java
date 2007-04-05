/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTComponentModel;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.Application;
import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;

public class InstrumentedClassesPanel extends ConfigurationEditorPanel implements SWTComponentModel {

  private static final String INCLUDE = "include";
  private static final String EXCLUDE = "exclude";
  private final Layout        m_layout;
  private State               m_state;

  public InstrumentedClassesPanel(Composite parent, int style) {
    super(parent, style);
    this.m_layout = new Layout(this);
  }

  // ================================================================================
  // INTERFACE
  // ================================================================================

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
    createContextListeners();
    initTableItems();
    setActive(true);
  }

  // ================================================================================
  // INIT LISTENERS
  // ================================================================================

  private void createContextListeners() {
    m_layout.m_table.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_layout.m_removeButton.setEnabled(true);
        int index = m_layout.m_table.getSelectionIndex();
        if (index == -1) {
          m_layout.m_moveUpButton.setEnabled(false);
          m_layout.m_moveDownButton.setEnabled(false);
        } else if (index == 0) {
          m_layout.m_moveUpButton.setEnabled(false);
          m_layout.m_moveDownButton.setEnabled(true);
        } else if (index == m_layout.m_table.getItemCount() -1) {
          m_layout.m_moveUpButton.setEnabled(true);
          m_layout.m_moveDownButton.setEnabled(false);
        } else {
          m_layout.m_moveUpButton.setEnabled(true);
          m_layout.m_moveDownButton.setEnabled(true);
        }
      }
    });
  }

  // ================================================================================
  // HELPERS
  // ================================================================================

  private void initTableItems() {
    SWTUtil.makeTableComboItem(m_layout.m_table, 0, new String[] { INCLUDE, EXCLUDE });
    XmlObject[] classes = m_state.classes.selectPath("*");
    for (int i = 0; i < classes.length; i++) {
      if (classes[i] instanceof Include) {
        createIncludeTableItem((Include) classes[i]);
      } else {
        createExcludeTableItem((ClassExpression) classes[i]);
      }
    }
  }

  private void createIncludeTableItem(Include include) {
    TableItem item = new TableItem(m_layout.m_table, SWT.NONE);
    item.setText(Layout.RULE_COLUMN, INCLUDE);
    item.setText(Layout.EXPRESSION_COLUMN, include.getClassExpression());
    item.setData(include);
  }

  private void createExcludeTableItem(ClassExpression exclude) {
    TableItem item = new TableItem(m_layout.m_table, SWT.NONE);
    item.setText(Layout.RULE_COLUMN, EXCLUDE);
    item.setText(Layout.EXPRESSION_COLUMN, exclude.getStringValue());
    item.setData(exclude);
  }

  // ================================================================================
  // STATE
  // ================================================================================

  private class State {
    final IProject             project;
    final XmlConfigContext     xmlContext;
    final XmlConfigUndoContext xmlUndoContext;
    final InstrumentedClasses  classes;

    private State(IProject project) {
      this.project = project;
      this.xmlContext = XmlConfigContext.getInstance(project);
      this.xmlUndoContext = XmlConfigUndoContext.getInstance(project);
      Application app = TcPlugin.getDefault().getConfiguration(project).getApplication();
      if (app == null) app = TcPlugin.getDefault().getConfiguration(project).addNewApplication();
      DsoApplication dso = app.getDso();
      if (dso == null) dso = app.addNewDso();
      InstrumentedClasses cl = dso.getInstrumentedClasses();
      if (cl == null) cl = dso.addNewInstrumentedClasses();
      this.classes = cl;
    }
  }

  // ================================================================================
  // LAYOUT
  // ================================================================================

  private static class Layout {

    private static final int    RULE_COLUMN           = 0;
    private static final int    EXPRESSION_COLUMN     = 1;
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
    private static final String ON_LOAD               = "On Load Behavior";
    private static final String DO_NOTHING            = "Do Nothing";
    private static final String CALL_A_METHOD         = "Call a Method";
    private static final String EXECUTE_CODE          = "Execute Code";

    private Table               m_table;
    private Button              m_honorTransientCheck;
    private Button              m_doNothingCheck;
    private Button              m_callAMethodCheck;
    private Text                m_callAMethodText;
    private Button              m_executeCodeCheck;
    private Text                m_executeCodeText;
    private Button              m_addButton;
    private Button              m_removeButton;
    private Button              m_moveUpButton;
    private Button              m_moveDownButton;

    private void reset() {
      m_table.removeAll();
      m_honorTransientCheck.setSelection(false);
      m_honorTransientCheck.setEnabled(false);
      m_doNothingCheck.setSelection(false);
      m_doNothingCheck.setEnabled(false);
      m_callAMethodCheck.setSelection(false);
      m_callAMethodCheck.setEnabled(false);
      m_callAMethodText.setText("");
      m_callAMethodText.setEnabled(false);
      m_executeCodeCheck.setSelection(false);
      m_executeCodeCheck.setEnabled(false);
      m_executeCodeText.setText("");
      m_executeCodeText.setEnabled(false);
      m_removeButton.setEnabled(false);
      m_moveUpButton.setEnabled(false);
      m_moveDownButton.setEnabled(false);
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
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

      m_table = new Table(tablePanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
      m_table.setHeaderVisible(true);
      m_table.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeWeightedWidth(tablePanel, m_table, new int[] { 1, 4 });
      SWTUtil.makeTableColumnsEditable(m_table, new int[] { 1 });

      TableColumn ruleColumn = new TableColumn(m_table, SWT.NONE, RULE_COLUMN);
      ruleColumn.setResizable(true);
      ruleColumn.setText(RULE);
      ruleColumn.pack();

      TableColumn expressionColumn = new TableColumn(m_table, SWT.NONE, EXPRESSION_COLUMN);
      expressionColumn.setResizable(true);
      expressionColumn.setText(EXPRESSION);
      expressionColumn.pack();

      Group detailGroup = new Group(sidePanel, SWT.BORDER);
      detailGroup.setText(DETAILS);
      detailGroup.setEnabled(false);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      detailGroup.setLayout(gridLayout);
      detailGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      m_honorTransientCheck = new Button(detailGroup, SWT.CHECK);
      m_honorTransientCheck.setText(HONOR_TRANSIENT);
      m_honorTransientCheck.setEnabled(false);

      new Label(detailGroup, SWT.NONE); // filler

      Group onLoadGroup = new Group(detailGroup, SWT.BORDER);
      onLoadGroup.setText(ON_LOAD);
      onLoadGroup.setEnabled(false);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      onLoadGroup.setLayout(gridLayout);
      onLoadGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

      m_doNothingCheck = new Button(onLoadGroup, SWT.RADIO);
      m_doNothingCheck.setText(DO_NOTHING);
      m_doNothingCheck.setEnabled(false);
      GridData gridData = new GridData();
      gridData.horizontalSpan = 2;
      m_doNothingCheck.setLayoutData(gridData);

      m_callAMethodCheck = new Button(onLoadGroup, SWT.RADIO);
      m_callAMethodCheck.setText(CALL_A_METHOD);
      m_callAMethodCheck.setEnabled(false);

      m_callAMethodText = new Text(onLoadGroup, SWT.BORDER);
      m_callAMethodText.setEnabled(false);
      int width = SWTUtil.textColumnsToPixels(m_callAMethodText, 50);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.minimumWidth = width;
      m_callAMethodText.setLayoutData(gridData);

      m_executeCodeCheck = new Button(onLoadGroup, SWT.RADIO);
      m_executeCodeCheck.setEnabled(false);
      m_executeCodeCheck.setText(EXECUTE_CODE);
      gridData = new GridData();
      gridData.horizontalSpan = 2;
      m_executeCodeCheck.setLayoutData(gridData);

      m_executeCodeText = new Text(onLoadGroup, SWT.BORDER | SWT.MULTI);
      m_executeCodeText.setEnabled(false);
      gridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
      gridData.horizontalSpan = 2;
      gridData.minimumHeight = SWTUtil.textRowsToPixels(m_executeCodeText, 4);
      m_executeCodeText.setLayoutData(gridData);

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      new Label(buttonPanel, SWT.NONE); // filler

      m_addButton = new Button(buttonPanel, SWT.PUSH);
      m_addButton.setText(ADD);
      m_addButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_addButton);

      m_removeButton = new Button(buttonPanel, SWT.PUSH);
      m_removeButton.setText(REMOVE);
      m_removeButton.setEnabled(false);
      m_removeButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeButton);

      new Label(buttonPanel, SWT.NONE); // filler

      m_moveUpButton = new Button(buttonPanel, SWT.PUSH);
      m_moveUpButton.setText(ORDER);
      m_moveUpButton.setEnabled(false);
      m_moveUpButton.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(UP)));
      m_moveUpButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_moveUpButton);

      m_moveDownButton = new Button(buttonPanel, SWT.PUSH);
      m_moveDownButton.setText(ORDER);
      m_moveDownButton.setEnabled(false);
      m_moveDownButton.setImage(new Image(parent.getDisplay(), this.getClass().getResourceAsStream(DOWN)));
      m_moveDownButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_moveDownButton);
    }
  }
}
