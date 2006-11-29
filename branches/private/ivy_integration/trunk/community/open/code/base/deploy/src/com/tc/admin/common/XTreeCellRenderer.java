package com.tc.admin.common;

import org.dijon.DefaultTreeCellRenderer;

import java.awt.Component;

import javax.swing.JTree;

public class XTreeCellRenderer extends DefaultTreeCellRenderer {
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                boolean expanded, boolean leaf, int row,
                                                boolean focused)
  {
    Component comp =
      super.getTreeCellRendererComponent(
        tree, value, sel, expanded, leaf, row, focused);

    if(value instanceof XTreeNode) {
      setIcon(((XTreeNode)value).getIcon());
    }

    return comp;
  }
}
