/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public final class SWTUtil {

  private SWTUtil() {
  // cannot instantiate
  }

  public static int textColumnsToPixels(Text text, int columns) {
    GC gc = new GC(text);
    FontMetrics fm = gc.getFontMetrics();
    int width = columns * fm.getAverageCharWidth();
    int height = fm.getHeight();
    gc.dispose();
    return text.computeSize(width, height).x;
  }

  public static void applyDefaultButtonSize(Button button) {
    Point preferredSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
    Point hint = Geometry.max(LayoutConstants.getMinButtonSize(), preferredSize);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(hint).applyTo(button);
  }

  public static void placeDialogInCenter(Shell parent, Shell shell) {
    Rectangle parentSize = parent.getBounds();
    Rectangle mySize = shell.getBounds();
    int locationX, locationY;
    locationX = (parentSize.width - mySize.width) / 2 + parentSize.x;
    locationY = (parentSize.height - mySize.height) / 2 + parentSize.y;
    shell.setLocation(new Point(locationX, locationY));
  }

  public static void makeTableColumnsResizeEqualWidth(final Composite tablePanel, final Table table) {
    tablePanel.addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        Rectangle area = tablePanel.getClientArea();
        Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int width = area.width - 2 * table.getBorderWidth();
        if (preferredSize.y > area.height + table.getHeaderHeight()) {
          Point vBarSize = table.getVerticalBar().getSize();
          width -= vBarSize.x;
        }
        TableColumn[] columns = table.getColumns();
        int colWidth = width / columns.length;
        for (int i = 0; i < columns.length; i++) {
          columns[i].setWidth(colWidth);
        }
      }
    });
  }

  public static void makeTableColumnsResizeWeightedWidth(final Composite tablePanel, final Table table,
                                                         final int[] columnWeights) {
    int weight = 0;
    for (int i = 0; i < columnWeights.length; i++) {
      weight += columnWeights[i];
    }
    final int totalWeight = weight;
    tablePanel.addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        Rectangle area = tablePanel.getClientArea();
        Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int width = area.width - 2 * table.getBorderWidth();
        if (preferredSize.y > area.height + table.getHeaderHeight()) {
          Point vBarSize = table.getVerticalBar().getSize();
          width -= vBarSize.x;
        }
        TableColumn[] columns = table.getColumns();
        int widthUnit = width / totalWeight;
        for (int i = 0; i < columns.length; i++) {
          columns[i].setWidth(widthUnit * columnWeights[i]);
        }
      }
    });
  }

  public static void makeTableColumnsEditable(final Table table, final int[] indices) {
    final TableEditor editor = new TableEditor(table);
    editor.horizontalAlignment = SWT.LEFT;
    editor.grabHorizontal = true;
    table.addListener(SWT.MouseDown, new Listener() {
      public void handleEvent(Event event) {
        Rectangle clientArea = table.getClientArea();
        Point pt = new Point(event.x, event.y);
        int index = table.getTopIndex();
        while (index < table.getItemCount()) {
          boolean visible = false;
          final TableItem item = table.getItem(index);
          for (int i = 0; i < indices.length; i++) {
            Rectangle rect = item.getBounds(i);
            if (rect.contains(pt)) {
              final int column = indices[i];
              final Text text = new Text(table, SWT.NONE);
              Listener textListener = new Listener() {
                public void handleEvent(final Event e) {
                  switch (e.type) {
                    case SWT.FocusOut:
                      item.setText(column, text.getText());
                      text.dispose();
                      break;
                    case SWT.Traverse:
                      switch (e.detail) {
                        case SWT.TRAVERSE_RETURN:
                          item.setText(column, text.getText());
                          // FALL THROUGH
                        case SWT.TRAVERSE_ESCAPE:
                          text.dispose();
                          e.doit = false;
                      }
                      break;
                  }
                }
              };
              text.addListener(SWT.FocusOut, textListener);
              text.addListener(SWT.Traverse, textListener);
              editor.setEditor(text, item, i);
              text.setText(item.getText(i));
              text.selectAll();
              text.setFocus();
              return;
            }
            if (!visible && rect.intersects(clientArea)) {
              visible = true;
            }
          }
          if (!visible) return;
          index++;
        }
      }
    });
  }
}
