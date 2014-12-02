/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextPane;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.Icon;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

public class ClusteredHeapNode extends ComponentNode implements HyperlinkListener {
  protected final IAdminClientContext adminClientContext;
  protected final IClusterModel       clusterModel;

  protected XScrollPane               clusteredHeapPanel;

  public ClusteredHeapNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(adminClientContext.getString("dso.heap"));

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    add(createRootsNode());
    add(new ClassesNode(adminClientContext, clusterModel));
  }

  protected RootsNode createRootsNode() {
    return new RootsNode(adminClientContext, clusterModel);
  }

  protected ClassesNode createClassesNode() {
    return new ClassesNode(adminClientContext, clusterModel);
  }

  @Override
  public Component getComponent() {
    if (clusteredHeapPanel == null) {
      XTextPane textPane = new XTextPane();
      clusteredHeapPanel = new XScrollPane(textPane);
      try {
        textPane.setPage(getClass().getResource("ClusteredHeapIntro.html"));
      } catch (Exception e) {
        adminClientContext.log(e);
      }
      textPane.setEditable(false);
      textPane.addHyperlinkListener(this);
    }
    return clusteredHeapPanel;
  }

  public void hyperlinkUpdate(HyperlinkEvent e) {
    XTextPane textPane = (XTextPane) e.getSource();
    HyperlinkEvent.EventType type = e.getEventType();
    Element elem = e.getSourceElement();

    if (elem == null || type == HyperlinkEvent.EventType.ENTERED || type == HyperlinkEvent.EventType.EXITED) { return; }

    if (textPane.getCursor().getType() != Cursor.WAIT_CURSOR) {
      AttributeSet anchor = (AttributeSet) elem.getAttributes().getAttribute(HTML.Tag.A);
      String action = (String) anchor.getAttribute(HTML.Attribute.HREF);
      adminClientContext.getAdminClientController().selectNode(this, action);
    }
  }

  @Override
  public Icon getIcon() {
    return DSOHelper.getHelper().getClusteredHeapIcon();
  }

  @Override
  public void tearDown() {
    if (clusteredHeapPanel != null) {
      XTextPane textPane = (XTextPane) clusteredHeapPanel.getViewport().getView();
      textPane.removeHyperlinkListener(this);
    }
    super.tearDown();
  }
}
