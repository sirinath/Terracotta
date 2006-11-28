package com.tc.admin.common;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

/**
 * This is a TreeCellRenderer that can delegate to either a node-specific
 * renderer or to the default TreeCellRenderer.
 */

public class XTreeCellRendererDelegate implements TreeCellRenderer {
  protected XTreeCellRenderer m_defaultRenderer;
  
  public XTreeCellRendererDelegate() {
    m_defaultRenderer = new XTreeCellRenderer();
  }
  
  protected TreeCellRenderer getNodeRenderer(Object value) {
    TreeCellRenderer nodeRenderer = null;
    
    if(value instanceof XTreeNode) {
      nodeRenderer = ((XTreeNode)value).getRenderer();
    }
    
    return nodeRenderer != null ? nodeRenderer : m_defaultRenderer;
  }
  
  public Component getTreeCellRendererComponent(
    JTree   tree, 
    Object  value,
    boolean sel,
    boolean expanded,
    boolean leaf,
    int     row,
    boolean focused)
  {
    return 
      getNodeRenderer(value).getTreeCellRendererComponent(
        tree, value, sel, expanded, leaf, row, focused);
  }
}
