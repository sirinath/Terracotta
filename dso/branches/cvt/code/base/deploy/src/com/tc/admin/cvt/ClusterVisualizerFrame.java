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
import org.apache.commons.lang.builder.HashCodeBuilder;
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
import java.util.Arrays;
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
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ClusterVisualizerFrame extends JFrame {
  private ImportAction                             fImportAction;
  private RetrieveAction                           fRetrieveAction;
  private JComboBox                                fSessionSelector;
  private JPanel                                   fToolBar;
  private JSlider                                  fHeightSlider;
  private SessionInfo                              fSessionInfo;
  private JPanel                                   fBottomPanel;
  private JPanel                                   fControlPanel;
  private JPanel                                   fGraphPanel;
  private MyStatisticsStore                        fStore;
  private Map<NodeControl, Map<String, JCheckBox>> fNodeGroups;
  private Map<StatControl, Map<Node, NodeControl>> fStatGroups;
  private Map<String, AbstractStatHandler>         fStatHandlerMap;
  private JLabel                                   fStatusLine;
  private JProgressBar                             fProgressBar;
  private File                                     fLastDir;
  private Map<DisplaySource, DataDisplay>          fDisplayCache;

  private Dimension                                fDefaultGraphSize = new Dimension(
                                                                                     ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH,
                                                                                     ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT);

  public ClusterVisualizerFrame(String[] args) {
    super("Cluster Visualizer");
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    JMenu fileMenu = new JMenu("File");
    fImportAction = new ImportAction();
    fileMenu.add(fImportAction);
    fRetrieveAction = new RetrieveAction();
    fileMenu.add(fRetrieveAction);
    fileMenu.addSeparator();
    ExitAction exitAction = new ExitAction();
    fileMenu.add(exitAction);
    menuBar.add(fileMenu);

    fToolBar = new JPanel(new GridBagLayout());
    getContentPane().add(fToolBar, BorderLayout.NORTH);
    fToolBar.add(new JButton(fImportAction));
    fToolBar.add(new JButton(fRetrieveAction));
    fToolBar.add(fSessionSelector = new JComboBox());
    fToolBar.add(fHeightSlider = new JSlider(100, 600, 250));
    fHeightSlider.addChangeListener(new SliderHeightListener());
    fSessionSelector.addActionListener(new SessionSelectorHandler());
    getContentPane().add(fBottomPanel = new JPanel(new BorderLayout()), BorderLayout.CENTER);
    JPanel statusPanel = new JPanel(new BorderLayout());
    statusPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
    statusPanel.add(fStatusLine = new JLabel());
    statusPanel.add(fProgressBar = new JProgressBar(), BorderLayout.EAST);
    getContentPane().add(statusPanel, BorderLayout.SOUTH);
    fProgressBar.setIndeterminate(true);
    fProgressBar.setVisible(false);
    fSessionSelector.setVisible(false);
    fHeightSlider.setVisible(false);

    fNodeGroups = new HashMap<NodeControl, Map<String, JCheckBox>>();
    fStatGroups = new HashMap<StatControl, Map<Node, NodeControl>>();
    fStatHandlerMap = new HashMap<String, AbstractStatHandler>();
    putStatHandler(new MemoryUsageHandler());
    putStatHandler(new L2toL1FaultHandler());
    putStatHandler(new TxRateHandler());
    putStatHandler(new CPUUsageHandler());

    fDisplayCache = new HashMap<DisplaySource, DataDisplay>();

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  class SliderHeightListener implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      int newHeight = fHeightSlider.getValue();
      Dimension newDim = new Dimension(ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH, newHeight);
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
      fSessionInfo = (SessionInfo) fSessionSelector.getSelectedItem();
      if (fSessionInfo == null) return;
      fGraphPanel.removeAll();
      fGraphPanel.setLayout(null);
      fProgressBar.setVisible(true);
      new NodeStatMappingThread().start();
    }
  }

  class GenerateGraphsAction extends AbstractAction implements Runnable {
    List<NodeGroupHandler> fNodeGroupHandlers;
    List<StatGroupHandler> fStatGroupHandlers;

    GenerateGraphsAction() {
      super("Generate graphs");
    }

    List<NodeGroupHandler> getNodeGroupHandlers() {
      ArrayList<NodeGroupHandler> list = new ArrayList<NodeGroupHandler>();
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
          list.add(new NodeGroupHandler(node, statList));
        }
      }
      return list;
    }

    List<StatGroupHandler> getStatGroupHandlers() {
      ArrayList<StatGroupHandler> list = new ArrayList<StatGroupHandler>();
      Iterator<StatControl> statIter = fStatGroups.keySet().iterator();
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
          list.add(new StatGroupHandler(stat, nodeList));
        }
      }
      return list;
    }

    public void actionPerformed(ActionEvent ae) {
      fGraphPanel.removeAll();
      fGraphPanel.setLayout(new GridLayout(0, 1));
      fProgressBar.setVisible(true);
      setToolBarEnabled(false);

      fNodeGroupHandlers = getNodeGroupHandlers();
      fStatGroupHandlers = getStatGroupHandlers();

      Thread graphBuilderThread = new Thread(this);
      graphBuilderThread.start();
    }

    public void run() {
      Iterator<NodeGroupHandler> nodeGroupHandlerIter = fNodeGroupHandlers.iterator();
      while (nodeGroupHandlerIter.hasNext()) {
        NodeGroupHandler ngh = nodeGroupHandlerIter.next();
        List<DataDisplay> displayList = ngh.generateDisplay();
        buildGraphLater(ngh.fNode.toString(), displayList, true);
      }

      Iterator<StatGroupHandler> statGroupHandlerIter = fStatGroupHandlers.iterator();
      while (statGroupHandlerIter.hasNext()) {
        StatGroupHandler sgh = statGroupHandlerIter.next();
        List<DataDisplay> displayList = sgh.generateDisplay();
        buildGraphLater(sgh.fStat, displayList, false);
      }

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          fProgressBar.setVisible(false);
          fStatusLine.setText("");
          fHeightSlider.setVisible(true);
          setToolBarEnabled(true);
        }
      });
    }

    private void buildGraphLater(final String name, final List<DataDisplay> displayList, final boolean multiAxis) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          buildGraph(name, displayList, multiAxis);
          fGraphPanel.revalidate();
          fGraphPanel.repaint();
        }
      });
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
      setToolBarEnabled(false);
      File file = chooser.getSelectedFile();
      fLastDir = file.getParentFile();
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

  private void setStatusLineLater(final String s) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        fStatusLine.setText(s);
      }
    });
  }

  private void populateStore(InputStream in) throws Exception {
    resetStore();

    try {
      fStatusLine.setText("");
      fStore.importCsvStatistics(new InputStreamReader(in), new StatisticsStoreImportListener() {
        public void started() {
        }

        public void imported(final long count) {
          setStatusLineLater("Read " + count + " entries");
        }

        public void optimizing() {
          setStatusLineLater(fStatusLine.getText() + " (optimizing...)");
        }

        public void finished(final long total) {
          setStatusLineLater("Imported " + total + " entries");
        }
      });
    } finally {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            newStore();
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            fProgressBar.setVisible(false);
            setToolBarEnabled(true);
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

    fDisplayCache.clear();
  }

  void newStore() throws Exception {
    fBottomPanel.removeAll();
    fGraphPanel = new JPanel();
    fBottomPanel.add(new JScrollPane(fGraphPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

    List<SessionInfo> sessionList = fStore.getAllSessions();
    DefaultComboBoxModel comboModel = new DefaultComboBoxModel(sessionList.toArray(new SessionInfo[0]));
    fSessionSelector.setModel(comboModel);
    fSessionSelector.setVisible(true);
  }

  /**
   * This is just a quick, temporary, way to grab the stats data from the L2 at localhost:9520.
   */
  class RetrieveAction extends AbstractAction {
    RetrieveAction() {
      super("Retrieve...");
    }

    public void actionPerformed(ActionEvent ae) {
      String addr = (String) JOptionPane.showInputDialog(fBottomPanel, "Enter server address", getTitle(),
                                                         JOptionPane.QUESTION_MESSAGE, null, null, "localhost:9510");
      if (addr == null) return;
      GetMethod get = null;
      try {
        fProgressBar.setVisible(true);
        String uri = "http://" + addr + "/statistics-gatherer/retrieveStatistics?format=txt";
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
        setToolBarEnabled(false);
        new Thread(new StreamCopierRunnable(get)).start();
      } catch (Exception e) {
        fProgressBar.setVisible(false);
        setToolBarEnabled(true);
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

  private void filterStats(List<String> stats) {
    if (stats.contains("memory used")) {
      Iterator<String> iter = stats.iterator();
      while (iter.hasNext()) {
        String stat = iter.next();
        if (stat.startsWith("memory ")) {
          iter.remove();
        }
      }
      stats.add("memory usage");
    }

    if (stats.contains("cpu combined")) {
      Iterator<String> iter = stats.iterator();
      while (iter.hasNext()) {
        String stat = iter.next();
        if (stat.startsWith("cpu ")) {
          iter.remove();
        }
      }
      stats.add("cpu usage");
    }
  }

  private void setToolBarEnabled(boolean enabled) {
    fToolBar.setEnabled(enabled);
    fImportAction.setEnabled(enabled);
    fRetrieveAction.setEnabled(enabled);
    fSessionSelector.setEnabled(enabled);
    fHeightSlider.setEnabled(enabled);
  }

  class NodeStatMappingThread extends Thread {
    public void run() {
      List<Node> nodes = fSessionInfo.fNodeList;
      Iterator<Node> nodeIter = nodes.iterator();
      fSessionInfo.fNodeStatsMap.clear();
      while (nodeIter.hasNext()) {
        Node node = nodeIter.next();
        setStatusLineLater("Getting stats for '" + node + "'");
        List<String> nodeStats = getAvailableStatsForNode(node);
        filterStats(nodeStats);
        fSessionInfo.fNodeStatsMap.put(node, nodeStats);
      }

      List<String> stats = fSessionInfo.fStatList;
      Iterator<String> statIter = stats.iterator();
      fSessionInfo.fStatNodesMap.clear();
      while (statIter.hasNext()) {
        String stat = statIter.next();
        setStatusLineLater("Getting nodes for '" + stat + "'");
        List<Node> statNodes = getAvailableNodesForStat(stat);
        fSessionInfo.fStatNodesMap.put(stat, statNodes);
      }

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          fStatusLine.setText("");
          setupControlPanel();
          fProgressBar.setVisible(false);
          setToolBarEnabled(true);
        }
      });
    }
  }

  private List<String> getAvailableStatsForNode(Node node) {
    try {
      return fStore.getAvailableStatsForNode(fSessionInfo.fId, node);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<String>();
    }
  }

  private List<Node> getAvailableNodesForStat(String stat) {
    try {
      return fStore.getAvailableNodesForStat(fSessionInfo.fId, stat);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<Node>();
    }
  }

  private void setupControlPanel() {
    fControlPanel = new JPanel(new BorderLayout());
    fControlPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
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
      List<String> nodeStats = fSessionInfo.fNodeStatsMap.get(node);
      if (nodeStats != null && nodeStats.size() > 0) {
        gridPanel.add(createNodeGroup(node, nodeStats), gbc);
      }
    }
    fStatGroups.clear();
    boolean haveMemoryUsage = false;
    boolean haveCPUUsage = false;
    while (statIter.hasNext()) {
      String stat = statIter.next();
      List<Node> statNodes = fSessionInfo.fStatNodesMap.get(stat);
      if (statNodes != null && statNodes.size() > 0) {
        if (!haveMemoryUsage && stat.startsWith("memory ")) {
          stat = "memory usage";
          haveMemoryUsage = true;
          gridPanel.add(createStatGroup(stat, statNodes), gbc);
        } else if (!haveCPUUsage && stat.startsWith("cpu ")) {
          stat = "cpu usage";
          haveCPUUsage = true;
          gridPanel.add(createStatGroup(stat, statNodes), gbc);
        } else if (stat.equals("l2 transaction count") || stat.equals("l2 l1 fault")) {
          gridPanel.add(createStatGroup(stat, statNodes), gbc);
        }
      }
    }
    fBottomPanel.add(new JScrollPane(fControlPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.WEST);
    fBottomPanel.revalidate();
    fBottomPanel.repaint();
  }

  private Container createNodeGroup(Node node, List<String> stats) {
    Map<String, JCheckBox> controlMap = new HashMap<String, JCheckBox>();
    JPanel panel = new JPanel(new GridLayout(stats.size() + 1, 1));
    Insets insets = new Insets(0, 20, 0, 0);
    NodeControl leadControl = new NodeLeadControl(node);
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
    StatControl leadControl = new StatLeadControl(stat);
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
    ChartPanel chartPanel = new ChartPanel(chart, true);
    chartPanel.setBorder(new TitledBorder(title));
    fGraphPanel.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    return chart;
  }

  class DisplaySource {
    Node   fNode;
    String fStat;

    DisplaySource(Node node, String stat) {
      fNode = node;
      fStat = stat;
    }

    public int hashCode() {
      HashCodeBuilder hcb = new HashCodeBuilder();
      return hcb.append(fNode).append(fStat).toHashCode();
    }

    public boolean equals(Object other) {
      if (other == null) return false;
      if (!(other instanceof DisplaySource)) return false;
      if (other == this) return true;
      DisplaySource otherDisplaySource = (DisplaySource) other;
      return fNode.equals(otherDisplaySource.fNode) && fStat.equals(otherDisplaySource.fStat);
    }

    public String toString() {
      return fNode + ":" + fStat;
    }
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
        DisplaySource displaySource = new DisplaySource(fNode, stat);
        DataDisplay cachedDisplay = fDisplayCache.get(displaySource);
        if (cachedDisplay != null) {
          DataDisplay display = cachedDisplay.createCopy(stat);
          if (display != null) {
            System.out.println("Copied existing display for '" + display + "'");
            displayList.add(display);
            continue;
          }
        }
        AbstractStatHandler handler = getStatHandler(stat);
        if (handler != null) {
          handler.setNode(fNode);
          handler.setLegend(stat);
          setStatusLineLater("Generating display for '" + displaySource + "'");
          DataDisplay display = handler.generateDisplay();
          displayList.add(display);
          fDisplayCache.put(displaySource, display);
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
          DisplaySource displaySource = new DisplaySource(node, fStat);
          DataDisplay cachedDisplay = fDisplayCache.get(displaySource);
          if (cachedDisplay != null) {
            DataDisplay display = cachedDisplay.createCopy(node.toString());
            if (display != null) {
              System.out.println("Copied existing display for '" + display + "'");
              displayList.add(display);
              continue;
            }
          }
          handler.setNode(node);
          handler.setLegend(node.toString());
          setStatusLineLater("Generating display for '" + displaySource + "'");
          DataDisplay display = handler.generateDisplay();
          displayList.add(display);
          fDisplayCache.put(displaySource, display);
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
      criteria.addName("cpu combined");
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

  class TxRateHandler extends AbstractStatHandler {
    String getName() {
      return "l2 transaction count";
    }

    TimeSeries generateSeries() {
      StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
      criteria.addName("l2 transaction count");
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
      NumberAxis axis = new NumberAxis("Tx Rate");
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

  class NodeLeadControl extends NodeControl implements ActionListener {
    NodeLeadControl(Node node) {
      super(node);
      addActionListener(this);
    }

    public void actionPerformed(ActionEvent ae) {
      if (fNodeGroups != null) {
        boolean selected = isSelected();
        Map<String, JCheckBox> statControlMap = fNodeGroups.get(this);
        if (statControlMap != null) {
          Iterator<String> statIter = statControlMap.keySet().iterator();
          while (statIter.hasNext()) {
            JCheckBox statControl = statControlMap.get(statIter.next());
            statControl.setEnabled(selected);
          }
        }
      }
    }
  }

  class StatControl extends JCheckBox {
    String fStat;

    StatControl(String stat) {
      super(stat);
      fStat = stat;
      setSelected(true);
    }

    String getStat() {
      return fStat;
    }
  }

  class StatLeadControl extends StatControl implements ActionListener {
    StatLeadControl(String stat) {
      super(stat);
      addActionListener(this);
    }

    public void actionPerformed(ActionEvent ae) {
      if (fStatGroups != null) {
        boolean selected = isSelected();
        Map<Node, NodeControl> nodeControlMap = fStatGroups.get(this);
        if (nodeControlMap != null) {
          Iterator<Node> nodeIter = nodeControlMap.keySet().iterator();
          while (nodeIter.hasNext()) {
            NodeControl nodeControl = nodeControlMap.get(nodeIter.next());
            nodeControl.setEnabled(selected);
          }
        }
      }
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

  public int hashCode() {
    HashCodeBuilder hcb = new HashCodeBuilder();
    return hcb.append(fIpAddr).append(fName).toHashCode();
  }

  public boolean equals(Object other) {
    if (other == null) return false;
    if (!(other instanceof Node)) return false;
    if (other == this) return true;
    Node otherNode = (Node) other;
    return fIpAddr.equals(otherNode.fIpAddr) && fName.equals(otherNode.fName);
  }

  public String toString() {
    return fIpAddr + " " + fName;
  }
}

class DataDisplay {
  XYDataset  fXYDataset;
  NumberAxis fAxis;

  DataDisplay(XYDataset xyDataset, NumberAxis axis) {
    fXYDataset = xyDataset;
    fAxis = axis;
  }

  public DataDisplay createCopy(String name) {
    try {
      TimeSeries ts = ((TimeSeriesCollection) fXYDataset).getSeries(0);
      ts.setKey(name);
      TimeSeriesCollection tsc = new TimeSeriesCollection(ts);
      DataDisplay dd = new DataDisplay(tsc, (NumberAxis) fAxis.clone());
      return dd;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }
  
  public String toString() {
    return ((TimeSeriesCollection) fXYDataset).getSeries(0).getKey() + ":" + fAxis.getLabel();
  }
}

class SessionInfo {
  String                  fId;
  Date                    fStart;
  Date                    fEnd;
  List<Node>              fNodeList;
  Map<Node, List<String>> fNodeStatsMap;
  List<String>            fStatList;
  Map<String, List<Node>> fStatNodesMap;

  SessionInfo(String id) {
    fId = id;
    fNodeStatsMap = new HashMap<Node, List<String>>();
    fStatNodesMap = new HashMap<String, List<Node>>();
  }

  public String toString() {
    return fId;
  }
}

class MyStatisticsStore extends H2StatisticsStoreImpl {
  private final static String       SQL_GET_NODES          = "SELECT agentIp, agentDifferentiator FROM statisticlog WHERE sessionid = ? GROUP BY agentIp, agentDifferentiator ORDER BY agentIp ASC";
  private final static String       SQL_GET_STATS          = "SELECT statname FROM statisticlog WHERE sessionid = ? GROUP BY statname ORDER BY statname ASC";
  private final static String       SQL_GET_STATS_FOR_NODE = "SELECT statname FROM statisticlog WHERE sessionid = ? AND agentIp = ? AND agentDifferentiator = ? GROUP BY statname ORDER BY statname ASC";
  private final static String       SQL_GET_NODES_FOR_STAT = "SELECT agentIp, agentDifferentiator FROM statisticlog WHERE sessionid = ? AND statname = ? GROUP BY agentIp, agentDifferentiator ORDER BY agentIp ASC";

  private static final List<String> STATS_NON_GRATIS       = new ArrayList<String>();

  static {
    STATS_NON_GRATIS.addAll(Arrays.asList(new String[] { "startup timestamp", "shutdown timestamp",
      "system properties", "cpu idle", "cpu nice", "cpu wait", "cpu sys", "cpu user", "memory free" }));
  }

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

    results.removeAll(STATS_NON_GRATIS);

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

    results.removeAll(STATS_NON_GRATIS);

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