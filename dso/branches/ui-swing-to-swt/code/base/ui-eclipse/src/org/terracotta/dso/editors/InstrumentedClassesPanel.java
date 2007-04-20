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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.editors.chooser.ClassBehavior;
import org.terracotta.dso.editors.chooser.ExpressionChooser;
import org.terracotta.dso.editors.chooser.NavigatorBehavior;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigEvent;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;

public class InstrumentedClassesPanel extends ConfigurationEditorPanel {

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

  public synchronized void refreshContent() {
    m_layout.reset();
    initTableItems();
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
        } else if (index == m_layout.m_table.getItemCount() - 1) {
          m_layout.m_moveUpButton.setEnabled(true);
          m_layout.m_moveDownButton.setEnabled(false);
        } else {
          m_layout.m_moveUpButton.setEnabled(true);
          m_layout.m_moveDownButton.setEnabled(true);
        }
        TableItem item = m_layout.m_table.getItem(m_layout.m_table.getSelectionIndex());
        if (item.getText(Layout.RULE_COLUMN).equals(INCLUDE)) {
          initIncludeAttributes();
        } else if (item.getText(Layout.RULE_COLUMN).equals(EXCLUDE)) {
          m_layout.resetIncludeAttributes();
        }
      }
    });
    // - add class
    m_layout.m_addButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_table.forceFocus();
        NavigatorBehavior behavior = new ClassBehavior();
        ExpressionChooser chooser = new ExpressionChooser(getShell(), behavior.getTitle(), ClassBehavior.ADD_MSG,
            m_state.project, behavior);
        chooser.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent updateEvent) {
            String[] items = (String[]) updateEvent.data;
            for (int i = 0; i < items.length; i++) {
              XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.CREATE_INSTRUMENTED_CLASS);
              event.data = items[i];
              m_state.xmlContext.notifyListeners(event);
            }
          }
        });
        setActive(true);
        chooser.open();
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        createIncludeTableItem((Include) event.element);
      }
    }, XmlConfigEvent.NEW_INSTRUMENTED_CLASS, this);
    // - remove class
    m_layout.m_removeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_table.forceFocus();
        int selected = m_layout.m_table.getSelectionIndex();
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.DELETE_INSTRUMENTED_CLASS);
        event.index = selected;
        XmlObject data = (XmlObject) m_layout.m_table.getItem(selected).getData();
        event.data = data.getDomNode();
        setActive(true);
        m_state.xmlContext.notifyListeners(event);
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        m_layout.m_table.remove(((XmlConfigEvent) e).index);
        m_layout.m_removeButton.setEnabled(false);
      }
    }, XmlConfigEvent.REMOVE_INSTRUMENTED_CLASS, this);
    // - element order buttons
    m_layout.m_moveUpButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_table.forceFocus();
        int selected = m_layout.m_table.getSelectionIndex();
        if (selected - 1 < 0) return;
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.INSTRUMENTED_CLASS_ORDER_UP);
        event.element = (XmlObject) m_layout.m_table.getItem(selected).getData();
        event.data = m_layout.m_table.getItem(selected - 1).getData();
        event.variable = m_state.xmlContext.getParentElementProvider().hasInstrumentedClasses();
        event.index = selected;
        setActive(true);
        m_state.xmlContext.notifyListeners(event);
      }
    });
    m_layout.m_moveDownButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        setActive(false);
        m_layout.m_table.forceFocus();
        int selected = m_layout.m_table.getSelectionIndex();
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.INSTRUMENTED_CLASS_ORDER_DOWN);
        event.element = (XmlObject) m_layout.m_table.getItem(selected).getData();
        if (selected + 2 < m_layout.m_table.getItemCount()) event.data = m_layout.m_table.getItem(selected + 2)
            .getData();
        event.variable = m_state.xmlContext.getParentElementProvider().hasInstrumentedClasses();
        event.index = selected;
        setActive(true);
        m_state.xmlContext.notifyListeners(event);
      }
    });
    // - element order listeners
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        int index = event.index;
        if (index == 0) return;
        TableItem item = m_layout.m_table.getItem(index);
        TableItem movedItem = new TableItem(m_layout.m_table, SWT.NONE, index - 1);
        movedItem.setText(new String[] { item.getText(Layout.RULE_COLUMN), item.getText(Layout.EXPRESSION_COLUMN) });
        m_layout.m_table.remove(m_layout.m_table.indexOf(item));
        refreshTableItemXmlData();
        m_layout.m_table.select(m_layout.m_table.indexOf(movedItem));
        if (m_layout.m_table.indexOf(movedItem) == 0) m_layout.m_moveUpButton.setEnabled(false);
        m_layout.m_moveDownButton.setEnabled(true);
      }
    }, XmlConfigEvent.INSTRUMENTED_CLASS_ORDER_UP, this);
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        int index = event.index;
        int count = m_layout.m_table.getItemCount();
        if (index == count - 1) return;
        TableItem item = m_layout.m_table.getItem(index);
        TableItem movedItem = new TableItem(m_layout.m_table, SWT.NONE, index + 2);
        movedItem.setText(new String[] { item.getText(Layout.RULE_COLUMN), item.getText(Layout.EXPRESSION_COLUMN) });
        m_layout.m_table.remove(m_layout.m_table.indexOf(item));
        refreshTableItemXmlData();
        m_layout.m_table.select(m_layout.m_table.indexOf(movedItem));
        if (m_layout.m_table.indexOf(movedItem) == count - 1) m_layout.m_moveDownButton.setEnabled(false);
        m_layout.m_moveUpButton.setEnabled(true);
      }
    }, XmlConfigEvent.INSTRUMENTED_CLASS_ORDER_DOWN, this);
    // - table cell update
    m_layout.m_table.addListener(SWT.SetData, new Listener() {
      public void handleEvent(Event event) {
        if (!m_isActive) return;
        TableItem item = (TableItem) event.item;
        XmlObject xmlObj = (XmlObject) item.getData();
        int type = -1;
        switch (event.index) {
          case Layout.RULE_COLUMN:
            type = XmlConfigEvent.INSTRUMENTED_CLASS_RULE;
            break;
          case Layout.EXPRESSION_COLUMN:
            type = XmlConfigEvent.INSTRUMENTED_CLASS_EXPRESSION;
            break;
          default:
            break;
        }
        XmlConfigEvent e = new XmlConfigEvent(item.getText(event.index), null, xmlObj, type);
        if (type == XmlConfigEvent.INSTRUMENTED_CLASS_RULE) {
          e.variable = m_state.xmlContext.getParentElementProvider().hasInstrumentedClasses();
        }
        m_state.xmlContext.notifyListeners(e);
      }
    });
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        if (event.element instanceof Include) initIncludeAttributes();
        else m_layout.enableIncludeAttributes(false);
        refreshTableItemXmlData();
      }
    }, XmlConfigEvent.INSTRUMENTED_CLASS_RULE, this);
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        if (event.data == null) event.data = "";
        TableItem[] items = m_layout.m_table.getItems();
        for (int i = 0; i < items.length; i++) {
          if (items[i].getData() == event.element) {
            items[i].setText(Layout.EXPRESSION_COLUMN, (String) event.data);
          }
        }
      }
    }, XmlConfigEvent.INSTRUMENTED_CLASS_EXPRESSION, this);
  }

  // ================================================================================
  // HELPERS
  // ================================================================================

  private void initTableItems() {
    InstrumentedClasses classesElement = m_state.xmlContext.getParentElementProvider().hasInstrumentedClasses();
    if (classesElement == null) return;
    SWTUtil.makeTableComboItem(m_layout.m_table, 0, new String[] { INCLUDE, EXCLUDE });
    XmlObject[] classes = classesElement.selectPath("*");
    for (int i = 0; i < classes.length; i++) {
      if (classes[i] instanceof Include) {
        createIncludeTableItem((Include) classes[i]);
      } else {
        createExcludeTableItem((ClassExpression) classes[i]);
      }
    }
  }

  // this can fall out of sync if the table and model don't match. Timing is imperative
  private void refreshTableItemXmlData() {
    InstrumentedClasses classesElement = m_state.xmlContext.getParentElementProvider().hasInstrumentedClasses();
    if (classesElement == null) return;
    XmlObject[] classes = classesElement.selectPath("*");
    for (int i = 0; i < classes.length; i++) {
      m_layout.m_table.getItem(i).setData(classes[i]);
    }
  }

  private void initIncludeAttributes() {
    m_layout.enableIncludeAttributes(true);
  }

  private void createIncludeTableItem(Include include) {
    TableItem item = new TableItem(m_layout.m_table, SWT.NONE);
    item.setText(Layout.RULE_COLUMN, INCLUDE);
    item.setText(Layout.EXPRESSION_COLUMN, include.getClassExpression() + "");
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

    private State(IProject project) {
      this.project = project;
      this.xmlContext = XmlConfigContext.getInstance(project);
      this.xmlUndoContext = XmlConfigUndoContext.getInstance(project);
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
    private Group               m_onLoadGroup;
    private Group               m_detailGroup;

    private void reset() {
      m_table.removeAll();
      m_removeButton.setEnabled(false);
      m_moveUpButton.setEnabled(false);
      m_moveDownButton.setEnabled(false);
      resetIncludeAttributes();
    }

    private void resetIncludeAttributes() {
      m_detailGroup.setEnabled(false);
      m_onLoadGroup.setEnabled(false);
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
    }

    private void enableIncludeAttributes(boolean enable) {
      m_detailGroup.setEnabled(enable);
      m_onLoadGroup.setEnabled(enable);
      m_honorTransientCheck.setEnabled(enable);
      m_doNothingCheck.setEnabled(enable);
      m_callAMethodCheck.setEnabled(enable);
      m_executeCodeCheck.setEnabled(enable);
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

      m_detailGroup = new Group(sidePanel, SWT.BORDER);
      m_detailGroup.setText(DETAILS);
      m_detailGroup.setEnabled(false);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      m_detailGroup.setLayout(gridLayout);
      m_detailGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      m_honorTransientCheck = new Button(m_detailGroup, SWT.CHECK);
      m_honorTransientCheck.setText(HONOR_TRANSIENT);
      m_honorTransientCheck.setEnabled(false);

      new Label(m_detailGroup, SWT.NONE); // filler

      m_onLoadGroup = new Group(m_detailGroup, SWT.BORDER);
      m_onLoadGroup.setText(ON_LOAD);
      m_onLoadGroup.setEnabled(false);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 5;
      gridLayout.marginHeight = 5;
      m_onLoadGroup.setLayout(gridLayout);
      m_onLoadGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

      m_doNothingCheck = new Button(m_onLoadGroup, SWT.RADIO);
      m_doNothingCheck.setText(DO_NOTHING);
      m_doNothingCheck.setEnabled(false);
      GridData gridData = new GridData();
      gridData.horizontalSpan = 2;
      m_doNothingCheck.setLayoutData(gridData);

      m_callAMethodCheck = new Button(m_onLoadGroup, SWT.RADIO);
      m_callAMethodCheck.setText(CALL_A_METHOD);
      m_callAMethodCheck.setEnabled(false);

      m_callAMethodText = new Text(m_onLoadGroup, SWT.BORDER);
      m_callAMethodText.setEnabled(false);
      int width = SWTUtil.textColumnsToPixels(m_callAMethodText, 50);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.minimumWidth = width;
      m_callAMethodText.setLayoutData(gridData);

      m_executeCodeCheck = new Button(m_onLoadGroup, SWT.RADIO);
      m_executeCodeCheck.setEnabled(false);
      m_executeCodeCheck.setText(EXECUTE_CODE);
      gridData = new GridData();
      gridData.horizontalSpan = 2;
      m_executeCodeCheck.setLayoutData(gridData);

      m_executeCodeText = new Text(m_onLoadGroup, SWT.BORDER | SWT.MULTI);
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
