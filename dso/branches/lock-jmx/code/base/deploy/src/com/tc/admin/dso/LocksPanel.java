/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.ContainerResource;
import org.dijon.Spinner;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.LockElementWrapper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XComboBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XPopupListener;
import com.tc.management.L2LockStatsManagerImpl.LockStat;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.objectserver.lockmanager.api.LockHolder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.management.MBeanServerInvocationHandler;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

public class LocksPanel extends XContainer {
  private XComboBox                  m_typeCombo;
  private Spinner                    m_countSpinner;
  private XButton                    m_refreshButton;
  private XObjectTable               m_lockTable;
  private XObjectTableModel          m_lockTableModel;
  private LockStatisticsMonitorMBean lockStatsMBean;
  private XPopupListener             m_popupListener;

  private final int                  DEFAULT_LOCK_COUNT        = 20;

  private final String               HOLDER_TYPE_HELD          = "Held";
  private final String               HOLDER_TYPE_WAITING_LOCKS = "WaitingLocks";

  private final String               STAT_TYPE_REQUESTED       = "Requested";
  private final String               STAT_TYPE_CONTENDED_LOCKS = "ContendedLocks";
  private final String               STAT_TYPE_LOCK_HOPS       = "LockHops";

  private final String[]             ALL_TYPES                 = { HOLDER_TYPE_HELD, HOLDER_TYPE_WAITING_LOCKS,
      STAT_TYPE_REQUESTED, STAT_TYPE_CONTENDED_LOCKS, STAT_TYPE_LOCK_HOPS };

  private static final String        REFRESH                   = "Refresh";

