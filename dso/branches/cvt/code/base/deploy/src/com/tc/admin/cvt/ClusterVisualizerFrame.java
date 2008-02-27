/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.cvt;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import com.tc.admin.common.DemoChartFactory;
import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseException;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.jdbc.ResultSetHandler;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStoreImportListener;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreSetupErrorException;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ClusterVisualizerFrame extends JFrame {
  private JComboBox                                fSessionSelector;
  private JSlider                                  fHeightSlider;
  private SessionInfo                              fSessionInfo;
  private JPanel                                   fBottomPanel;
  private JPanel                                   fControlPanel;
  private JPanel                                   fGraphPanel;
  private MyStatisticsStore                        fStore;
  private Map<NodeControl, Map<String, JCheckBox>> fNodeGroups;
  private Map<JCheckBox, Map<Node, NodeControl>>   fStatGroups;
  private Map<String, AbstractStatHandler>         fStatHandlerMap;
  private JProgressBar                             fProgressBar;
  private File                                     fLastDir;

  public ClusterVisualizerFrame(String[] args) {
    super("Cluster Visualizer");
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    JMenu fileMenu = new JMenu("File");
    ImportAction importAction = new ImportAction();
    fileMenu.add(importAction);
    RetrieveAction retrieveAction = new RetrieveAction();
    fileMenu.add(retrieveAction);
    fileMenu.addSeparator();
    ExitAction exitAction = new ExitAction();
    fileMenu.add(exitAction);
    menuBar.add(fileMenu);

    JPanel toolBar = new JPanel(new FlowLayout());
    getContentPane().add(toolBar, BorderLayout.NORTH);
    toolBar.add(new JButton(importAction));
    toolBar.add(new JButton(retrieveAction));
    toolBar.add(fSessionSelector = new JComboBox());
    toolBar.add(fHeightSlider = new JSlider(100, 600, 250));
    fHeightSlider.addChangeListener(new SliderHeightListener());
    fSessionSelector.addActionListener(new SessionSelectorHandler());
    getContentPane().add(fBottomPanel = new JPanel(new BorderLayout()), BorderLayout.CENTER);
    getContentPane().add(fProgressBar = new JProgressBar(), BorderLayout.SOUTH);

    fNodeGroups = new HashMap<NodeControl, Map<String, JCheckBox>>();
    fStatGroups = new HashMap<JCheckBox, Map<Node, NodeControl>>();
    fStatHandlerMap = new HashMap<String, AbstractStatHandler>();
    putStatHandler(new MemoryUsageHandler());
    putStatHandler(new L2toL1FaultHandler());
    putStatHandler(new CPUUsageHandler());

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  class SliderHeightListener implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      int newHeight = fHeightSlider.getValue();
      Dimension newDim = new Dimension(ChartPanel.DEFAULT_WIDTH, newHeight);
      for (Component comp : fGraphPanel.getComponents()) {
        if (comp instanceof ChartPanel) {
          ChartPanel chartPanel = (ChartPanel) comp;
          chartPanel.setPreferredSize(newDim);
        }
      }
      fGraphPanel.revalidate();
      fGraphPanel.repaint();
    }
  }

  class SessionSelectorHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      try {
        fSessionInfo = (SessionInfo) fSessionSelector.getSelectedItem();
        if (fSessionInfo == null) return;
        fGraphPanel.removeAll();
        fGraphPanel.setLayout(null);
        setupControlPanel();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  class GenerateGraphsAction extends AbstractAction {
    GenerateGraphsAction() {
      super("Generate graphs");
    }

    public void actionPerformed(ActionEvent ae) {
      fGraphPanel.removeAll();
      fGraphPanel.setLayout(new GridLayout(0, 1));

      {
        Iterator<NodeControl> nodeIter = fNodeGroups.keySet().iterator();
        while (nodeIter.hasNext()) {
          NodeControl nodeControl = nodeIter.next();
          Node node = nodeControl.getNode();
          ArrayList<String> statList = new ArrayList<String>();
          if (nodeControl.isSelected()) {
            Map<String, JCheckBox> controlMap = fNodeGroups.get(nodeControl);
            Iterator<String> statIter = controlMap.keySet().iterator();
            while (statIter.hasNext()) {
              String stat = statIter.next();
              JCheckBox control = controlMap.get(stat);
              if (control.isSelected()) {
                statList.add(stat);
              }
            }
          }
          if (statList.size() > 0) {
            NodeGroupHandler ngh = new NodeGroupHandler(node, statList);
            List<DataDisplay> displayList = ngh.generateDisplay();
            JFreeChart chart = buildGraph(node.toString(), displayList, true);
          }
        }
      }

      {
        Iterator<JCheckBox> statIter = fStatGroups.keySet().iterator();
        while (statIter.hasNext()) {
          JCheckBox statControl = statIter.next();
          String stat = statControl.getName();
          ArrayList<Node> nodeList = new ArrayList<Node>();
          if (statControl.isSelected()) {
            Map<Node, NodeControl> controlMap = fStatGroups.get(statControl);
            Iterator<Node> nodeIter = controlMap.keySet().iterator();
            while (nodeIter.hasNext()) {
              Node node = nodeIter.next();
              JCheckBox control = controlMap.get(node);
              if (control.isSelected()) {
                nodeList.add(node);
              }
            }
          }
          if (nodeList.size() > 0) {
            StatGroupHandler sgh = new StatGroupHandler(stat, nodeList);
            List<DataDisplay> displayList = sgh.generateDisplay();
            JFreeChart chart = buildGraph(stat, displayList, false);
          }
        }
      }

      fGraphPanel.revalidate();
      fGraphPanel.repaint();
    }
  }

  class ImportAction extends AbstractAction {
    ImportAction() {
      super("Import...");
    }

    public void actionPerformed(ActionEvent ae) {
      JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setDialogTitle("Import statistics session");
      chooser.setMultiSelectionEnabled(false);
      if (fLastDir != null) {
        chooser.setCurrentDirectory(fLastDir);
      }
      if (chooser.showOpenDialog(ClusterVisualizerFrame.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      fLastDir = file.getParentFile();
      fProgressBar.setIndeterminate(true);
      fProgressBar.setVisible(true);
      new Thread(new ImportRunner(file)).start();
    }
  }

  class ImportRunner implements Runnable {
    File fFile;

    ImportRunner(File file) {
      fFile = file;
    }

    public void run() {
      String fileName = fFile.getName();
      if (fileName.endsWith(".zip")) {
        FileInputStream fis = null;

        try {
          fis = new FileInputStream(fFile);
          ZipInputStream zis = null;
          try {
            zis = new ZipInputStream(fis);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
              String name = entry.getName();
              if (name.endsWith(".csv")) {
                populateStore(zis);
                zis.closeEntry();
                return;
              }
              zis.closeEntry();
            }
          } finally {
            IOUtils.closeQuietly(zis);
          }
        } catch (Exception e) {
          IOUtils.closeQuietly(fis);
          e.printStackTrace();
          return;
        }
      } else if (fileName.endsWith(".csv")) {
        FileInputStream fis = null;

        try {
          fis = new FileInputStream(fFile);
          populateStore(fis);
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          IOUtils.closeQuietly(fis);
        }
      }
    }
  }

  private void populateStore(InputStream in) throws Exception {
    resetStore();

    try {
      fStore.importCsvStatistics(new InputStreamReader(in), new StatisticsStoreImportListener() {
        public void started() {
        }
        public void imported(long count) {
        }
        public void optimizing() {
        }
        public void finished(long total) {
        }
      });
    } finally {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          fProgressBar.setVisible(false);
          try {
            newStore();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
    }
  }

  void resetStore() throws Exception {
    if (fStore != null) {
      fStore.close();
    }
    File dir = new File("C:/temp/retrieve-store");
    try {
      FileUtils.deleteDirectory(dir);
    } catch (Exception e) {
      e.printStackTrace();
    }
    dir.mkdir();
    fStore = new MyStatisticsStore(dir);
    fStore.open();
  }

  void newStore() throws Exception {
    fBottomPanel.removeAll();
    fGraphPanel = new JPanel();
    fBottomPanel.add(new JScrollPane(fGraphPanel), BorderLayout.CENTER);

    List<SessionInfo> sessionList = fStore.getAllSessions();
    DefaultComboBoxModel comboModel = new DefaultComboBoxModel(sessionList.toArray(new SessionInfo[0]));
    fSessionSelector.setModel(comboModel);
  }

  /**
   * This is just a quick, temporary, way to grab the stats data from the L2 at localhost:9520.
   */
  class RetrieveAction extends AbstractAction {
    RetrieveAction() {
      super("Retrieve...");
    }

    public void actionPerformed(ActionEvent ae) {
      GetMethod get = null;
      try {
        String uri = "http://localhost:9510/statistics-gatherer/retrieveStatistics?format=txt";
        URL url = new URL(uri);
        HttpClient httpClient = new HttpClient();

        get = new GetMethod(url.toString());
        get.setFollowRedirects(true);
        int status = httpClient.executeMethod(get);
        if (status != HttpStatus.SC_OK) {
          System.err.println("The http client has encountered a status code other than ok for the url: " + url
                             + " status: " + HttpStatus.getStatusText(status));
          return;
        }
        fProgressBar.setIndeterminate(true);
        fProgressBar.setVisible(true);
        new Thread(new StreamCopierRunnable(get)).start();
      } catch (Exception e) {
        e.printStackTrace();
        if (get != null) {
          get.releaseConnection();
        }
      }
    }
  }

  class StreamCopierRunnable implements Runnable {
    GetMethod fGetMethod;

    StreamCopierRunnable(GetMethod getMethod) {
      fGetMethod = getMethod;
    }

    public void run() {
      InputStream in = null;

      try {
        populateStore(in = fGetMethod.getResponseBodyAsStream());
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        IOUtils.closeQuietly(in);
        fGetMethod.releaseConnection();
      }
    }
  }

  private void filterStats(List<String> stats) throws Exception {
    if (stats.contains("memory used")) {
      Iterator<String> iter = stats.iterator();
      while(iter.hasNext()) {
        String stat = iter.next();
        if(stat.startsWith("memory ")) {
          iter.remove();
        }
      }
      stats.add("memory usage");
    }
    
    if(stats.contains("cpu user")) {
      Iterator<String> iter = stats.iterator();
      while(iter.hasNext()) {
        String stat = iter.next();
        if(stat.startsWith("cpu ")) {
          iter.remove();
        }
      }
      stats.add("cpu usage");
    }
  }

  private void setupControlPanel() throws Exception {
    if (fSessionInfo == null) return;

    fControlPanel = new JPanel(new BorderLayout());
    List<Node> nodes = fSessionInfo.fNodeList;
    List<String> stats = fSessionInfo.fStatList;
    Iterator<Node> nodeIter = nodes.iterator();
    Iterator<String> statIter = stats.iterator();
    JPanel buttonPanel = new JPanel(new GridBagLayout());
    buttonPanel.add(new JButton(new GenerateGraphsAction()));
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel gridPanel = new JPanel(new GridBagLayout());
    fControlPanel.add(gridPanel, BorderLayout.NORTH);
    gbc.gridx = 0;
    gbc.ipady = gbc.ipadx = 10;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gridPanel.add(buttonPanel);
    fNodeGroups.clear();
    while (nodeIter.hasNext()) {
      Node node = nodeIter.next();
      List<String> nodeStats = fStore.getAvailableStatsForNode(fSessionInfo.fId, node);
      filterStats(nodeStats);
      if (nodeStats.size() > 0) {
        gridPanel.add(createNodeGroup(node, nodeStats), gbc);
      }
    }
    fStatGroups.clear();
    boolean haveMemoryUsage = false;
    boolean haveCPUUsage = false;
    while (statIter.hasNext()) {
      String stat = statIter.next();
      List<Node> statNodes = fStore.getAvailableNodesForStat(fSessionInfo.fId, stat);
      if (statNodes.size() > 0) {
        if (!haveMemoryUsage && stat.startsWith("memory ")) {
          stat = "memory usage";
          haveMemoryUsage = true;
          gridPanel.add(createStatGroup(stat, statNodes), gbc);
        } else if (!haveCPUUsage && stat.startsWith("cpu ")) {
          stat = "cpu usage";
          haveCPUUsage = true;
          gridPanel.add(createStatGroup(stat, statNodes), gbc);
        }

      }
    }
    fBottomPanel.add(new JScrollPane(fControlPanel), BorderLayout.WEST);
    fBottomPanel.revalidate();
    fBottomPanel.repaint();
  }

  private Container createNodeGroup(Node node, List<String> stats) {
    Map<String, JCheckBox> controlMap = new HashMap<String, JCheckBox>();
    JPanel panel = new JPanel(new GridLayout(stats.size() + 1, 1));
    Insets insets = new Insets(0, 20, 0, 0);
    NodeControl leadControl = new NodeControl(node);
    panel.add(leadControl);
    Iterator<String> iter = stats.iterator();
    while (iter.hasNext()) {
      String follower = iter.next();
      JCheckBox control = new JCheckBox(follower, true);
      control.setName(follower);
      control.setMargin(insets);
      panel.add(control);
      controlMap.put(follower, control);
    }
    fNodeGroups.put(leadControl, controlMap);
    return panel;
  }

  private Container createStatGroup(String stat, List<Node> nodes) {
    Map<Node, NodeControl> controlMap = new HashMap<Node, NodeControl>();
    JPanel panel = new JPanel(new GridLayout(nodes.size() + 1, 1));
    Insets insets = new Insets(0, 20, 0, 0);
    JCheckBox leadControl = new JCheckBox(stat, true);
    leadControl.setName(stat);
    panel.add(leadControl);
    Iterator<Node> iter = nodes.iterator();
    while (iter.hasNext()) {
      Node node = iter.next();
      NodeControl control = new NodeControl(node);
      control.setMargin(insets);
      panel.add(control);
      controlMap.put(node, control);
    }
    fStatGroups.put(leadControl, controlMap);
    return panel;
  }

  private XYDataset[] display2Dataset(List<DataDisplay> displayList) {
    Iterator<DataDisplay> iter = displayList.iterator();
    List<XYDataset> list = new ArrayList<XYDataset>();
    while (iter.hasNext()) {
      DataDisplay dd = iter.next();
      list.add(dd.fXYDataset);
    }
    return list.toArray(new XYDataset[0]);
  }

  private JFreeChart buildGraph(String title, List<DataDisplay> displayList, boolean multiAxis) {
    XYDataset[] xyDatasets = display2Dataset(displayList);
    JFreeChart chart = DemoChartFactory.createXYLineChart("", "", "", xyDatasets[0], true);
    XYPlot plot = (XYPlot) chart.getPlot();
    ((DateAxis) plot.getDomainAxis()).setMinimumDate(fSessionInfo.fStart);
    boolean topOrLeft = true;
    for (int i = 0; i < displayList.size(); i++) {
      DataDisplay dd = displayList.get(i);
      plot.setDataset(i, xyDatasets[i]);
      if (i == 0 || multiAxis) {
        plot.setRangeAxis(i, dd.fAxis);
        if (!multiAxis) {
          dd.fAxis.setLabel("");
        }
      }
      XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
      plot.setRenderer(i, renderer);
      Paint paint = renderer.lookupSeriesPaint(i);
      renderer.setSeriesPaint(0, paint);
      if (multiAxis) {
        plot.setRangeAxisLocation(i, topOrLeft ? AxisLocation.TOP_OR_LEFT : AxisLocation.BOTTOM_OR_RIGHT);
        topOrLeft = topOrLeft ? false : true;
        plot.mapDatasetToRangeAxis(i, i);
        dd.fAxis.setLabelPaint(paint);
        dd.fAxis.setTickLabelPaint(paint);
      }
    }
    ChartPanel chartPanel = new ChartPanel(chart, false);
    chartPanel.setBorder(new TitledBorder(title));
    fGraphPanel.add(chartPanel);
    return chart;
  }

  class NodeGroupHandler {
    Node fNode;
    List fStats;

    NodeGroupHandler(Node node, List<String> stats) {
      fNode = node;
      fStats = stats;
    }

    List<DataDisplay> generateDisplay() {
      ArrayList<DataDisplay> displayList = new ArrayList<DataDisplay>();
      Iterator<String> statIter = fStats.iterator();
      while (statIter.hasNext()) {
        String stat = statIter.next();
        AbstractStatHandler handler = getStatHandler(stat);
        if (handler != null) {
          handler.setNode(fNode);
          handler.setLegend(stat);
          displayList.add(handler.generateDisplay());
        }
      }
      return displayList;
    }
  }

  class StatGroupHandler {
    String     fStat;
    List<Node> fNodes;

    StatGroupHandler(String stat, List<Node> nodes) {
      fStat = stat;
      fNodes = nodes;
    }

    List<DataDisplay> generateDisplay() {
      ArrayList<DataDisplay> displayList = new ArrayList<DataDisplay>();
      AbstractStatHandler handler = getStatHandler(fStat);
      if (handler != null) {
        Iterator<Node> nodeIter = fNodes.iterator();
        while (nodeIter.hasNext()) {
          Node node = nodeIter.next();
          handler.setNode(node);
          handler.setLegend(node.toString());
          displayList.add(handler.generateDisplay());
        }
      }
      return displayList;
    }
  }

  abstract class AbstractStatHandler {
    Node   fNode;
    String fLegend;

    void setNode(Node node) {
      fNode = node;
    }

    void setLegend(String legend) {
      fLegend = legend;
    }

    abstract String getName();

    abstract TimeSeries generateSeries();

    abstract DataDisplay generateDisplay();

    abstract NumberAxis generateAxis();
  }

  class MemoryUsageHandler extends AbstractStatHandler {
    private Long fMax;

    String getName() {
      return "memory usage";
    }

    TimeSeries generateSeries() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName("memory max");
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      safeRetrieveStatistics(criteria, new StatisticsConsumer() {
        public boolean consumeStatisticData(StatisticData sd) {
          fMax = (Long) sd.getData();
          return false;
        }
      });

      criteria = new StatisticsRetrievalCriteria();
      criteria.addName("memory used");
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final TimeSeries series = new TimeSeries(fLegend, Second.class);
      safeRetrieveStatistics(criteria, new StatisticsConsumer() {
        public boolean consumeStatisticData(StatisticData sd) {
          Long value = (Long) sd.getData();
          Date moment = sd.getMoment();
          double d = (value / ((double) fMax));
          series.addOrUpdate(new Second(moment), d);
          return true;
        }
      });

      return series;
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis("Memory Usage");
      axis.setRange(0.0, 1.0);
      return axis;
    }

    DataDisplay generateDisplay() {
      DataDisplay display = new DataDisplay(new TimeSeriesCollection(generateSeries()), generateAxis());
      return display;
    }

  }

  class CPUUsageHandler extends AbstractStatHandler {
    String getName() {
      return "cpu usage";
    }

    TimeSeries generateSeries() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName("cpu user");
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final TimeSeries series = new TimeSeries(fLegend, Second.class);
      safeRetrieveStatistics(criteria, new StatisticsConsumer() {
        public boolean consumeStatisticData(StatisticData sd) {
          Number value = (Number) sd.getData();
          Date moment = sd.getMoment();
          series.addOrUpdate(new Second(moment), value.doubleValue());
          return true;
        }
      });

      return series;
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis("CPU Usage");
      axis.setRange(0.0, 1.0);
      return axis;
    }

    DataDisplay generateDisplay() {
      DataDisplay display = new DataDisplay(new TimeSeriesCollection(generateSeries()), generateAxis());
      return display;
    }

  }
  
  class L2toL1FaultHandler extends AbstractStatHandler {
    String getName() {
      return "l2 l1 fault";
    }

    TimeSeries generateSeries() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName("l2 l1 fault");
      criteria.setAgentIp(fNode.fIpAddr);
      criteria.setAgentDifferentiator(fNode.fName);
      criteria.setSessionId(fSessionInfo.fId);
      final TimeSeries series = new TimeSeries(fLegend, Second.class);
      safeRetrieveStatistics(criteria, new StatisticsConsumer() {
        public boolean consumeStatisticData(StatisticData sd) {
          Long value = (Long) sd.getData();
          Date moment = sd.getMoment();
          series.addOrUpdate(new Second(moment), value);
          return true;
        }
      });

      return series;
    }

    NumberAxis generateAxis() {
      NumberAxis axis = new NumberAxis("L2-L1 Fault");
      return axis;
    }

    DataDisplay generateDisplay() {
      DataDisplay display = new DataDisplay(new TimeSeriesCollection(generateSeries()), generateAxis());
      return display;
    }
  }

  void putStatHandler(AbstractStatHandler handler) {
    fStatHandlerMap.put(handler.getName(), handler);
  }

  AbstractStatHandler getStatHandler(String stat) {
    return fStatHandlerMap.get(stat);
  }

  void safeRetrieveStatistics(StatisticsRetrievalCriteria criteria, StatisticsConsumer consumer) {
    try {
      fStore.retrieveStatistics(criteria, consumer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  class ExitAction extends AbstractAction {
    ExitAction() {
      super("Exit");
    }

    public void actionPerformed(ActionEvent ae) {
      try {
        fStore.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.exit(0);
    }
  }

  public static void main(String[] args) throws Exception {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    ClusterVisualizerFrame frame = new ClusterVisualizerFrame(args);
    frame.setSize(new Dimension(700, 650));
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

class Node {
  String fIpAddr;
  String fName;

  Node(String ipAddr, String name) {
    fIpAddr = ipAddr;
    fName = name;
  }

  public String toString() {
    return fIpAddr + " " + fName;
  }
}

class NodeControl extends JCheckBox {
  Node fNode;

  NodeControl(Node node) {
    super(node.toString());
    fNode = node;
    setSelected(true);
  }

  Node getNode() {
    return fNode;
  }
}

class DataDisplay {
  XYDataset  fXYDataset;
  NumberAxis fAxis;

  DataDisplay(XYDataset xyDataset, NumberAxis axis) {
    fXYDataset = xyDataset;
    fAxis = axis;
  }
}

class SessionInfo {
  String       fId;
  Date         fStart;
  Date         fEnd;
  List<Node>   fNodeList;
  List<String> fStatList;

  SessionInfo(String id) {
    fId = id;
  }

  public String toString() {
    return fId;
  }
}

class MyStatisticsStore extends H2StatisticsStoreImpl {
  private final static String SQL_GET_NODES          = "SELECT agentIp, agentDifferentiator FROM statisticlog WHERE sessionid = ? GROUP BY agentIp, agentDifferentiator ORDER BY agentIp ASC";
  private final static String SQL_GET_STATS          = "SELECT statname FROM statisticlog WHERE sessionid = ? GROUP BY statname ORDER BY statname ASC";
  private final static String SQL_GET_STATS_FOR_NODE = "SELECT statname FROM statisticlog WHERE sessionid = ? AND agentIp = ? AND agentDifferentiator = ? GROUP BY statname ORDER BY statname ASC";
  private final static String SQL_GET_NODES_FOR_STAT = "SELECT agentIp, agentDifferentiator FROM statisticlog WHERE sessionid = ? AND statname = ? GROUP BY agentIp, agentDifferentiator ORDER BY agentIp ASC";

  public MyStatisticsStore(File dir) {
    super(dir);
  }

  public synchronized void open() throws TCStatisticsStoreException {
    super.open();
    try {
      database.createPreparedStatement(SQL_GET_NODES);
      database.createPreparedStatement(SQL_GET_STATS);
      database.createPreparedStatement(SQL_GET_STATS_FOR_NODE);
      database.createPreparedStatement(SQL_GET_NODES_FOR_STAT);
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsStoreSetupErrorException(e);
    }
  }

  public List<SessionInfo> getAllSessions() throws TCStatisticsStoreException {
    String[] ids = getAvailableSessionIds();
    List<SessionInfo> result = new ArrayList<SessionInfo>();
    for (String id : ids) {
      final SessionInfo sessionInfo = new SessionInfo(id);
      StatisticsRetrievalCriteria critera = new StatisticsRetrievalCriteria();
      critera.addName("startup timestamp");
      critera.setSessionId(id);
      retrieveStatistics(critera, new StatisticsConsumer() {
        public boolean consumeStatisticData(StatisticData sd) {
          sessionInfo.fStart = sd.getMoment();
          return false;
        }
      });
      critera = new StatisticsRetrievalCriteria();
      critera.addName("shutdown timestamp");
      critera.setSessionId(id);
      retrieveStatistics(critera, new StatisticsConsumer() {
        public boolean consumeStatisticData(StatisticData sd) {
          sessionInfo.fEnd = sd.getMoment();
          return false;
        }
      });
      sessionInfo.fNodeList = getAvailableNodes(id);
      sessionInfo.fStatList = getAvailableStats(id);

      result.add(sessionInfo);
    }
    return result;
  }

  public List<Node> getAvailableNodes(String sessionId) throws TCStatisticsStoreException {
    final List<Node> results = new ArrayList<Node>();
    try {
      database.ensureExistingConnection();
      PreparedStatement ps = database.getPreparedStatement(SQL_GET_NODES);
      ps.setString(1, sessionId);
      JdbcHelper.executeQuery(ps, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            Node node = new Node(resultSet.getString("agentIp"), resultSet.getString("agentDifferentiator"));
            results.add(node);
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TCStatisticsStoreException("getAvailableNodes", e);
    }

    return results;
  }

  public List<String> getAvailableStats(String sessionId) throws TCStatisticsStoreException {
    final List<String> results = new ArrayList<String>();
    try {
      database.ensureExistingConnection();
      PreparedStatement ps = database.getPreparedStatement(SQL_GET_STATS);
      ps.setString(1, sessionId);
      JdbcHelper.executeQuery(ps, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            results.add(new String(resultSet.getString("statname")));
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TCStatisticsStoreException("getAvailableStats", e);
    }

    results.remove("startup timestamp");
    results.remove("shutdown timestamp");
    results.remove("system properties");

    return results;
  }

  public List<String> getAvailableStatsForNode(String sessionId, Node node) throws TCStatisticsStoreException {
    final List<String> results = new ArrayList<String>();
    try {
      database.ensureExistingConnection();
      PreparedStatement ps = database.getPreparedStatement(SQL_GET_STATS_FOR_NODE);
      ps.setString(1, sessionId);
      ps.setString(2, node.fIpAddr);
      ps.setString(3, node.fName);
      JdbcHelper.executeQuery(ps, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            results.add(new String(resultSet.getString("statname")));
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TCStatisticsStoreException("getAvailableStatsForNode", e);
    }

    results.remove("startup timestamp");
    results.remove("shutdown timestamp");
    results.remove("system properties");

    return results;
  }

  public List<Node> getAvailableNodesForStat(String sessionId, String stat) throws TCStatisticsStoreException {
    final List<Node> results = new ArrayList<Node>();
    try {
      database.ensureExistingConnection();
      PreparedStatement ps = database.getPreparedStatement(SQL_GET_NODES_FOR_STAT);
      ps.setString(1, sessionId);
      ps.setString(2, stat);
      JdbcHelper.executeQuery(ps, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            Node node = new Node(resultSet.getString("agentIp"), resultSet.getString("agentDifferentiator"));
            results.add(node);
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TCStatisticsStoreException("getAvailableNodesForStat", e);
    }

    return results;
  }

}