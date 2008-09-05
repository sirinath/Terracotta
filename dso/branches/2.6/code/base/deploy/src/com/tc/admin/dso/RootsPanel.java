/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTree;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class RootsPanel extends XContainer {
  private ConnectionContext m_cc;
  private XTree             m_tree;

  public RootsPanel(ConnectionContext cc, DSORoot[] roots) {
    super(new BorderLayout());

    m_tree = new XTree();
    m_tree.setShowsRootHandles(true);
    add(new JScrollPane(m_tree), BorderLayout.CENTER);

    setup(cc, roots);
  }

  public void setup(ConnectionContext cc, DSORoot[] roots) {
    m_cc = cc;
    setRoots(roots);
  }

  public void setRoots(DSORoot[] roots) {
    m_tree.setModel(new RootTreeModel(m_cc, roots));
    m_tree.revalidate();
    m_tree.repaint();
  }

  public void clearModel() {
    m_tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
    m_tree.revalidate();
    m_tree.repaint();
  }

  public void refresh() {
    ((RootTreeModel) m_tree.getModel()).refresh();
  }

  public void add(DSORoot root) {
    ((RootTreeModel) m_tree.getModel()).add(root);
    m_tree.revalidate();
    m_tree.repaint();
  }

  public void tearDown() {
    super.tearDown();
    m_cc = null;
    m_tree = null;
  }
}
