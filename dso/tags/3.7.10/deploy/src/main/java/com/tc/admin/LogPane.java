/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.TextComponentHelper;
import com.tc.admin.common.XTextPane;

import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

public class LogPane extends XTextPane implements HierarchyListener {
  private boolean             autoScroll = true;
  private final StringBuilder buffer     = new StringBuilder();

  public LogPane() {
    super();
    setEditable(false);
    setFont(Font.getFont("monospaced-plain-12"));
    addHierarchyListener(this);
  }

  @Override
  protected TextComponentHelper createHelper() {
    return new LogPaneHelper();
  }

  class LogPaneHelper extends TextComponentHelper {
    LogPaneHelper() {
      super(LogPane.this);
    }

    @Override
    public JPopupMenu createPopup() {
      JPopupMenu popup = super.createPopup();
      addClearAction(popup);
      return popup;
    }
  }

  public void setAutoScroll(boolean autoScroll) {
    if ((this.autoScroll = autoScroll) == true) {
      if (getDocument().getLength() > 0) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            // Would like to just use setCaretPosition here but the if
            // the model isn't changed, that will have no affect.
            // We're just adding a space, removing it, and then
            // setting the caret position. Bummer.
            int end = getDocument().getLength();
            try {
              getDocument().insertString(end, " ", null);
              getDocument().remove(end, 1);
              setCaretPosition(end - 1);
            } catch (BadLocationException e) {/**/
            }
          }
        });
      }
    }
  }

  public boolean getAutoScroll() {
    return autoScroll;
  }

  private void testDrainBuffer() {
    synchronized (buffer) {
      if (buffer.length() > 0) {
        append(buffer.toString());
        buffer.setLength(0);
      }
    }
  }

  public void log(String s) {
    if (isShowing()) {
      boolean doAutoScroll = getAutoScroll();
      testDrainBuffer();
      append(s);
      if (doAutoScroll) {
        setCaretPosition(getDocument().getLength() - 1);
      }
    } else {
      synchronized (buffer) {
        buffer.append(s);
      }
    }
  }

  public void hierarchyChanged(HierarchyEvent e) {
    long flags = e.getChangeFlags();
    if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (isShowing()) {
        testDrainBuffer();
      }
    }
  }
}
