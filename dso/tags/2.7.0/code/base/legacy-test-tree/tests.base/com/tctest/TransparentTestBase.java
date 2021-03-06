/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.lang.ClassUtils;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.simulator.app.ApplicationConfigBuilder;
import com.tc.simulator.app.ErrorContext;
import com.tc.test.ProcessInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.activepassive.ActivePassiveServerConfigCreator;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.test.restart.RestartTestHelper;
import com.tc.test.restart.ServerCrasher;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.runtime.ThreadDump;
import com.tctest.runner.DistributedTestRunner;
import com.tctest.runner.DistributedTestRunnerConfig;
import com.tctest.runner.TestGlobalIdGenerator;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.AssertionFailedError;

public abstract class TransparentTestBase extends BaseDSOTestCase implements TransparentTestIface, TestConfigurator {

  public static final int               DEFAULT_CLIENT_COUNT            = 2;
  public static final int               DEFAULT_INTENSITY               = 10;
  public static final int               DEFAULT_VALIDATOR_COUNT         = 0;
  public static final int               DEFAULT_ADAPTED_MUTATOR_COUNT   = 0;
  public static final int               DEFAULT_ADAPTED_VALIDATOR_COUNT = 0;

  protected DistributedTestRunner       runner;

  private DistributedTestRunnerConfig   runnerConfig                    = new DistributedTestRunnerConfig(
                                                                                                          getTimeoutValueInSeconds());
  private TransparentAppConfig          transparentAppConfig;
  private ApplicationConfigBuilder      possibleApplicationConfigBuilder;

  private String                        mode;
  private ServerControl                 serverControl;
  private boolean                       controlledCrashMode             = false;
  private ServerCrasher                 crasher;
  private File                          javaHome;
  private int                           pid                             = -1;
  private final ProxyConnectManager     proxyMgr                        = new ProxyConnectManagerImpl();

  // for active-passive tests
  private ActivePassiveServerManager    apServerManager;
  private ActivePassiveTestSetupManager apSetupManager;
  private TestState                     crashTestState;

  // used by ResolveTwoActiveServersTest only
  private ServerControl[]               serverControls                  = null;
  private TCPProxy[]                    proxies                         = null;

  private int                           dsoPort                         = -1;
  private int                           adminPort                       = -1;

  protected TestConfigObject getTestConfigObject() {
    return TestConfigObject.getInstance();
  }

  protected void setJavaHome() {
    if (javaHome == null) {
      String javaHome_local = getTestConfigObject().getL2StartupJavaHome();
      if (javaHome_local == null) { throw new IllegalStateException(TestConfigObject.L2_STARTUP_JAVA_HOME
                                                                    + " must be set to a valid JAVA_HOME"); }
      javaHome = new File(javaHome_local);
    }
  }

