/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.dso.trace;

import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.model.IClient;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

public class ClientMethodTracingPanel extends XContainer {
  private IAdminClientContext           adminClientContext;
  private MethodTracesGetter            currentTraceResultsGetter;
  private XObjectTable                  methodTracingTable;
  private MethodTracingTableModel       methodTracingTableModel;

  private static final int              REFRESH_TIMEOUT_SECONDS    = Integer.MAX_VALUE;

  public ClientMethodTracingPanel(IClient client, IAdminClientContext adminClientContext) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;

    TableMouseListener tableMouseListener = new TableMouseListener();

    /** Server * */
    XContainer methodTracingPanel = new XContainer(new BorderLayout());
    methodTracingTable = new MethodTracingTable();
    methodTracingTableModel = new MethodTracingTableModel(adminClientContext);
    methodTracingTable.setModel(methodTracingTableModel);
    methodTracingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JPopupMenu serverLocksPopup = new JPopupMenu();
    methodTracingTable.setPopupMenu(serverLocksPopup);
    methodTracingTable.addMouseListener(tableMouseListener);
    methodTracingPanel.add(new XScrollPane(methodTracingTable));

    add(methodTracingPanel, BorderLayout.CENTER);
  }

  private class TableMouseListener extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int row = table.rowAtPoint(e.getPoint());
      if (row != -1) {
        table.setRowSelectionInterval(row, row);
      }
    }
  }

  void refresh() {
    currentTraceResultsGetter = new MethodTracesGetter();
    adminClientContext.submit(currentTraceResultsGetter);
  }

  class MethodTracesGetter extends BasicWorker<Collection<MethodTraceResult>> {

    MethodTracesGetter() {
      super(new Callable<Collection<MethodTraceResult>>() {
        public Collection<MethodTraceResult> call() throws Exception {
          //Do code to get stuff here
          return Collections.<MethodTraceResult>emptySet();
        }
      }, REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected void finished() {
      currentTraceResultsGetter = null;

      Exception e = getException();
      if (e != null) {
        String msg;
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (rootCause instanceof IOException) {
          return;
        } else if (rootCause instanceof TimeoutException) {
          msg = "timed-out after '" + REFRESH_TIMEOUT_SECONDS + "' seconds";
        } else if (rootCause instanceof CancellationException) {
          msg = "cancelled";
        } else {
          msg = rootCause.getMessage();
        }
        adminClientContext.log(new Date() + ": Method tracing: failed to refresh: " + msg);
      } else {
        Collection<MethodTraceResult> traceResults = getResult();

        methodTracingTable.setModel(methodTracingTableModel = new MethodTracingTableModel(adminClientContext, traceResults));
        methodTracingTable.sort();
      }
    }
  }

  @Override
  public synchronized void tearDown() {
    super.tearDown();

    adminClientContext = null;
    methodTracingTable = null;
    methodTracingTableModel = null;
  }
}