/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Button;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.Spinner;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;

import com.tc.admin.AdminClient;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.XContainer;
import com.tc.stats.statistics.CountStatistic;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RuntimeStatsPanel extends XContainer {
  private Timer                 m_statsGathererTimer;
  protected Container           m_chartsPanel;
  private Button                m_startMonitoringButton;
  private Button                m_stopMonitoringButton;
  private Button                m_clearSamplesButton;
  private Spinner               m_samplePeriodSpinner;
  private Spinner               m_sampleHistorySpinner;

  protected static Dimension    fDefaultGraphSize               = new Dimension(ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH,
                                                                                ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT);

  private static final int      DEFAULT_POLL_PERIOD_SECS        = 3;
  private static final int      DEFAULT_SAMPLE_HISTORY_MINUTES  = 5;
  private static final int      SAMPLE_SAMPLE_HISTORY_STEP_SIZE = 1;

  private ArrayList<TimeSeries> m_allSeries;
  private ArrayList<JFreeChart> m_allCharts;

  public RuntimeStatsPanel() {
    super();
    m_allSeries = new ArrayList<TimeSeries>();
    m_allCharts = new ArrayList<JFreeChart>();
    load((ContainerResource)AdminClient.getContext().topRes.child("RuntimeStatsPanel"));
  }

  public void load(ContainerResource res) {
    super.load(res);

    m_chartsPanel = (Container) findComponent("ChartsPanel");

    m_startMonitoringButton = (Button) findComponent("StartMonitoringButton");
    m_startMonitoringButton.addActionListener(new StartMonitoringAction());

    m_stopMonitoringButton = (Button) findComponent("StopMonitoringButton");
    m_stopMonitoringButton.addActionListener(new StopMonitoringAction());

    m_clearSamplesButton = (Button) findComponent("ClearSamplesButton");
    m_clearSamplesButton.addActionListener(new ClearSamplesAction());

    m_samplePeriodSpinner = (Spinner) findComponent("SamplePeriodSpinner");
    m_samplePeriodSpinner.setModel(new SpinnerNumberModel(new Integer(DEFAULT_POLL_PERIOD_SECS), new Integer(1), null,
                                                          new Integer(1)));
    m_samplePeriodSpinner.addChangeListener(new SamplePeriodChangeHandler());

    m_sampleHistorySpinner = (Spinner) findComponent("SampleHistorySpinner");
    m_sampleHistorySpinner.setModel(new SpinnerNumberModel(new Integer(DEFAULT_SAMPLE_HISTORY_MINUTES), new Integer(1),
                                                           null, new Integer(SAMPLE_SAMPLE_HISTORY_STEP_SIZE)));
    m_sampleHistorySpinner.addChangeListener(new SampleHistoryChangeHandler());

    setup(m_chartsPanel);
  }

  private class StartMonitoringAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      startMonitoringRuntimeStats();
    }
  }

  private class StopMonitoringAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      stopMonitoringRuntimeStats();
    }
  }

  private class ClearSamplesAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      clearAllRuntimeStatsSamples();
    }
  }

  private class SamplePeriodChangeHandler implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      SpinnerNumberModel model = (SpinnerNumberModel) m_samplePeriodSpinner.getModel();
      Integer i = (Integer) model.getNumber();
      setRuntimeStatsPollPeriodSeconds(i.intValue());
    }
  }

  private class SampleHistoryChangeHandler implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      SpinnerNumberModel model = (SpinnerNumberModel) m_sampleHistorySpinner.getModel();
      Integer i = (Integer) model.getNumber();
      setRuntimeStatsSampleHistoryMinutes(i.intValue());
    }
  }

  protected TimeSeries createTimeSeries(String name) {
    TimeSeries ts = new TimeSeries(name, Second.class);
    ts.setMaximumItemCount(50);
    m_allSeries.add(ts);
    return ts;
  }

  protected JFreeChart createChart(TimeSeries series) {
    JFreeChart chart = DemoChartFactory.getXYLineChart("", "", "", series);

    int sampleHistoryMinutes = getRuntimeStatsSampleHistoryMinutes();
    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;
    ((XYPlot) chart.getPlot()).getDomainAxis().setFixedAutoRange(sampleHistoryMillis);

    int maxSampleCount = (sampleHistoryMinutes * 60) / getRuntimeStatsPollPeriodSeconds();
    series.setMaximumItemCount(maxSampleCount);

    /* TODO: This is a hack to make each of the the chart time axes line up */
    /* Need to do something better. */
    AxisSpace rangeAxisSpace = new AxisSpace();
    rangeAxisSpace.setLeft(50);
    rangeAxisSpace.setRight(10);
    ((XYPlot) chart.getPlot()).setFixedRangeAxisSpace(rangeAxisSpace);

    m_allCharts.add(chart);
    return chart;
  }

  protected JFreeChart createChart(TimeSeries[] seriesArray) {
    JFreeChart chart = DemoChartFactory.getXYLineChart("", "", "", seriesArray);

    int sampleHistoryMinutes = getRuntimeStatsSampleHistoryMinutes();
    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;
    ((XYPlot) chart.getPlot()).getDomainAxis().setFixedAutoRange(sampleHistoryMillis);

    int maxSampleCount = (sampleHistoryMinutes * 60) / getRuntimeStatsPollPeriodSeconds();
    for (TimeSeries series : seriesArray) {
      series.setMaximumItemCount(maxSampleCount);
    }

    /* TODO: This is a hack to make each of the the chart time axes line up */
    /* Need to do something better. */
    AxisSpace rangeAxisSpace = new AxisSpace();
    rangeAxisSpace.setLeft(50);
    rangeAxisSpace.setRight(10);
    ((XYPlot) chart.getPlot()).setFixedRangeAxisSpace(rangeAxisSpace);

    m_allCharts.add(chart);
    return chart;
  }

  protected void setup(Container runtimeStatsPanel) {
    /* override this */
  }

  public void startMonitoringRuntimeStats() {
    testStartStatsGatherer();
    m_chartsPanel.setVisible(true);
    m_startMonitoringButton.setEnabled(false);
    m_stopMonitoringButton.setEnabled(true);
  }

  public void stopMonitoringRuntimeStats() {
    if (m_statsGathererTimer != null) {
      m_statsGathererTimer.stop();
      m_startMonitoringButton.setEnabled(true);
      m_stopMonitoringButton.setEnabled(false);
    }
  }

  private void clearAllRuntimeStatsSamples() {
    boolean monitoring = false;
    if (m_statsGathererTimer != null && m_statsGathererTimer.isRunning()) {
      monitoring = true;
      m_statsGathererTimer.stop();
    }

    Iterator<TimeSeries> iter = m_allSeries.iterator();
    while (iter.hasNext()) {
      iter.next().clear();
    }

    if (monitoring) {
      m_statsGathererTimer.start();
    }
  }

  private void setRuntimeStatsPollPeriodSeconds(int seconds) {
    if (m_statsGathererTimer != null) {
      int pollMillis = seconds * 1000;
      m_statsGathererTimer.setDelay(pollMillis);

      Iterator<JFreeChart> chartIter = m_allCharts.iterator();
      int sampleHistoryMinutes = getRuntimeStatsSampleHistoryMinutes();
      int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;
      while (chartIter.hasNext()) {
        ((XYPlot) chartIter.next().getPlot()).getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
      }
    }
  }

  private int getRuntimeStatsPollPeriodSeconds() {
    SpinnerNumberModel model = (SpinnerNumberModel) m_samplePeriodSpinner.getModel();
    Integer i = (Integer) model.getNumber();
    return i.intValue();
  }

  private int getRuntimeStatsPollPeriodMillis() {
    return getRuntimeStatsPollPeriodSeconds() * 1000;
  }

  private int getRuntimeStatsSampleHistoryMinutes() {
    SpinnerNumberModel model = (SpinnerNumberModel) m_sampleHistorySpinner.getModel();
    Integer i = (Integer) model.getNumber();
    return i.intValue();
  }

  private void setRuntimeStatsSampleHistoryMinutes(int sampleHistoryMinutes) {
    int sampleHistoryMillis = sampleHistoryMinutes * 60 * 1000;
    int maxSampleCount = (sampleHistoryMinutes * 60) / getRuntimeStatsPollPeriodSeconds();

    Iterator<TimeSeries> seriesIter = m_allSeries.iterator();
    while (seriesIter.hasNext()) {
      seriesIter.next().setMaximumItemCount(maxSampleCount);
    }

    Iterator<JFreeChart> chartIter = m_allCharts.iterator();
    while (chartIter.hasNext()) {
      ((XYPlot) chartIter.next().getPlot()).getDomainAxis().setFixedAutoRange(sampleHistoryMillis);
    }
  }

  private void testStartStatsGatherer() {
    if (m_statsGathererTimer == null) {
      int pollMillis = getRuntimeStatsPollPeriodMillis();
      m_statsGathererTimer = new Timer(pollMillis, new StatisticsRetrievalAction());
    }
    if (!m_statsGathererTimer.isRunning()) {
      m_statsGathererTimer.start();
    }
  }

  private Date m_tmpDate = new Date();

  protected void updateSeries(TimeSeries series, CountStatistic value) {
    m_tmpDate.setTime(value.getLastSampleTime());
    series.addOrUpdate(new Second(m_tmpDate), value.getCount());
  }

  protected void retrieveStatistics() {
    /* override this */
  }

  class StatisticsRetrievalAction implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      retrieveStatistics();
    }
  }

}
