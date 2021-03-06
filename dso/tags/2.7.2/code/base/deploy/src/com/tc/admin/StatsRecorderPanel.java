/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlOptions;
import org.dijon.Button;
import org.dijon.CheckBox;
import org.dijon.ContainerResource;
import org.dijon.List;
import org.dijon.Spinner;
import org.dijon.ToggleButton;

import EDU.oswego.cs.dl.util.concurrent.misc.SwingWorker;

import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.ProgressDialog;
import com.tc.admin.common.XContainer;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererAlreadyConnectedException;
import com.tc.statistics.retrieval.actions.SRAThreadDump;
import com.terracottatech.config.TcStatsConfigDocument;
import com.terracottatech.config.TcStatsConfigDocument.TcStatsConfig;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

public class StatsRecorderPanel extends XContainer implements PropertyChangeListener, ClientConnectionListener {
  private AdminClientContext           m_acc;
  private StatsRecorderNode            m_statsRecorderNode;

  private StatisticsGathererListener   m_statsGathererListener;
  private ToggleButton                 m_startGatheringStatsButton;
  private ToggleButton                 m_stopGatheringStatsButton;
  private List                         m_statsSessionsList;
  private DefaultListModel             m_statsSessionsListModel;
  private XContainer                   m_availableStatsArea;
  private HashMap<String, JCheckBox>   m_statsControls;
  private CheckBox                     m_selectAllToggle;
  private Spinner                      m_samplePeriodSpinner;
  private Button                       m_importStatsConfigButton;
  private Button                       m_exportStatsConfigButton;
  private StatisticsLocalGathererMBean m_statisticsGathererMBean;
  private String                       m_currentStatsSessionId;
  private boolean                      m_isRecording;
  private Button                       m_exportStatsButton;
  private Button                       m_viewStatsButton;
  private File                         m_lastExportDir;
  private Button                       m_clearStatsSessionButton;
  private Button                       m_clearAllStatsSessionsButton;
  private boolean                      m_haveClientStats;

  private static final int             DEFAULT_STATS_POLL_PERIOD_SECONDS = 2;
  private static final String          DEFAULT_STATS_CONFIG_FILENAME     = "tc-stats-config.xml";

  public StatsRecorderPanel(StatsRecorderNode statsRecorderNode) {
    super();

    m_acc = AdminClient.getContext();
    m_statsRecorderNode = statsRecorderNode;

    load((ContainerResource) m_acc.getComponent("StatsRecorderPanel"));

    m_startGatheringStatsButton = (ToggleButton) findComponent("StartGatheringStatsButton");
    m_startGatheringStatsButton.addActionListener(new StartGatheringStatsAction());

    m_stopGatheringStatsButton = (ToggleButton) findComponent("StopGatheringStatsButton");
    m_stopGatheringStatsButton.addActionListener(new StopGatheringStatsAction());

    m_statsSessionsList = (List) findComponent("StatsSessionsList");
    m_statsSessionsList.addListSelectionListener(new StatsSessionsListSelectionListener());
    m_statsSessionsList.setModel(m_statsSessionsListModel = new DefaultListModel());
    m_statsSessionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    m_availableStatsArea = (XContainer) findComponent("AvailableStatsArea");
    m_availableStatsArea.setLayout(new GridLayout(0, 2));

    m_selectAllToggle = (CheckBox) findComponent("SelectAllToggle");
    m_selectAllToggle.addActionListener(new SelectAllHandler());

    m_samplePeriodSpinner = (Spinner) findComponent("SamplePeriodSpinner");
    m_samplePeriodSpinner.setModel(new SpinnerNumberModel(Integer.valueOf(DEFAULT_STATS_POLL_PERIOD_SECONDS), Integer
        .valueOf(1), null, Integer.valueOf(1)));

    m_importStatsConfigButton = (Button) findComponent("ImportStatsConfigButton");
    m_importStatsConfigButton.addActionListener(new ImportStatsConfigHandler());

    m_exportStatsConfigButton = (Button) findComponent("ExportStatsConfigButton");
    m_exportStatsConfigButton.addActionListener(new ExportStatsConfigHandler());

    m_exportStatsButton = (Button) findComponent("ExportStatsButton");
    m_exportStatsButton.addActionListener(new ExportStatsHandler());

    m_clearStatsSessionButton = (Button) findComponent("ClearStatsSessionButton");
    m_clearStatsSessionButton.addActionListener(new ClearStatsSessionHandler());

    m_clearAllStatsSessionsButton = (Button) findComponent("ClearAllStatsSessionsButton");
    m_clearAllStatsSessionsButton.addActionListener(new ClearAllStatsSessionsHandler());

    m_viewStatsButton = (Button) findComponent("ViewStatsSessionsButton");
    m_viewStatsButton.addActionListener(new ViewStatsSessionsHandler());

    setVisible(false);

    IClusterModel clusterModel = statsRecorderNode.getClusterModel();
    if (clusterModel.isActive()) {
      initiateStatsGathererConnectWorker();
    }
    clusterModel.addPropertyChangeListener(this);
    IClient[] clients = clusterModel.getClients();
    m_haveClientStats = clients.length > 0;
    if (!m_haveClientStats) {
      clusterModel.addClientConnectionListener(this);
    }
  }