  public LocksPanel(ConnectionContext cc) {
    super();

    AdminClientContext cntx = AdminClient.getContext();

    load((ContainerResource) cntx.topRes.getComponent("LocksPanel"));

    lockStatsMBean = (LockStatisticsMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, L2MBeanNames.LOCK_STATISTICS, LockStatisticsMonitorMBean.class, false);

    m_typeCombo = (XComboBox) findComponent("TypeCombo");
    m_typeCombo.setModel(new DefaultComboBoxModel(ALL_TYPES));
    m_typeCombo.setSelectedIndex(0);
    m_typeCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setType((String) m_typeCombo.getSelectedItem());
      }
    });

    m_countSpinner = (Spinner) findComponent("CountSpinner");
    m_countSpinner.setValue(Integer.valueOf(DEFAULT_LOCK_COUNT));

    RefreshAction refreshAction = new RefreshAction();
    m_refreshButton = (XButton) findComponent("RefreshButton");
    m_refreshButton.addActionListener(refreshAction);

    m_lockTable = (XObjectTable) findComponent("LockTable");
    m_lockTable.setSortColumn(-1);
    m_lockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setType((String) m_typeCombo.getSelectedItem());

    m_popupListener = new XPopupListener(m_lockTable);
    
    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true);
    getActionMap().put(REFRESH, refreshAction);
    getInputMap().put(ks, REFRESH);

    JPopupMenu popup = new JPopupMenu();
    popup.add(new JMenuItem(new EnableStatsAction()));
    popup.add(new JMenuItem(new EnableAllStatsAction()));
    popup.add(new JMenuItem(new DisableStatsAction()));
    popup.add(new JMenuItem(new DisableAllStatsAction()));
    popup.addSeparator();
    popup.add(new JMenuItem(new DisplayStackTraceAction()));
    popup.addSeparator();
    popup.add(new JMenuItem(refreshAction));
    m_popupListener.setPopupMenu(popup);
    
    updateTableModel();
  }

  private void setType(String type) {
    if (type == HOLDER_TYPE_HELD || type == HOLDER_TYPE_WAITING_LOCKS) {
      m_lockTableModel = new LockHolderTableModel(type);
    } else {
      m_lockTableModel = new LockStatTableModel(type);
    }
    m_lockTable.setModel(m_lockTableModel);
  }

  private int getMaxLocks() {
    Object o = m_countSpinner.getValue();
    if (o instanceof Integer) { return ((Integer) o).intValue(); }
    return DEFAULT_LOCK_COUNT;
  }

  public class RefreshAction extends XAbstractAction {
    RefreshAction() {
      super("Refresh");
    }
    
    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  public class EnableStatsAction extends XAbstractAction {
    EnableStatsAction() {
      super("Enable stats");
    }
    
    public void actionPerformed(ActionEvent ae) {
      int row = m_lockTable.getSelectedRow();
      if(row != -1) {
        LockElementWrapper wrapper = (LockElementWrapper)m_lockTableModel.getObjectAt(row);
        String id = wrapper.getLockID();
        lockStatsMBean.enableClientStat(id);
        AdminClient.getContext().log("Enabled stats for '"+id+"'");
      }
    }
  }

  public class EnableAllStatsAction extends XAbstractAction {
    EnableAllStatsAction() {
      super("Enable all stats");
    }
    
    public void actionPerformed(ActionEvent ae) {
      int count = m_lockTableModel.getRowCount();
      for(int i = 0; i < count; i++) {
        LockElementWrapper wrapper = (LockElementWrapper)m_lockTableModel.getObjectAt(i);
        String id = wrapper.getLockID();
        lockStatsMBean.enableClientStat(id);
      }
      AdminClient.getContext().log("Enabled all stats");
    }
  }

  public class DisableStatsAction extends XAbstractAction {
    DisableStatsAction() {
      super("Disable stats");
    }
    
    public void actionPerformed(ActionEvent ae) {
      int row = m_lockTable.getSelectedRow();
      if(row != -1) {
        LockElementWrapper wrapper = (LockElementWrapper)m_lockTableModel.getObjectAt(row);
        String id = wrapper.getLockID();
        lockStatsMBean.disableClientStat(id);
        AdminClient.getContext().log("Disabled stats for '"+id+"'");
      }
    }
  }

  public class DisableAllStatsAction extends XAbstractAction {
    DisableAllStatsAction() {
      super("Disable all stats");
    }
    
    public void actionPerformed(ActionEvent ae) {
      int count = m_lockTableModel.getRowCount();
      for(int i = 0; i < count; i++) {
        LockElementWrapper wrapper = (LockElementWrapper)m_lockTableModel.getObjectAt(i);
        String id = wrapper.getLockID();
        lockStatsMBean.disableClientStat(id);
      }
      AdminClient.getContext().log("Disabled all stats");
    }
  }

  public class DisplayStackTraceAction extends XAbstractAction {
    DisplayStackTraceAction() {
      super("Display stack trace");
    }
    
    public void actionPerformed(ActionEvent ae) {
      int row = m_lockTable.getSelectedRow();
      if(row != -1) {
        LockElementWrapper wrapper = (LockElementWrapper)m_lockTableModel.getObjectAt(row);
        String id = wrapper.getLockID();
        Collection c = lockStatsMBean.getStackTraces(id);
        if(c.isEmpty()) {
          AdminClient.getContext().log("No stack traces for '"+id+"'");
          return;
        }
        Iterator iter = c.iterator();
        while(iter.hasNext()) {
          AdminClient.getContext().log(iter.next().toString());
        }
      }
    }
  }

  private void updateTableModel() {
    setType((String) m_typeCombo.getSelectedItem());
  }

  public void refresh() {
    AdminClientContext acc = AdminClient.getContext();

    acc.controller.setStatus(acc.getMessage("dso.locks.refreshing"));
    updateTableModel();
    acc.controller.clearStatus();
  }

  static final String[] LOCK_HOLDER_ATTRS = { "LockID", "LockLevel", "NodeID", "ChannelAddr", "ThreadID",
      "WaitTimeInMillis", "HeldTimeInMillis" };

  class LockHolderTableModel extends XObjectTableModel {
    String lockType;

    LockHolderTableModel(String lockType) {
      super(LockHolderWrapper.class, LOCK_HOLDER_ATTRS, LOCK_HOLDER_ATTRS);
      this.lockType = lockType;
      init();
    }

    private Collection getCollection() {
      try {
        Method m = lockStatsMBean.getClass().getMethod("getTop" + lockType, new Class[] { Integer.TYPE });
        return (Collection) m.invoke(lockStatsMBean, new Object[] { Integer.valueOf(getMaxLocks()) });
      } catch (Exception e) {
        e.printStackTrace();
      }
      return Collections.EMPTY_LIST;
    }

    private void init() {
      try {
        set(wrap(getCollection()));
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    Object[] wrap(Collection c) {
      Iterator i = c.iterator();
      ArrayList<LockHolderWrapper> l = new ArrayList<LockHolderWrapper>();
      while (i.hasNext()) {
        l.add(new LockHolderWrapper((LockHolder) i.next()));
      }
      return l.toArray(new LockHolderWrapper[0]);
    }
  }

  static final String[] LOCK_STAT_ATTRS = { "LockID", "NumOfLockRequested", "NumOfLockReleased",
      "NumOfPendingRequests", "NumOfPendingWaiters", "NumOfPingPongRequests" };

  static final String[] LOCK_STAT_COLS  = { "LockID", "LockRequested", "LockReleased", "PendingRequests",
      "PendingWaiters", "PingPongRequests" };

  class LockStatTableModel extends XObjectTableModel {
    String lockType;

    LockStatTableModel(String lockType) {
      super(LockStatWrapper.class, LOCK_STAT_ATTRS, LOCK_STAT_COLS);
      this.lockType = lockType;
      init();
    }

    private Collection getCollection() {
      try {
        Method m = lockStatsMBean.getClass().getMethod("getTop" + lockType, new Class[] { Integer.TYPE });
        return (Collection) m.invoke(lockStatsMBean, new Object[] { Integer.valueOf(getMaxLocks()) });
      } catch (Exception e) {
        e.printStackTrace();
      }
      return Collections.EMPTY_LIST;
    }

    private void init() {
      try {
        set(wrap(getCollection()));
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    Object[] wrap(Collection c) {
      Iterator i = c.iterator();
      ArrayList<LockStatWrapper> l = new ArrayList<LockStatWrapper>();
      while (i.hasNext()) {
        l.add(new LockStatWrapper((LockStat) i.next()));
      }
      return l.toArray(new LockStatWrapper[0]);
    }
  }
}
