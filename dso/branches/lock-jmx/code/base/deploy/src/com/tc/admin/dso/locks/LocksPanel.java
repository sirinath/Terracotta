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
import org.dijon.ToggleButton;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;

import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

public class LocksPanel extends XContainer implements NotificationListener {
  private ConnectionContext          fConnectionContext;
  private LockStatisticsMonitorMBean fLockStats;
  private ToggleButton               fEnableButton;
  private ToggleButton               fDisableButton;
  private boolean                    fLocksPanelEnabled;
  private Spinner                    fTraceDepthSpinner;
  private ChangeListener             fTraceDepthSpinnerChangeListener;
  private Timer                      fTraceDepthChangeTimer;
  private int                        fLastTraceDepth;
  private Button                     fRefreshButton;
  private LockTreeTable              fTreeTable;
  private LockTreeTableModel         fTreeTableModel;
  private LockTreeTableModel         fEmptyTreeTableModel;
  private XObjectTable               fServerLocksTable;
  private ServerLockTableModel       fServerLockTableModel;
  private TextArea                   fTraceText;
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
    fLastTraceDepth = fLockStats.getTraceDepth();

    fEnableButton = (ToggleButton) findComponent("EnableButton");
    fEnableButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        toggleLocksPanelEnabled();
      }
    });
    fDisableButton = (ToggleButton) findComponent("DisableButton");
    fDisableButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        toggleLocksPanelEnabled();
      }
    });
    fTraceDepthSpinner = (Spinner) findComponent("TraceDepthSpinner");
    ((SpinnerNumberModel) fTraceDepthSpinner.getModel()).setValue(Integer.valueOf(fLastTraceDepth));
    fTraceDepthSpinner.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        testSetTraceDepth();
      }
    });
    fTraceDepthChangeTimer = new Timer(1000, new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        testSetTraceDepth();
      }
    });
    fTraceDepthChangeTimer.setRepeats(false);
    fTraceDepthSpinnerChangeListener = new TraceDepthSpinnerChangeListener();
    fTraceDepthSpinner.addChangeListener(fTraceDepthSpinnerChangeListener);
    fRefreshButton = (Button) findComponent("RefreshButton");
    fRefreshButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        fTreeTableModel.init();
        fTreeTable.sort();
        
        fServerLockTableModel.init();
        fServerLocksTable.sort();
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
        populateTraceText(path);
      }
    });

    fServerLocksTable = (XObjectTable) findComponent("ServerLocksTable");
    
    fTraceText = (TextArea) findComponent("TraceText");
    fConfigLabel = (Label) findComponent("ConfigLabel");
    fConfigText = (TextArea) findComponent("ConfigText");

    setLocksPanelEnabled(fLockStats.isLockStatisticsEnabled());
    
    fConnectionContext = cc;
    try {
      cc.addNotificationListener(L2MBeanNames.LOCK_STATISTICS, this);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  class TraceDepthSpinnerChangeListener implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      fTraceDepthChangeTimer.stop();
      fTraceDepthChangeTimer.start();
    }
  }
  
  private void populateTraceText(Object[] nodePath) {
    String nl = System.getProperty("line.separator");
    fTraceText.setText("");
    if (nodePath != null && nodePath.length > 2) {
      for (int i = 2; i < nodePath.length; i++) {
        fTraceText.append(nodePath[i].toString());
        fTraceText.append(nl);
      }
    }
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
    setTraceDepth(fLastTraceDepth);

    if ((fLocksPanelEnabled = enabled) == true) {
      fTreeTable.setTreeTableModel(fTreeTableModel = createLocksTreeTableModel());
      fServerLocksTable.setModel(fServerLockTableModel = new ServerLockTableModel(fLockStats));
      fEnableButton.setSelected(true);
      fDisableButton.setSelected(false);
    } else {
      fTreeTable.setTreeTableModel(fEmptyTreeTableModel);
      fServerLocksTable.setModel(ServerLockTableModel.EMPTY_MODEL);
      fConfigText.setText("");
      fEnableButton.setSelected(false);
      fDisableButton.setSelected(true);
    }
    fTraceDepthSpinner.setEnabled(enabled);
    fTraceDepthSpinner.getEditor().setEnabled(enabled);
    
    fTraceText.setText("");
    fConfigLabel.setText("");
  }

  private int getSpinnerValue(JSpinner spinner) {
    try {
      spinner.commitEdit();
      spinner.setForeground(null);
    } catch (ParseException pe) {
      // Edited value is invalid, spinner.getValue() will return
      // the last valid value, you could revert the spinner to show that:
      JComponent editor = spinner.getEditor();
      if (editor instanceof JSpinner.DefaultEditor) {
        ((JSpinner.DefaultEditor) editor).getTextField().setValue(spinner.getValue());
      }
      spinner.setForeground(Color.red);
    }
    return ((SpinnerNumberModel) spinner.getModel()).getNumber().intValue();
  }

  private void testSetTraceDepth() {
    int newTraceDepth = getTraceDepth();
    if(newTraceDepth != fLastTraceDepth) {
      setTraceDepth(newTraceDepth);
    }
  }

  private void setTraceDepth(int traceDepth) {
    fLockStats.setLockStatisticsConfig(fLastTraceDepth = traceDepth, 1);
  }
  
  private int getTraceDepth() {
    return getSpinnerValue(fTraceDepthSpinner);
  }

  private LockTreeTableModel createLocksTreeTableModel() {
    return new LockTreeTableModel(fLockStats);
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();
    if(type.equals(LockStatisticsMonitorMBean.TRACE_DEPTH)) {
      int newTraceDepth = fLockStats.getTraceDepth();
      if(fLastTraceDepth != newTraceDepth) {
        fLastTraceDepth = newTraceDepth;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ((SpinnerNumberModel) fTraceDepthSpinner.getModel()).setValue(Integer.valueOf(fLastTraceDepth));
          }
        });
      }
    } else if(type.equals(LockStatisticsMonitorMBean.TRACES_ENABLED)) {
      final boolean enabled = fLockStats.isLockStatisticsEnabled();
      if(enabled != fRefreshButton.isEnabled()) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            setLocksPanelEnabled(enabled);
          }
        });
      }
    }
  }
  
  public void tearDown() {
    super.tearDown();
    try {
      fConnectionContext.addNotificationListener(L2MBeanNames.LOCK_STATISTICS, this);
    } catch(Exception e) {
      // ignore
    }
  }
}
