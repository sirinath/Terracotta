/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.Label;
import org.dijon.ScrollPane;
import org.dijon.Spinner;
import org.dijon.TextArea;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;

import javax.management.MBeanServerInvocationHandler;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

public class LocksPanel extends XContainer {
  private LockStatisticsMonitorMBean fLockStats;
  private Button                     fEnableButton;
  private boolean                    fLocksPanelEnabled;
  private Spinner                    fTraceDepthSpinner;
  private Button                     fTraceDepthButton;
  private Button                     fRefreshButton;
  private LockTreeTable              fTreeTable;
  private LockTreeTableModel         fTreeTableModel;
  private LockTreeTableModel         fEmptyTreeTableModel;
  private XObjectTable               fTraceTable;
  private ListSelectionListener      fTraceTableSelectionListener;
  private Label                      fConfigLabel;
  private TextArea                   fConfigText;

  public LocksPanel(ConnectionContext cc) {
    super();

    AdminClientContext cntx = AdminClient.getContext();

    load((ContainerResource) cntx.topRes.getComponent("LocksPanel"));

    fLockStats = (LockStatisticsMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, L2MBeanNames.LOCK_STATISTICS, LockStatisticsMonitorMBean.class, false);

    // We do this to force an early error if the server we're connecting to is old and doesn't
    // have the LockStatisticsMonitorMBean. DSONode catches the error and doesn't display the LocksNode.
    int traceDepth = fLockStats.getTraceDepth();

    fEnableButton = (Button) findComponent("EnableButton");
    fEnableButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        toggleLocksPanelEnabled();
      }
    });
    fTraceDepthSpinner = (Spinner) findComponent("TraceDepthSpinner");
    ((SpinnerNumberModel) fTraceDepthSpinner.getModel()).setValue(Integer.valueOf(traceDepth));
    fTraceDepthButton = (Button) findComponent("TraceDepthButton");
    fTraceDepthButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        fLockStats.setLockStatisticsConfig(getTraceDepth(), 1);
      }
    });
    fRefreshButton = (Button) findComponent("RefreshButton");
    fRefreshButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        fTreeTableModel.init();
        fTreeTable.sort();
      }
    });
    fTreeTableModel = createLocksTreeTableModel();
    fEmptyTreeTableModel = new EmptyLockTreeTableModel(fLockStats);
    fTreeTable = new LockTreeTable(fEmptyTreeTableModel, cntx.prefs.node("LockTreeTable"));
    ScrollPane tableScroller = (ScrollPane) findComponent("TableScroller");
    tableScroller.setViewportView(fTreeTable);
    fTreeTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    fTreeTable.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        TreePath treePath = e.getNewLeadSelectionPath();
        if (treePath == null) return;
        Object[] path = treePath.getPath();
        LockSpecNode lockSpecNode = (LockSpecNode) path[1];
        Object last = path[path.length - 1];
        String text = "";
        if (last instanceof LockTraceElementNode) {
          LockTraceElementNode lastNode = (LockTraceElementNode) last;
          text = lastNode.getConfigText();
        }
        fConfigText.setText(text);
        fConfigLabel.setText(lockSpecNode.toString());
        populateTraceTable(path);
      }
    });

    fTraceTable = (XObjectTable) findComponent("TraceTable");
    fTraceTable.setModel(new XObjectTableModel(LockTraceElementNode.class, new String[] { "Name" },
                                               new String[] { "Trace" }));
    fTraceTableSelectionListener = new TraceTableSelectionListener();
    fTraceTable.getSelectionModel().addListSelectionListener(fTraceTableSelectionListener);
    fConfigLabel = (Label) findComponent("ConfigLabel");
    fConfigText = (TextArea) findComponent("ConfigText");

    setLocksPanelEnabled(fLockStats.isLockStatisticsEnabled());
  }

  class TraceTableSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting()) return;
      JTree tree = fTreeTable.getTree();
      TreePath treePath = tree.getLeadSelectionPath();
      if (treePath == null) return;
      XObjectTableModel model = (XObjectTableModel) fTraceTable.getModel();
      ArrayList<Object> list = new ArrayList<Object>();
      list.add(treePath.getPathComponent(0));
      list.add(treePath.getPathComponent(1));
      int selectedRow = fTraceTable.getSelectedRow();
      for (int i = 0; i <= selectedRow; i++) {
        list.add(model.getObjectAt(i));
      }
      tree.setSelectionPath(new TreePath(list.toArray(new Object[0])));
    }
  }

  private void populateTraceTable(Object[] nodePath) {
    fTraceTable.getSelectionModel().removeListSelectionListener(fTraceTableSelectionListener);

    XObjectTableModel model = (XObjectTableModel) fTraceTable.getModel();
    model.clear();
    if (nodePath != null && nodePath.length > 2) {
      for (int i = 2; i < nodePath.length; i++) {
        model.add(nodePath[i]);
      }
    }
    model.fireTableDataChanged();

    int last = model.getRowCount() - 1;
    if (last >= 0) {
      fTraceTable.setRowSelectionInterval(0, last);
      Action action = TransferHandler.getCopyAction();
      action.actionPerformed(new ActionEvent(fTraceTable, ActionEvent.ACTION_PERFORMED, (String) action
          .getValue(Action.NAME), EventQueue.getMostRecentEventTime(), 0));
      fTraceTable.setRowSelectionInterval(last, last);
    }

    fTraceTable.getSelectionModel().addListSelectionListener(fTraceTableSelectionListener);
  }

  private void toggleLocksPanelEnabled() {
    setLocksPanelEnabled(fLocksPanelEnabled ? false : true);
  }

  private void setLocksPanelEnabled(boolean enabled) {
    fTreeTable.setEnabled(enabled);
    fConfigLabel.setEnabled(enabled);
    fConfigText.setEnabled(enabled);
    fRefreshButton.setEnabled(enabled);
    fLockStats.setLockStatisticsEnabled(enabled);

    String enableButtonText;
    if ((fLocksPanelEnabled = enabled) == true) {
      fTreeTable.setTreeTableModel(fTreeTableModel = createLocksTreeTableModel());
      enableButtonText = "Disable Lock Tracing";
    } else {
      fTreeTable.setTreeTableModel(fEmptyTreeTableModel);
      fConfigText.setText("");
      enableButtonText = "Enable Lock Tracing";
    }
    fConfigLabel.setText("");
    fEnableButton.setText(enableButtonText);
  }

  private int getSpinnerValue(JSpinner spinner) {
    try {
      spinner.commitEdit();
    } catch (ParseException pe) {
      // Edited value is invalid, spinner.getValue() will return
      // the last valid value, you could revert the spinner to show that:
      JComponent editor = spinner.getEditor();
      if (editor instanceof JSpinner.DefaultEditor) {
        ((JSpinner.DefaultEditor) editor).getTextField().setValue(spinner.getValue());
      }
    }
    return ((SpinnerNumberModel) spinner.getModel()).getNumber().intValue();
  }

  private int getTraceDepth() {
    return getSpinnerValue(fTraceDepthSpinner);
  }

  private LockTreeTableModel createLocksTreeTableModel() {
    return new LockTreeTableModel(fLockStats);
  }
}
