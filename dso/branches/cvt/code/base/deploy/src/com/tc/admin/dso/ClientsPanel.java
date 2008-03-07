/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.category.DefaultCategoryDataset;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XContainer;
import com.tc.stats.counter.Counter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

public class ClientsPanel extends XContainer {
  private ConnectionContext      m_cc;
  private ClientsNode            m_clientsNode;
  private ClientsTable           m_table;
  private ChartPanel             m_chartPanel;
  private DefaultCategoryDataset m_dataSet;
  private Timer                  m_timer;
  private boolean                m_shouldAutoStart;

  private static final String    PENDING_REQUESTS_SERIES_NAME = "Pending Requests";

  public ClientsPanel(ConnectionContext cc, ClientsNode clientsNode, DSOClient[] clients) {
    super(new BorderLayout());
    m_cc = cc;
    m_clientsNode = clientsNode;
    add(new JScrollPane(m_table = new ClientsTable(clients)));
    initAllPendingRequestsGraph();
    m_timer = new Timer(2000, new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        try {
          Map map = DSOHelper.getHelper().getAllPendingRequests(m_cc);
          m_dataSet.clear();
          if(map != null) {
            Iterator iter = map.keySet().iterator();
            while (iter.hasNext()) {
              String key = (String) iter.next();
              Counter count = (Counter) map.get(key);
              m_dataSet.addValue(count.getValue(), PENDING_REQUESTS_SERIES_NAME, key);
            }
            m_timer.start();
          }
        } catch (Exception e) {
          /* JMX Connection has probably been dropped.  Wait for tearDown to start timer. */
        }
      }
    });
  }

  private void initAllPendingRequestsGraph() {
    m_dataSet = new DefaultCategoryDataset();
    JFreeChart chart = ChartFactory.createBarChart("", "", "", m_dataSet, PlotOrientation.VERTICAL, true, true, false);
    CategoryPlot plot = (CategoryPlot) chart.getPlot();

    // set the range axis to display integers only...
    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    rangeAxis.setRangeType(RangeType.POSITIVE);
    rangeAxis.setAutoRangeMinimumSize(10.0);

    // disable bar outlines...
    BarRenderer renderer = (BarRenderer) plot.getRenderer();
    renderer.setDrawBarOutline(false);

    // set up gradient paints for series...
    GradientPaint gp = new GradientPaint(0.0f, 0.0f, Color.blue, 0.0f, 0.0f, new Color(0, 0, 64));
    renderer.setSeriesPaint(0, gp);

    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));

    m_chartPanel = new ChartPanel(chart);
    m_chartPanel.setBorder(new TitledBorder("Pending Requests"));
    add(m_chartPanel, BorderLayout.SOUTH);
    m_chartPanel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2) {
          ChartEntity ce = m_chartPanel.getEntityForPoint(e.getX(), e.getY());
          if(ce != null) {
            if(ce instanceof CategoryItemEntity) {
              String clientAddr = ((CategoryItemEntity)ce).getColumnKey().toString();
              m_clientsNode.selectClientNode(clientAddr);
            }
          }
        }
      }
    });
    m_shouldAutoStart = true;
  }

  private void startTimer() {
    m_timer.start();
  }

  private void stopTimer() {
    m_timer.stop();
  }

  public void setClients(DSOClient[] clients) {
    m_table.setClients(clients);
    if (isShowing() && haveAnyClients() && !m_timer.isRunning()) {
      startTimer();
    }
  }

  public void add(DSOClient client) {
    m_table.addClient(client);
    if (isShowing() && !m_timer.isRunning()) {
      startTimer();
    }
  }

  public void remove(DSOClient client) {
    m_table.removeClient(client);
    if (!haveAnyClients() && m_timer.isRunning()) {
      stopTimer();
      m_dataSet.clear();
    }
  }

  private boolean haveAnyClients() {
    return m_table.getModel().getRowCount() > 0;
  }

  public void addNotify() {
    super.addNotify();

    if (haveAnyClients() && m_shouldAutoStart && !m_timer.isRunning()) {
      startTimer();
      m_shouldAutoStart = false;
    }
  }

  public void tearDown() {
    stopTimer();
    m_dataSet.clear();

    super.tearDown();

    m_cc = null;
    m_clientsNode = null;
    m_table = null;
    m_dataSet = null;
    m_timer = null;
  }
}