  protected void setJvmArgsL1Reconnect(final ArrayList jvmArgs) {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED + "=true");
  }

  protected void setJvmArgsL2Reconnect(final ArrayList jvmArgs) {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "true");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "true");

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED + "=true");
  }

  protected void setJvmArgsCvtIsolation(final ArrayList jvmArgs) {
    final String buffer_randomsuffix_sysprop = TCPropertiesImpl
        .tcSysProp(TCPropertiesConsts.CVT_BUFFER_RANDOM_SUFFIX_ENABLED);
    final String store_randomsuffix_sysprop = TCPropertiesImpl
        .tcSysProp(TCPropertiesConsts.CVT_STORE_RANDOM_SUFFIX_ENABLED);
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.CVT_BUFFER_RANDOM_SUFFIX_ENABLED, "true");
    tcProps.setProperty(TCPropertiesConsts.CVT_STORE_RANDOM_SUFFIX_ENABLED, "true");
    System.setProperty(buffer_randomsuffix_sysprop, "true");
    System.setProperty(store_randomsuffix_sysprop, "true");

    jvmArgs.add("-D" + buffer_randomsuffix_sysprop + "=true");
    jvmArgs.add("-D" + store_randomsuffix_sysprop + "=true");
  }

  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    // to be overwritten
  }

  protected void setUp() throws Exception {
    setUpTransparent(configFactory(), configHelper());

    // config should be set up before tc-config for external L2s are written out
    setupConfig(configFactory());

    if (!canSkipL1ReconnectCheck() && canRunL1ProxyConnect() && !enableL1Reconnect()) { throw new AssertionError(
                                                                                                                 "L1 proxy-connect needs l1reconnect enabled, please overwrite enableL1Reconnect()"); }

    if (canRunL2ProxyConnect() && !enableL2Reconnect()) { throw new AssertionError(
                                                                                   "L2 proxy-connect needs l2reconnect enabled, please overwrite enableL2Reconnect()"); }

    ArrayList jvmArgs = new ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    setJvmArgsCvtIsolation(jvmArgs);

    // for some test cases to enable l1reconnect
    if (enableL1Reconnect()) {
      setJvmArgsL1Reconnect(jvmArgs);
    }

    if (enableL2Reconnect()) {
      setJvmArgsL2Reconnect(jvmArgs);
    }

    setExtraJvmArgs(jvmArgs);

    RestartTestHelper helper = null;
    PortChooser portChooser = new PortChooser();
    if ((isCrashy() && canRunCrash()) || useExternalProcess()) {
      // javaHome is set here only to enforce that java home is defined in the test config
      // javaHome is set again inside RestartTestEnvironment because how that class is used
      // TODO: clean this up
      setJavaHome();

      helper = new RestartTestHelper(mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH),
                                     new RestartTestEnvironment(getTempDirectory(), portChooser,
                                                                RestartTestEnvironment.PROD_MODE, configFactory()),
                                     jvmArgs);
      dsoPort = helper.getServerPort();
      adminPort = helper.getAdminPort();
      ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(dsoPort);
      ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(adminPort);
      if (!canRunL1ProxyConnect()) configFactory().addServerToL1Config(null, dsoPort, adminPort);
      serverControl = helper.getServerControl();
    } else if (isActivePassive() && canRunActivePassive()) {
      setUpActivePassiveServers(portChooser, jvmArgs);
    } else {
      dsoPort = portChooser.chooseRandomPort();
      adminPort = portChooser.chooseRandomPort();
      ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(dsoPort);
      ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(adminPort);
      if (!canRunL1ProxyConnect()) configFactory().addServerToL1Config(null, dsoPort, -1);
    }

    if (canRunL1ProxyConnect()) {
      setupProxyConnect(helper, portChooser);
    }

    this.doSetUp(this);

    if (isCrashy() && canRunCrash()) {
      crashTestState = new TestState(false);
      crasher = new ServerCrasher(serverControl, helper.getServerCrasherConfig().getRestartInterval(), helper
          .getServerCrasherConfig().isCrashy(), crashTestState, proxyMgr);
      if (canRunL1ProxyConnect()) crasher.setProxyConnectMode(true);
      crasher.startAutocrash();
    }
  }

  protected int getDsoPort() {
    return dsoPort;
  }

  protected int getAdminPort() {
    return adminPort;
  }

  protected ProxyConnectManager getProxyConnectManager() {
    return this.proxyMgr;
  }

  private final void addTestTcPropertiesFile(List jvmArgs) {
    URL url = getClass().getResource("/com/tc/properties/tests.properties");
    if (url == null) {
      // System.err.println("\n\n ##### No tests.properties defined for this module \n\n");
      return;
    }
    String pathToTestTcProperties = url.getPath();
    if (pathToTestTcProperties == null || pathToTestTcProperties.equals("")) {
      // System.err.println("\n\n ##### No path to tests.properties defined \n\n");
      return;
    }
    // System.err.println("\n\n ##### -Dcom.tc.properties=" + pathToTestTcProperties + "\n\n");
    jvmArgs.add("-Dcom.tc.properties=" + pathToTestTcProperties);
  }

  private final void setUpActivePassiveServers(PortChooser portChooser, List jvmArgs) throws Exception {
    controlledCrashMode = true;
    setJavaHome();
    apSetupManager = new ActivePassiveTestSetupManager();
    setupActivePassiveTest(apSetupManager);
    apServerManager = new ActivePassiveServerManager(mode()
        .equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE), getTempDirectory(), portChooser,
                                                     ActivePassiveServerConfigCreator.DEV_MODE, apSetupManager,
                                                     javaHome, configFactory(), jvmArgs, canRunL2ProxyConnect());
    apServerManager.addServersToL1Config(configFactory());
    if (canRunL2ProxyConnect()) setupL2ProxyConnectTest(apServerManager.getL2ProxyManagers());
  }

  protected void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    throw new AssertionError("The sub-class (test) should override this method.");
  }

  protected void setupConfig(TestTVSConfigurationSetupManagerFactory configFactory) {
    // do nothing
  }

  protected File makeTmpDir(Class klass) {
    File tmp_dir_root = new File(getTestConfigObject().tempDirectoryRoot());
    File tmp_dir = new File(tmp_dir_root, ClassUtils.getShortClassName(klass));
    tmp_dir.mkdirs();
    return tmp_dir;
  }

  private final void setupProxyConnect(RestartTestHelper helper, PortChooser portChooser) throws Exception {
    dsoPort = 0;
    adminPort = 0;

    if (helper != null) {
      dsoPort = helper.getServerPort();
      adminPort = helper.getAdminPort();
      // for crash+proxy, set crash interval to 60 sec
      helper.getServerCrasherConfig().setRestartInterval(60 * 1000);
    } else if (isActivePassive() && canRunActivePassive()) {
      // not doing active-passive for proxy yet
      throw new AssertionError("Proxy-connect is yet not running with active-passive mode");
    } else {
      dsoPort = portChooser.chooseRandomPort();
      adminPort = portChooser.chooseRandomPort();
    }

    int dsoProxyPort = portChooser.chooseRandomPort();

    proxyMgr.setDsoPort(dsoPort);
    proxyMgr.setProxyPort(dsoProxyPort);
    proxyMgr.setupProxy();
    setupL1ProxyConnectTest(proxyMgr);

    ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(dsoPort);
    ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(adminPort);
    configFactory().addServerToL1Config(null, dsoProxyPort, -1);
  }

  protected void setupL1ProxyConnectTest(ProxyConnectManager mgr) {
    /*
     * subclass can overwrite to change the test parameters.
     */
    mgr.setProxyWaitTime(20 * 1000);
    mgr.setProxyDownTime(100);
  }

  protected void setupL2ProxyConnectTest(ProxyConnectManager[] managers) {
    /*
     * subclass can overwrite to change the test parameters.
     */
    for (int i = 0; i < managers.length; ++i) {
      managers[i].setProxyWaitTime(20 * 1000);
      managers[i].setProxyDownTime(100);
    }
  }

  protected boolean useExternalProcess() {
    return getTestConfigObject().isL2StartupModeExternal();
  }

  // only used by regular system tests (not crash or active-passive)
  protected final void setUpControlledServer(TestTVSConfigurationSetupManagerFactory factory,
                                             DSOClientConfigHelper helper, int serverPort, int adminPort,
                                             String configFile) throws Exception {
    setUpControlledServer(factory, helper, serverPort, adminPort, configFile, null);
  }

  protected final void setUpForMultipleExternalProcesses(TestTVSConfigurationSetupManagerFactory factory,
                                                         DSOClientConfigHelper helper, int[] dsoPorts, int[] jmxPorts,
                                                         int[] l2GroupPorts, int[] proxyPorts, String[] serverNames,
                                                         File[] configFiles) throws Exception {
    assertEquals(dsoPorts.length, 2);

    controlledCrashMode = true;
    setJavaHome();
    serverControls = new ServerControl[dsoPorts.length];

    if (proxyPorts != null) {
      proxies = new TCPProxy[2];
    }

    for (int i = 0; i < 2; i++) {
      if (proxies != null) {
        proxies[i] = new TCPProxy(proxyPorts[i], InetAddress.getLocalHost(), l2GroupPorts[i], 0L, false, new File("."));
        proxies[i].setReuseAddress(true);
      }
      List al = new ArrayList();
      al.add("-Dtc.node-name=" + serverNames[i]);
      serverControls[i] = new ExtraProcessServerControl("localhost", dsoPorts[i], jmxPorts[i], configFiles[i]
          .getAbsolutePath(), true, serverNames[i], null, javaHome, true);
    }
    setUpTransparent(factory, helper, true);
  }

  protected final void setUpControlledServer(TestTVSConfigurationSetupManagerFactory factory,
                                             DSOClientConfigHelper helper, int serverPort, int adminPort,
                                             String configFile, List jvmArgs) throws Exception {
    controlledCrashMode = true;
    if (jvmArgs == null) {
      jvmArgs = new ArrayList();
    }
    addTestTcPropertiesFile(jvmArgs);
    setUpExternalProcess(factory, helper, serverPort, adminPort, configFile, jvmArgs);
  }

  protected void setUpExternalProcess(TestTVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper,
                                      int serverPort, int adminPort, String configFile, List jvmArgs) throws Exception {
    setJavaHome();
    assertNotNull(jvmArgs);
    serverControl = new ExtraProcessServerControl("localhost", serverPort, adminPort, configFile, true, javaHome,
                                                  jvmArgs);
    setUpTransparent(factory, helper);

    ((SettableConfigItem) configFactory().l2DSOConfig().listenPort()).setValue(serverPort);
    ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(adminPort);
    configFactory().addServerToL1Config(null, serverPort, adminPort);
  }

  private final void setUpTransparent(TestTVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper)
      throws Exception {
    setUpTransparent(factory, helper, false);
  }

  private final void setUpTransparent(TestTVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper,
                                      boolean serverControlsSet) throws Exception {
    super.setUp(factory, helper);
    if (serverControlsSet) {
      transparentAppConfig = new TransparentAppConfig(getApplicationClass().getName(), new TestGlobalIdGenerator(),
                                                      DEFAULT_CLIENT_COUNT, DEFAULT_INTENSITY, serverControls, proxies);
    } else {
      transparentAppConfig = new TransparentAppConfig(getApplicationClass().getName(), new TestGlobalIdGenerator(),
                                                      DEFAULT_CLIENT_COUNT, DEFAULT_INTENSITY, serverControl,
                                                      DEFAULT_VALIDATOR_COUNT, DEFAULT_ADAPTED_MUTATOR_COUNT,
                                                      DEFAULT_ADAPTED_VALIDATOR_COUNT);
    }

    transparentAppConfig.setAttribute(TransparentAppConfig.PROXY_CONNECT_MGR, proxyMgr);
  }

  protected synchronized final String mode() {
    if (mode == null) {
      mode = getTestConfigObject().transparentTestsMode();
    }

    return mode;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    // Nothing here, by default
  }

  private boolean isCrashy() {
    return TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH.equals(mode());
  }

  private boolean isActivePassive() {
    return TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE.equals(mode());
  }

  public DistributedTestRunnerConfig getRunnerConfig() {
    return this.runnerConfig;
  }

  public void setApplicationConfigBuilder(ApplicationConfigBuilder builder) {
    this.possibleApplicationConfigBuilder = builder;
  }

  public TransparentAppConfig getTransparentAppConfig() {
    return this.transparentAppConfig;
  }

  protected ApplicationConfigBuilder getApplicationConfigBuilder() {
    if (possibleApplicationConfigBuilder != null) return possibleApplicationConfigBuilder;
    else return transparentAppConfig;
  }

  protected abstract Class getApplicationClass();

  protected Map getOptionalAttributes() {
    return new HashMap();
  }

  String getServerPortProp() {
    return System.getProperty("test.base.server.port");
  }

  private boolean getStartServer() {
    return getServerPortProp() == null && mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_NORMAL)
           && !controlledCrashMode && !useExternalProcess();
  }

  public void initializeTestRunner() throws Exception {
    initializeTestRunner(false);
  }

  public void initializeTestRunner(boolean isMutateValidateTest) throws Exception {
    this.runner = new DistributedTestRunner(runnerConfig, configFactory(), configHelper(), getApplicationClass(),
                                            getOptionalAttributes(), getApplicationConfigBuilder()
                                                .newApplicationConfig(), getStartServer(), isMutateValidateTest,
                                            (isActivePassive() && canRunActivePassive()), apServerManager,
                                            transparentAppConfig);
  }

  protected boolean canRun() {
    return (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_NORMAL) && canRunNormal())
           || (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH) && canRunCrash())
           || (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE) && canRunActivePassive());
  }

  protected boolean canRunNormal() {
    return true;
  }

  protected boolean canRunCrash() {
    return false;
  }

  protected boolean canRunActivePassive() {
    return false;
  }

  protected boolean canRunL1ProxyConnect() {
    return false;
  }

  protected boolean canSkipL1ReconnectCheck() {
    return false;
  }

  protected boolean enableManualProxyConnectControl() {
    return false;
  }

  protected boolean enableL1Reconnect() {
    return false;
  }

  protected boolean canRunL2ProxyConnect() {
    return false;
  }

  protected boolean enableL2Reconnect() {
    return false;
  }

  protected void startServerControlsAndProxies() throws Exception {
    assertEquals(serverControls.length, 2);
    for (int i = 0; i < serverControls.length; i++) {
      serverControls[i].start();

      // make sure that the first server becomes active
      if (i == 0) {
        Thread.sleep(10 * 1000);
      } else {
        if (proxies != null) {
          proxies[1].start();
          proxies[0].start();
        }
      }
    }
  }

  /*
   * Can be overwritten for customerizing active passive test
   */
  protected void customerizeActivePassiveTest() throws Exception {
    apServerManager.startActivePassiveServers();
  }

  protected void apStartServer(int index) throws Exception {
    apServerManager.startServer(index);
  }

  protected void apStopServer(int index) throws Exception {
    apServerManager.stopServer(index);
  }

  protected void apCleanupServerDB(int index) throws Exception {
    apServerManager.cleanupServerDB(index);
  }

  protected void apCrashActiveserver() throws Exception {
    apServerManager.crashActive();
  }

  protected int apGetActiveIndex() throws Exception {
    return apServerManager.getAndUpdateActiveIndex();
  }

  protected void waitServerIsPassiveStandby(int index, int waitSeconds) throws Exception {
    boolean isStandby = apServerManager.waitServerIsPassiveStandby(index, waitSeconds);
    Assert.assertTrue(isStandby);
  }

  protected void duringRunningCluster() throws Exception {
    // do not delete this method, it is used by tests that override it
  }

  public void test() throws Exception {
    if (canRun()) {
      if (controlledCrashMode && isActivePassive() && apServerManager != null) {
        // active passive tests
        customerizeActivePassiveTest();
      } else if (controlledCrashMode && serverControls != null) {
        startServerControlsAndProxies();
      } else if (serverControl != null && crasher == null) {
        // normal mode tests
        serverControl.start();
      }
      // NOTE: for crash tests the server needs to be started by the ServerCrasher.. timing issue

      this.runner.startServer();
      if (canRunL1ProxyConnect()) {
        proxyMgr.proxyUp();

        if (!enableManualProxyConnectControl()) {
          proxyMgr.startProxyTest();
        }
      }
      this.runner.run();
      duringRunningCluster();

      if (this.runner.executionTimedOut() || this.runner.startTimedOut()) {
        try {
          System.err.println("##### About to shutdown server crasher");
          synchronized (crashTestState) {
            crashTestState.setTestState(TestState.STOPPING);
          }
          System.err.println("##### About to dump server");
          dumpServers();
        } finally {
          if (pid != 0) {
            System.out.println("Thread dumping test process");
            ThreadDump.dumpThreadsMany(getThreadDumpCount(), getThreadDumpInterval());
          }
        }
      }

      if (!this.runner.success()) {
        AssertionFailedError e = new AssertionFailedError(new ErrorContextFormatter(this.runner.getErrors())
            .formatForExceptionMessage());
        throw e;
      }
    } else {
      System.err.println("NOTE: " + getClass().getName() + " can't be run in mode '" + mode()
                         + "', and thus will be skipped.");
    }
  }

  private void dumpServers() throws Exception {
    if (serverControl != null && serverControl.isRunning()) {
      System.out.println("Dumping server=[" + serverControl.getDsoPort() + "]");
      dumpServerControl(serverControl);
    }

    if (apServerManager != null) {
      apServerManager.dumpAllServers(pid, getThreadDumpCount(), getThreadDumpInterval());
      pid = apServerManager.getPid();
    }

    if (serverControls != null) {
      for (int i = 0; i < serverControls.length; i++) {
        dumpServerControl(serverControls[i]);
      }
    }

    if (runner != null) {
      runner.dumpServer();
    } else {
      System.err.println("Runner is null !!");
    }
  }

  private void dumpServerControl(ServerControl control) throws Exception {
    JMXConnector jmxConnector = ActivePassiveServerManager.getJMXConnector(control.getAdminPort());
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    L2DumperMBean mbean = (L2DumperMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DUMPER,
                                                                                        L2DumperMBean.class, true);
    while (true) {
      try {
        mbean.doServerDump();
        break;
      } catch (Exception e) {
        System.out.println("Could not find L2DumperMBean... sleep for 1 sec.");
        Thread.sleep(1000);
      }
    }

    if (pid != 0) {
      mbean.setThreadDumpCount(getThreadDumpCount());
      mbean.setThreadDumpInterval(getThreadDumpInterval());
      System.out.println("Thread dumping server=[" + serverControl.getDsoPort() + "] pid=[" + pid + "]");
      pid = mbean.doThreadDump();
    }
    jmxConnector.close();
  }

  protected void tearDown() throws Exception {
    if (controlledCrashMode) {
      if (isActivePassive() && canRunActivePassive()) {
        System.out.println("Currently running java processes: " + ProcessInfo.ps_grep_java());
        apServerManager.stopAllServers();
      } else if (isCrashy() && canRunCrash()) {
        synchronized (crashTestState) {
          crashTestState.setTestState(TestState.STOPPING);
          if (serverControl.isRunning()) {
            serverControl.shutdown();
          }
        }
      }
    }

    if (serverControls != null) {
      for (int i = 0; i < serverControls.length; i++) {
        if (serverControls[i].isRunning()) {
          serverControls[i].shutdown();
        }
      }
    }

    if (serverControl != null && serverControl.isRunning()) {
      serverControl.shutdown();
    }

    super.tearDown();
  }

  protected void doDumpServerDetails() {
    try {
      dumpServers();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private static final class ErrorContextFormatter {
    private final Collection   contexts;
    private final StringBuffer buf = new StringBuffer();

    public ErrorContextFormatter(Collection contexts) {
      this.contexts = contexts;
    }

    private void div() {
      buf.append("\n**************************************************************\n");
    }

    private void println(Object message) {
      buf.append(message + "\n");
    }

    public String formatForExceptionMessage() {
      buf.delete(0, buf.length());
      div();
      println("There are " + contexts.size() + " error contexts:");
      int count = 1;
      for (Iterator i = contexts.iterator(); i.hasNext();) {
        ErrorContext ctxt = (ErrorContext) i.next();
        println("Error context " + count + "\n");
        println(ctxt);
        count++;
      }
      println("End error contexts.");
      div();
      return buf.toString();
    }
  }

  protected File writeMinimalConfig(int port, int administratorPort) {
    TerracottaConfigBuilder builder = createConfigBuilder(port, administratorPort);
    FileOutputStream out = null;
    File configFile = null;
    try {
      configFile = getTempFile("config-file.xml");
      out = new FileOutputStream(configFile);
      CopyUtils.copy(builder.toString(), out);
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    } finally {
      try {
        out.close();
      } catch (Exception e) { /* oh well, we tried */
      }
    }

    return configFile;
  }

  protected TerracottaConfigBuilder createConfigBuilder(int port, int administratorPort) {
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(administratorPort);

    return out;
  }

  /*
   * State inner class
   */

}