  public void propertyChange(PropertyChangeEvent evt) {
    if (IClusterModel.PROP_ACTIVE_SERVER.equals(evt.getPropertyName())) {
      if (((IClusterModel) evt.getSource()).getActiveServer() != null) {
        initiateStatsGathererConnectWorker();
      }
    }
  }

  private void initiateStatsGathererConnectWorker() {
    m_acc.execute(new StatsGathererConnectWorker());
  }

  void testTriggerThreadDumpSRA() {
    if (isRecording()) {
      m_acc.submit(new Runnable() {
        public void run() {
          m_statisticsGathererMBean.captureStatistic(SRAThreadDump.ACTION_NAME);
        }
      });
    }
  }

  class StatsGathererConnectWorker extends BasicWorker<Void> {
    StatsGathererConnectWorker() {
      super(new Callable() {
        public Void call() throws Exception {
          ConnectionContext cc = m_statsRecorderNode.getConnectionContext();
          m_statisticsGathererMBean = m_statsRecorderNode.getStatisticsGathererMBean();
          if (m_statsGathererListener == null) {
            m_statsGathererListener = new StatisticsGathererListener();
          }
          cc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATHERER, m_statsGathererListener);

          if (m_statisticsGathererMBean.isActive()) {
            m_statisticsGathererMBean.startup();
          } else {
            throw new RuntimeException("Statistics subsystem not active.");
          }
          return null;
        }
      });
    }

