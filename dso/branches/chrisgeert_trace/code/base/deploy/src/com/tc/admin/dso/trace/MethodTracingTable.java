/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.dso.trace;

import com.tc.admin.common.XObjectTable;
import com.tc.admin.dso.locks.ServerLockTableModel;
import com.tc.admin.dso.locks.StatValueRenderer;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


public class MethodTracingTable extends XObjectTable {
  private TableCellRenderer                     fTableColumnHeaderRenderer = new LockTableHeaderRenderer();

  private static final DefaultTableCellRenderer HEADER_RENDERER            = new DefaultTableCellRenderer();

  public MethodTracingTable() {
    super();
    setDefaultRenderer(Long.class, new StatValueRenderer());
  }

  class LockTableHeaderRenderer extends TableColumnRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      ServerLockTableModel model = (ServerLockTableModel) table.getModel();
      ((JComponent) c).setToolTipText(model.columnTip(column));
      return c;
    }
  }

  public void setModel(TableModel model) {
    super.setModel(model);
    TableColumnModel colModel = getColumnModel();
    for (int i = 0; i < colModel.getColumnCount(); i++) {
      colModel.getColumn(i).setHeaderRenderer(fTableColumnHeaderRenderer);
    }
  }

  public void addColumn(TableColumn aColumn) {
    super.addColumn(aColumn);
    aColumn.setHeaderRenderer(HEADER_RENDERER);
  }
}