    private boolean isAlreadyConnectedException(Throwable t) {
      return ExceptionHelper.getCauseOfType(t, StatisticsGathererAlreadyConnectedException.class) != null;
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        if (isAlreadyConnectedException(e)) {
          gathererConnected();
        } else {
          m_statsRecorderNode.makeUnavailable();
        }
      }
    }
  }

  private void gathererConnected() {
    m_statsSessionsListModel.clear();
    m_acc.execute(new GathererConnectedWorker());
  }

  private static class GathererConnectedState {
    private boolean  fIsCapturing;
    private String[] fSessions;
    private String   fActiveStatsSessionId;
    private String[] fSupportedStats;

    GathererConnectedState(boolean isCapturing, String[] sessions, String activeSessionId, String[] supportedStats) {
      fIsCapturing = isCapturing;
      fSessions = sessions;
      fActiveStatsSessionId = activeSessionId;
      fSupportedStats = supportedStats;
    }

    boolean isCapturing() {
      return fIsCapturing;
    }

    String[] getAllSessions() {
      return fSessions;
    }

    String getActiveStatsSessionId() {
      return fActiveStatsSessionId;
    }

    String[] getSupportedStats() {
      return fSupportedStats;
    }
  }

  class GathererConnectedWorker extends BasicWorker<GathererConnectedState> {
    GathererConnectedWorker() {
      super(new Callable<GathererConnectedState>() {
        public GathererConnectedState call() {
          boolean isCapturing = m_statisticsGathererMBean.isCapturing();
          String[] allSessions = m_statisticsGathererMBean.getAvailableSessionIds();
          String activeSessionId = m_statisticsGathererMBean.getActiveSessionId();
          String[] supportedStats = m_statisticsGathererMBean.getSupportedStatistics();

          return new GathererConnectedState(isCapturing, allSessions, activeSessionId, supportedStats);
        }
      });
    }

    public void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          m_acc.log(e);
        }
      } else {
        GathererConnectedState connectedState = getResult();
        String[] allSessions = connectedState.getAllSessions();
        String[] supportedStats = connectedState.getSupportedStats();
        boolean sessionInProgress = connectedState.isCapturing();

        m_currentStatsSessionId = connectedState.getActiveStatsSessionId();
        for (int i = 0; i < allSessions.length; i++) {
          String sessionId = allSessions[i];
          if (sessionInProgress && sessionId.equals(m_currentStatsSessionId)) {
            continue;
          }
          m_statsSessionsListModel.addElement(new StatsSessionListItem(sessionId));
        }
        boolean haveAnySessions = allSessions.length > 0;
        m_clearAllStatsSessionsButton.setEnabled(haveAnySessions);
        m_exportStatsButton.setEnabled(haveAnySessions);
        m_viewStatsButton.setEnabled(haveAnySessions);
        m_statsGathererListener.init(sessionInProgress);
        setupStatsConfigPanel(supportedStats);
        setVisible(true);
      }
    }
  }

  class StatisticsGathererListener implements NotificationListener {
    private void init(boolean isRecording) {
      if (isRecording) {
        showRecordingInProgress();
      } else {
        hideRecordingInProgress();
      }
    }

    private void setRecording(boolean recording) {
      m_isRecording = recording;

      m_startGatheringStatsButton.setSelected(recording);
      m_startGatheringStatsButton.setEnabled(!recording);

      m_stopGatheringStatsButton.setSelected(!recording);
      m_stopGatheringStatsButton.setEnabled(recording);

      updateSessionsControls();
      m_statsRecorderNode.showRecording(recording);
    }

    private void showRecordingInProgress() {
      setRecording(true);
    }

    private void hideRecordingInProgress() {
      setRecording(false);
    }

    public void handleNotification(final Notification notification, final Object handback) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          String type = notification.getType();
          Object userData = notification.getUserData();

          if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_STARTEDUP_TYPE)) {
            gathererConnected();
            return;
          }

          if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_SESSION_CREATED_TYPE)) {
            m_currentStatsSessionId = (String) userData;
            return;
          }

          if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_CAPTURING_STARTED_TYPE)) {
            showRecordingInProgress();
            return;
          }

          if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_CAPTURING_STOPPED_TYPE)) {
            String thisSession = (String) userData;
            if (m_currentStatsSessionId != null && m_currentStatsSessionId.equals(thisSession)) {
              m_statsSessionsListModel.addElement(new StatsSessionListItem(thisSession));
              m_statsSessionsList.setSelectedIndex(m_statsSessionsListModel.getSize() - 1);
              m_currentStatsSessionId = null;
              hideRecordingInProgress();
              return;
            }
          }

          if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_SESSION_CLEARED_TYPE)) {
            String sessionId = (String) userData;
            int sessionCount = m_statsSessionsListModel.getSize();
            for (int i = 0; i < sessionCount; i++) {
              StatsSessionListItem item = (StatsSessionListItem) m_statsSessionsListModel.elementAt(i);
              if (sessionId.equals(item.getSessionId())) {
                m_statsSessionsListModel.remove(i);
                break;
              }
            }
            return;
          }

          if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_ALLSESSIONS_CLEARED_TYPE)) {
            m_statsSessionsListModel.clear();
            m_currentStatsSessionId = null;
            return;
          }
        }
      });
    }
  }

  private void setupStatsConfigPanel(String[] stats) {
    m_availableStatsArea.removeAll();
    Map<String, Boolean> selectedStates = new HashMap<String, Boolean>();
    if (m_statsControls == null) {
      m_statsControls = new HashMap<String, JCheckBox>();
    } else {
      Iterator<String> statIter = m_statsControls.keySet().iterator();
      while (statIter.hasNext()) {
        String stat = statIter.next();
        JCheckBox control = m_statsControls.get(stat);
        if (control != null) {
          selectedStates.put(stat, control.isSelected());
        }
      }
      m_statsControls.clear();
    }
    if (stats != null) {
      for (String stat : stats) {
        JCheckBox control = new JCheckBox();
        control.setText(stat);
        control.setName(stat);
        m_statsControls.put(stat, control);
        m_availableStatsArea.add(control);
        Boolean state = selectedStates.get(stat);
        if (state == null) {
          state = m_selectAllToggle.isSelected();
        }
        control.setSelected(state);
      }
    }
    m_availableStatsArea.revalidate();
    m_availableStatsArea.repaint();
  }

  private class SelectAllHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      boolean selected = m_selectAllToggle.isSelected();
      Iterator<JCheckBox> iter = m_statsControls.values().iterator();
      while (iter.hasNext()) {
        iter.next().setSelected(selected);
      }
    }
  }

  private long getSamplePeriodMillis() {
    Number samplePeriod = (Number) m_samplePeriodSpinner.getValue();
    return samplePeriod.longValue() * 1000;
  }

  private java.util.List<String> getSelectedStats() {
    Iterator<String> iter = m_statsControls.keySet().iterator();
    ArrayList<String> statList = new ArrayList<String>();
    while (iter.hasNext()) {
      String stat = iter.next();
      JCheckBox control = m_statsControls.get(stat);
      if (control.isSelected()) {
        statList.add(stat);
      }
    }
    return statList;
  }

  private void disableAllStats() {
    Iterator<String> iter = m_statsControls.keySet().iterator();
    while (iter.hasNext()) {
      JCheckBox control = m_statsControls.get(iter.next());
      if (control != null) {
        control.setSelected(false);
      }
    }
  }

  private void setSelectedStats(String[] stats) {
    disableAllStats();
    for (String stat : stats) {
      JCheckBox control = m_statsControls.get(stat);
      if (control != null) {
        control.setSelected(true);
      }
    }
  }

  class StartGatheringStatsAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      java.util.List<String> statList = getSelectedStats();
      String[] stats = statList.toArray(new String[0]);
      long samplePeriodMillis = getSamplePeriodMillis();

      m_acc.execute(new StartGatheringStatsWorker(stats, samplePeriodMillis));
    }
  }

  class StartGatheringStatsWorker extends BasicWorker<Void> {
    StartGatheringStatsWorker(final String[] stats, final long samplePeriodMillis) {
      super(new Callable<Void>() {
        public Void call() {
          m_currentStatsSessionId = new Date().toString();
          m_statisticsGathererMBean.createSession(m_currentStatsSessionId);
          m_statisticsGathererMBean.enableStatistics(stats);
          m_statisticsGathererMBean.setSessionParam(StatisticsConfig.KEY_RETRIEVER_SCHEDULE_INTERVAL, Long
              .valueOf(samplePeriodMillis));
          m_statisticsGathererMBean.startCapturing();
          return null;
        }
      });
    }

    public void finished() {
      m_stopGatheringStatsButton.setSelected(false);
      m_statsRecorderNode.notifyChanged();
    }
  }

  class StopGatheringStatsAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      m_startGatheringStatsButton.setSelected(false);
      m_acc.execute(new StopGatheringStatsWorker());
    }
  }

  class StopGatheringStatsWorker extends BasicWorker<Void> {
    StopGatheringStatsWorker() {
      super(new Callable<Void>() {
        public Void call() {
          m_statisticsGathererMBean.stopCapturing();
          m_statisticsGathererMBean.closeSession();
          return null;
        }
      });
    }

    public void finished() {
      m_statsRecorderNode.notifyChanged();
    }
  }

  private void updateSessionsControls() {
    boolean haveAnySessions = m_statsSessionsListModel.getSize() > 0;
    boolean haveSelectedSession = getSelectedSessionId() != null;
    boolean recording = isRecording();
    m_exportStatsButton.setEnabled(haveAnySessions);
    m_clearStatsSessionButton.setEnabled(haveSelectedSession);
    m_clearAllStatsSessionsButton.setEnabled(haveAnySessions && !recording);
    m_viewStatsButton.setEnabled(haveAnySessions);
  }

  private class StatsSessionsListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      updateSessionsControls();
    }
  }

  private static class StatsSessionListItem {
    private String fSessionId;

    StatsSessionListItem(String sessionId) {
      fSessionId = sessionId;
    }

    String getSessionId() {
      return fSessionId;
    }

    public String toString() {
      return fSessionId;
    }
  }

  public boolean isRecording() {
    return m_isRecording;
  }

  private String getSelectedSessionId() {
    if (m_statsSessionsList == null) return null;
    StatsSessionListItem item = (StatsSessionListItem) m_statsSessionsList.getSelectedValue();
    return item != null ? item.getSessionId() : null;
  }

  private class ImportStatsConfigHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      JFileChooser chooser = new JFileChooser();
      if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
      chooser.setDialogTitle("Import statistics configuration");
      chooser.setMultiSelectionEnabled(false);
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), DEFAULT_STATS_CONFIG_FILENAME));
      if (chooser.showOpenDialog(StatsRecorderPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      if (!file.exists()) {
        Frame frame = (Frame) getAncestorOfClass(Frame.class);
        String msg = "File '" + file + "' does not exist.";
        JOptionPane.showMessageDialog(StatsRecorderPanel.this, msg, frame.getTitle(), JOptionPane.WARNING_MESSAGE);
        return;
      }
      m_lastExportDir = file.getParentFile();
      try {
        TcStatsConfigDocument tcStatsConfigDoc = TcStatsConfigDocument.Factory.parse(file);
        TcStatsConfig tcStatsConfig = tcStatsConfigDoc.getTcStatsConfig();
        if (tcStatsConfig.isSetRetrievalPollPeriod()) {
          m_samplePeriodSpinner.setValue(tcStatsConfig.getRetrievalPollPeriod().longValue() / 1000);
        }
        if (tcStatsConfig.isSetEnabledStatistics()) {
          setSelectedStats(tcStatsConfig.getEnabledStatistics().getNameArray());
        }
      } catch (RuntimeException re) {
        throw re;
      } catch (Exception e) {
        Frame frame = (Frame) getAncestorOfClass(Frame.class);
        String msg = "Unable to parse '" + file.getName() + "' as a Terracotta stats config document";
        JOptionPane.showMessageDialog(StatsRecorderPanel.this, msg, frame.getTitle(), JOptionPane.ERROR_MESSAGE);
        return;
      }
    }
  }

  private class ExportStatsConfigHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      JFileChooser chooser = new JFileChooser();
      if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
      chooser.setDialogTitle("Export statistics configuration");
      chooser.setMultiSelectionEnabled(false);
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), DEFAULT_STATS_CONFIG_FILENAME));
      if (chooser.showSaveDialog(StatsRecorderPanel.this) != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      m_lastExportDir = file.getParentFile();
      java.util.List<String> statList = getSelectedStats();
      InputStream is = null;
      OutputStream os = null;
      try {
        TcStatsConfigDocument tcStatsConfigDoc = TcStatsConfigDocument.Factory.newInstance();
        TcStatsConfig tcStatsConfig = tcStatsConfigDoc.addNewTcStatsConfig();
        tcStatsConfig.setRetrievalPollPeriod(BigInteger.valueOf(getSamplePeriodMillis()));
        tcStatsConfig.addNewEnabledStatistics().setNameArray(statList.toArray(new String[0]));
        XmlOptions opts = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
        is = tcStatsConfigDoc.newInputStream(opts);
        os = new FileOutputStream(file);
        IOUtils.copy(is, os);
      } catch (IOException ioe) {
        m_acc.log(ioe);
      } finally {
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);
      }
    }
  }

  private static class ZipFileFilter extends FileFilter {
    public boolean accept(File file) {
      return file.isDirectory() || file.getName().endsWith(".zip");
    }

    public String getDescription() {
      return "ZIP files";
    }
  }

  class ExportStatsHandler implements ActionListener {
    private UsernamePasswordCredentials m_credentials;

    private JFileChooser createFileChooser() {
      JFileChooser chooser = new JFileChooser();
      if (m_lastExportDir != null) chooser.setCurrentDirectory(m_lastExportDir);
      chooser.setDialogTitle("Export statistics");
      chooser.setMultiSelectionEnabled(false);
      chooser.setFileFilter(new ZipFileFilter());
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "tc-stats.zip"));
      return chooser;
    }

    public void actionPerformed(ActionEvent ae) {
      if (m_statsSessionsListModel.getSize() == 0) return;
      JFileChooser chooser = createFileChooser();
      if (chooser.showSaveDialog(StatsRecorderPanel.this) != JFileChooser.APPROVE_OPTION) return;
      final File file = chooser.getSelectedFile();
      m_lastExportDir = file.getParentFile();
      GetMethod get = null;
      try {
        String uri = m_statsRecorderNode.getStatsExportServletURI();
        URL url = new URL(uri);
        HttpClient httpClient = new HttpClient();
        get = new GetMethod(url.toString());
        get.setFollowRedirects(true);
        if (m_credentials != null) {
          httpClient.getState().setCredentials(m_statsRecorderNode.getAuthScope(), m_credentials);
          get.setDoAuthentication(true);
        }
        int status = httpClient.executeMethod(get);
        while (status == HttpStatus.SC_UNAUTHORIZED) {
          UsernamePasswordCredentials creds = getCredentials();
          if (creds == null) return;
          if (creds.getUserName().length() == 0 || creds.getPassword().length() == 0) {
            m_credentials = null;
            continue;
          }
          httpClient = new HttpClient();
          httpClient.getState().setCredentials(m_statsRecorderNode.getAuthScope(), creds);
          get.setDoAuthentication(true);
          status = httpClient.executeMethod(get);
        }
        if (status != HttpStatus.SC_OK) {
          m_acc.log("The http client has encountered a status code other than ok for the url: " + url + " status: "
                    + HttpStatus.getStatusText(status));
          return;
        }
        Frame frame = (Frame) getAncestorOfClass(Frame.class);
        final ProgressDialog progressDialog = showProgressDialog(frame, "Exporting statistics to '" + file.getName()
                                                                        + ".' Please wait...");
        final GetMethod get2 = get;
        progressDialog.addWindowListener(new WindowAdapter() {
          public void windowOpened(WindowEvent e) {
            new Thread(new StreamCopierRunnable(get2, file, progressDialog)).start();
          }
        });
      } catch (Exception e) {
        m_acc.log(e);
        if (get != null) {
          get.releaseConnection();
        }
      }
    }

    private UsernamePasswordCredentials getCredentials() {
      if (m_credentials != null) return m_credentials;
      Frame frame = (Frame) getAncestorOfClass(Frame.class);
      LoginPanel loginPanel = new LoginPanel();
      int answer = JOptionPane.showConfirmDialog(frame, loginPanel, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
      if (answer == JOptionPane.OK_OPTION) {
        m_credentials = new UsernamePasswordCredentials(loginPanel.getUserName(), loginPanel.getPassword());
      } else {
        m_credentials = null;
      }
      return m_credentials;
    }
  }

  class StreamCopierRunnable implements Runnable {
    GetMethod      fGetMethod;
    File           fOutFile;
    ProgressDialog fProgressDialog;

    StreamCopierRunnable(GetMethod getMethod, File outFile, ProgressDialog progressDialog) {
      fGetMethod = getMethod;
      fOutFile = outFile;
      fProgressDialog = progressDialog;
    }

    public void run() {
      FileOutputStream out = null;

      try {
        out = new FileOutputStream(fOutFile);
        InputStream in = fGetMethod.getResponseBodyAsStream();

        byte[] buffer = new byte[1024 * 8];
        int count;
        try {
          while ((count = in.read(buffer)) >= 0) {
            out.write(buffer, 0, count);
          }
        } finally {
          SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
              fProgressDialog.setVisible(false);
              m_acc.setStatus("Wrote '" + fOutFile.getAbsolutePath() + "'");
            }
          });
          IOUtils.closeQuietly(in);
          IOUtils.closeQuietly(out);
        }
      } catch (Exception e) {
        m_acc.log(e);
      } finally {
        IOUtils.closeQuietly(out);
        fGetMethod.releaseConnection();
      }
    }
  }

  class ClearStatsSessionHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      final StatsSessionListItem item = (StatsSessionListItem) m_statsSessionsList.getSelectedValue();
      if (item != null) {
        String msg = "Really clear statistics from session '" + item + "?'";
        Frame frame = (Frame) StatsRecorderPanel.this.getAncestorOfClass(Frame.class);
        int result = JOptionPane.showConfirmDialog(StatsRecorderPanel.this, msg, frame.getTitle(),
                                                   JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
          final ProgressDialog progressDialog = showProgressDialog(frame, "Clearing statistics from session '" + item
                                                                          + ".' Please wait...");
          SwingWorker worker = new SwingWorker() {
            public Object construct() throws Exception {
              m_statisticsGathererMBean.clearStatistics(item.getSessionId());
              return null;
            }

            public void finished() {
              progressDialog.setVisible(false);
              InvocationTargetException ite = getException();
              if (ite != null) {
                Throwable cause = ite.getCause();
                m_acc.log(cause != null ? cause : ite);
                return;
              }
            }
          };
          worker.start();
        }
      }
    }
  }

  class ClearAllStatsSessionsHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String msg = "Really clear all recorded statistics?";
      Frame frame = (Frame) StatsRecorderPanel.this.getAncestorOfClass(Frame.class);
      int result = JOptionPane.showConfirmDialog(StatsRecorderPanel.this, msg, frame.getTitle(),
                                                 JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        final ProgressDialog progressDialog = showProgressDialog(frame,
                                                                 "Clearing all recorded statistics. Please wait...");
        SwingWorker worker = new SwingWorker() {
          public Object construct() throws Exception {
            m_statisticsGathererMBean.clearAllStatistics();
            return null;
          }

          public void finished() {
            progressDialog.setVisible(false);
            InvocationTargetException ite = getException();
            if (ite != null) {
              Throwable cause = ite.getCause();
              m_acc.log(cause != null ? cause : ite);
              return;
            }
          }
        };
        worker.start();
      }
    }
  }

  private ProgressDialog showProgressDialog(Frame owner, String msg) {
    ProgressDialog progressDialog = new ProgressDialog(owner, "Statistics recorder", msg);
    progressDialog.pack();
    progressDialog.center(owner);
    progressDialog.setVisible(true);
    return progressDialog;
  }

  class ViewStatsSessionsHandler implements ActionListener, PropertyChangeListener {
    private JFrame  m_svtFrame;
    private Method  m_retrieveMethod;
    private Method  m_setSessionMethod;
    private boolean m_shouldLogErrors = true;

    public void actionPerformed(ActionEvent ae) {
      if (m_svtFrame == null) {
        AdminClientPanel topPanel = (AdminClientPanel) getAncestorOfClass(AdminClientPanel.class);
        if ((m_svtFrame = topPanel.getSVTFrame()) != null) {
          m_svtFrame.addPropertyChangeListener("newStore", this);
        } else {
          // AdminClientPanel.getSVTFrame will open the user's browser on the website page with
          // download instructions.
          return;
        }
      }
      if (m_retrieveMethod == null) {
        try {
          m_retrieveMethod = m_svtFrame.getClass().getMethod("retrieveFromAddress", String.class);
        } catch (Exception e) {
          log(e);
        }
      }
      m_svtFrame.setVisible(true);
      if (m_retrieveMethod != null) {
        try {
          String addr = m_statsRecorderNode.getActiveServerAddress();
          m_retrieveMethod.invoke(m_svtFrame, addr);
        } catch (Exception e) {
          log(e);
        }
      }
    }

    public void propertyChange(PropertyChangeEvent evt) {
      String name = evt.getPropertyName();
      String selectedSession = getSelectedSessionId();
      if ("newStore".equals(name) && selectedSession != null) {
        if (m_setSessionMethod == null) {
          try {
            m_setSessionMethod = m_svtFrame.getClass().getMethod("setSession", String.class);
          } catch (Exception e) {
            log(e);
          }
        }
        if (m_setSessionMethod != null) {
          try {
            m_setSessionMethod.invoke(m_svtFrame, selectedSession);
          } catch (Exception e) {
            log(e);
          }
        }
      }
    }

    private void log(Throwable t) {
      if (m_shouldLogErrors) {
        m_acc.log(t);
      }
    }
  }

  public void tearDown() {
    IClusterModel clusterModel = m_statsRecorderNode.getClusterModel();
    if (clusterModel != null) {
      clusterModel.removePropertyChangeListener(this);
    }

    super.tearDown();

    m_availableStatsArea.tearDown();

    m_acc = null;
    m_statsGathererListener = null;
    m_startGatheringStatsButton = null;
    m_stopGatheringStatsButton = null;
    m_statsSessionsList = null;
    m_statsSessionsListModel = null;
    m_availableStatsArea = null;
    m_statsControls = null;
    m_samplePeriodSpinner = null;
    m_importStatsConfigButton = null;
    m_exportStatsConfigButton = null;
    m_statisticsGathererMBean = null;
    m_currentStatsSessionId = null;
    m_exportStatsButton = null;
    m_viewStatsButton = null;
    m_lastExportDir = null;
    m_clearStatsSessionButton = null;
    m_clearAllStatsSessionsButton = null;
  }

  private static class LoginPanel extends JPanel {
    private JTextField     m_userNameField;
    private JPasswordField m_passwordField;

    private LoginPanel() {
      super();
      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridx = gbc.gridy = 0;
      gbc.insets = new Insets(2, 2, 2, 2);
      add(new JLabel("Username:"), gbc);
      gbc.gridx++;
      m_userNameField = new JTextField(20);
      add(m_userNameField, gbc);
      gbc.gridx--;
      gbc.gridy++;
      add(new JLabel("Password:"), gbc);
      gbc.gridx++;
      m_passwordField = new JPasswordField(20);
      add(m_passwordField, gbc);

      m_userNameField.requestFocusInWindow();
    }

    String getUserName() {
      return m_userNameField.getText();
    }

    String getPassword() {
      return new String(m_passwordField.getPassword());
    }
  }

  class UpdateSupportedStatsWorker extends BasicWorker<String[]> {
    UpdateSupportedStatsWorker() {
      super(new Callable<String[]>() {
        public String[] call() {
          return m_statisticsGathererMBean.getSupportedStatistics();
        }
      });
    }

    public void finished() {
      Exception e = getException();
      if (e == null) {
        setupStatsConfigPanel(getResult());
      }
    }
  }

  public void clientConnected(IClient client) {
    if (m_statisticsGathererMBean != null && !m_haveClientStats) {
      m_acc.execute(new UpdateSupportedStatsWorker());
      m_haveClientStats = true;
    }
  }

  public void clientDisconnected(IClient client) {
    IClusterModel clusterModel = m_statsRecorderNode.getClusterModel();
    if (clusterModel != null) {
      IClient[] clients = clusterModel.getClients();
      m_haveClientStats = clients.length > 0;
      if (m_statisticsGathererMBean != null && !m_haveClientStats) {
        m_acc.execute(new UpdateSupportedStatsWorker());
      }
    }
  }
}
