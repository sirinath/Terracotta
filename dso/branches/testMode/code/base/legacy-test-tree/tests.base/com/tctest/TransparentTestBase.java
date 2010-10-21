/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.lang.ClassUtils;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.DSOApplicationConfigBuilderImpl;
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
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.ApplicationConfigBuilder;
import com.tc.simulator.app.ErrorContext;
import com.tc.test.MultipleServerManager;
import com.tc.test.MultipleServersConfigCreator;
import com.tc.test.ProcessInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.activeactive.ActiveActiveServerManager;
import com.tc.test.activeactive.ActiveActiveTestSetupManager;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.test.restart.RestartTestHelper;
import com.tc.test.restart.ServerCrasher;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.ThreadDump;
import com.tctest.modes.CrashTestMode;
import com.tctest.modes.NormalTestMode;
import com.tctest.modes.NormalTestSetupManager;
import com.tctest.modes.TestMode;
import com.tctest.runner.DistributedTestRunner;
import com.tctest.runner.DistributedTestRunnerConfig;
import com.tctest.runner.PostAction;
import com.tctest.runner.TestGlobalIdGenerator;
import com.tctest.runner.TransparentAppConfig;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.PersistenceMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.AssertionFailedError;

public abstract class TransparentTestBase extends BaseDSOTestCase implements TransparentTestIface, TestConfigurator {

  public static final int                   DEFAULT_CLIENT_COUNT            = 2;
  public static final int                   DEFAULT_INTENSITY               = 10;
  public static final int                   DEFAULT_VALIDATOR_COUNT         = 0;
  public static final int                   DEFAULT_ADAPTED_MUTATOR_COUNT   = 0;
  public static final int                   DEFAULT_ADAPTED_VALIDATOR_COUNT = 0;

  protected DistributedTestRunner           runner;

  private final DistributedTestRunnerConfig runnerConfig                    = new DistributedTestRunnerConfig(
                                                                                                              getTimeoutValueInSeconds());
  private TransparentAppConfig              transparentAppConfig;
  private ApplicationConfigBuilder          possibleApplicationConfigBuilder;

  private TestMode                          currentTestMode;
  private boolean                           isCurrentRunPossible            = false;

  private ServerControl                     serverControl;
  protected boolean                         controlledCrashMode             = false;
  private ServerCrasher                     crasher;
  protected File                            javaHome;
  protected int                             pid                             = -1;
  private final ProxyConnectManager         proxyMgr                        = new ProxyConnectManagerImpl();

  private TestState                         crashTestState;

  // used by ResolveTwoActiveServersTest only
  private ServerControl[]                   serverControls                  = null;
  private TCPProxy[]                        proxies                         = null;

  private int                               dsoPort                         = -1;
  private int                               adminPort                       = -1;
  private int                               groupPort                       = -1;
  private final List                        postActions                     = new ArrayList();

  private String                            testName                        = "default";

  private static final LinkedList<String>   modesToRun                      = new LinkedList<String>();
  private static TestModesHandler           handler;

  /**
   * The server manager which currently takes care of active-passive and active-active tests
   */
  protected MultipleServerManager           multipleServerManager;

  static {
    String mode = TestConfigObject.getInstance().transparentTestsMode();
    if (mode == null) {
      modesToRun.add(TestConfigObject.TRANSPARENT_TESTS_MODE_NORMAL);
      modesToRun.add(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH);
      modesToRun.add(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE);
      modesToRun.add(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_ACTIVE);
    } else {
      modesToRun.add(mode);
    }
  }

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

    if (Os.isLinux() || Os.isSolaris()) {
      // default 5000 ms seems to small occasionally in few linux machines
      tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, "10000");
      System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, "10000");
      jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS + "=10000");
    }
  }

  protected void setJvmArgsL2Reconnect(final ArrayList jvmArgs) {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "true");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "true");

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED + "=true");

    // for windows, it takes 10 seconds to restart proxy port
    if (Os.isWindows()) {
      setL2ReconnectTimout(jvmArgs, 20000);
    }
  }

  protected void setL2ReconnectTimout(final ArrayList jvmArgs, int timeoutMilliSecond) {
    String timeoutString = Integer.toString(timeoutMilliSecond);
    TCProperties tcProps = TCPropertiesImpl.getProperties();

    tcProps.setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT, timeoutString);
    System.setProperty("com.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT, timeoutString);
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT + "=" + timeoutString);
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
    if (isMultipleServerTest()) {
      // limit L2 heap size for all active-active and active-passive tests
      jvmArgs.add("-Xmx256m");
    }
  }

  @Override
  protected void setUp() throws Exception {
    setUpConfigThisMode();

    if (!isCurrentRunPossible) { return; }

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
    if (((isCrashy()) || useExternalProcess()) && !isMultipleServerTest()) {
      // javaHome is set here only to enforce that java home is defined in the test config
      // javaHome is set again inside RestartTestEnvironment because how that class is used
      // TODO: clean this up
      setJavaHome();

      helper = new RestartTestHelper(mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH),
                                     new RestartTestEnvironment(getTempDirectory(), portChooser,
                                                                RestartTestEnvironment.PROD_MODE, configFactory(),
                                                                this.testName), jvmArgs);
      dsoPort = helper.getServerPort();
      adminPort = helper.getAdminPort();
      groupPort = helper.getGroupPort();
      BindPort dsoBindPort = BindPort.Factory.newInstance();
      dsoBindPort.setIntValue(dsoPort);
      dsoBindPort.setBind("0.0.0.0");
      ((SettableConfigItem) configFactory().l2DSOConfig().dsoPort()).setValue(dsoBindPort);

      BindPort jmxBindPort = BindPort.Factory.newInstance();
      jmxBindPort.setIntValue(adminPort);
      jmxBindPort.setBind("0.0.0.0");
      ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(jmxBindPort);

      BindPort groupBindPort = BindPort.Factory.newInstance();
      groupBindPort.setIntValue(groupPort);
      groupBindPort.setBind("0.0.0.0");
      ((SettableConfigItem) configFactory().l2DSOConfig().l2GroupPort()).setValue(groupBindPort);

      if (!canRunL1ProxyConnect()) configFactory().addServerToL1Config(null, dsoPort, adminPort);
      serverControl = helper.getServerControl();
    } else if (isMultipleServerTest()) {
      setUpMultipleServersTest(portChooser, jvmArgs);
    } else {
      dsoPort = portChooser.chooseRandomPort();
      adminPort = portChooser.chooseRandomPort();
      groupPort = portChooser.chooseRandomPort();
      BindPort dsoBindPort = BindPort.Factory.newInstance();
      dsoBindPort.setIntValue(dsoPort);
      dsoBindPort.setBind("0.0.0.0");
      ((SettableConfigItem) configFactory().l2DSOConfig().dsoPort()).setValue(dsoBindPort);

      BindPort jmxBindPort = BindPort.Factory.newInstance();
      jmxBindPort.setIntValue(adminPort);
      jmxBindPort.setBind("0.0.0.0");
      ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(jmxBindPort);

      BindPort groupBindPort = BindPort.Factory.newInstance();
      groupBindPort.setIntValue(groupPort);
      groupBindPort.setBind("0.0.0.0");
      ((SettableConfigItem) configFactory().l2DSOConfig().l2GroupPort()).setValue(groupBindPort);

      if (!canRunL1ProxyConnect()) configFactory().addServerToL1Config(null, dsoPort, -1);
    }

    if (canRunL1ProxyConnect() && !isMultipleServerTest()) {
      setupProxyConnect(helper, portChooser);
    }

    this.doSetUp(this);
    this.transparentAppConfig.setAttribute(ApplicationConfig.JMXPORT_KEY, String.valueOf(configFactory()
        .createL2TVSConfigurationSetupManager(null).commonl2Config().jmxPort().getBindPort()));

    if (isCrashy()) {
      crashTestState = new TestState(false);
      crasher = new ServerCrasher(serverControl, getRestartInterval(helper),
                                  helper.getServerCrasherConfig().isCrashy(), crashTestState, proxyMgr);
      if (canRunL1ProxyConnect()) crasher.setProxyConnectMode(true);
      crasher.startAutocrash();
    }
  }

  private void setUpConfigThisMode() throws ConfigurationSetupException {
    initHandler();

    String currentRunningMode = null;

    do {
      if (modesToRun.size() == 0) {
        currentTestMode = null;
        isCurrentRunPossible = false;
        return;
      } else {
        currentRunningMode = modesToRun.getFirst();
      }
      currentTestMode = handler.getTestModeFor(currentRunningMode);
      if (currentTestMode == null) {
        modesToRun.removeFirst();
      }
    } while (currentTestMode == null);

    isCurrentRunPossible = true;
    this.testName = "test-" + currentRunningMode + "-" + handler.getIndexFor(currentRunningMode);

    switch (currentTestMode.getMode()) {
      case NORMAL:
        NormalTestSetupManager normalSetupManager = (NormalTestSetupManager) currentTestMode.getSetupManager();
        if (normalSetupManager.isPersistent()) {
          configFactory().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
        } else {
          configFactory().setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
        }
        break;
      case CRASH:
        // CrashTestSetupManager crashSetupManager = (CrashTestSetupManager) mode.getSetupManager();
        // if (crashSetupManager.isPersistent()) {
        // configFactory().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
        // } else {
        // configFactory().setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
        // }
        break;
      case ACTIVE_ACTIVE:
      case ACTIVE_PASSIVE:
        break;
    }
  }

  private void initHandler() {
    if (handler == null) {
      handler = new TestModesHandler(getTestModes());
    }
  }

  protected long getRestartInterval(RestartTestHelper helper) {
    return helper.getServerCrasherConfig().getRestartInterval();
  }

  protected void setUpMultipleServersTest(PortChooser portChooser, ArrayList jvmArgs) throws Exception {
    if (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE)) {
      setUpActivePassiveServers(portChooser, jvmArgs);
    } else if (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_ACTIVE)) {
      setUpActiveActiveServers(portChooser, jvmArgs);
    } else {
      throw new AssertionError("setUpMultipleServersTest mode=" + mode() + " not supported");
    }
  }

  private void setUpActivePassiveServers(PortChooser portChooser, List jvmArgs) throws Exception {
    controlledCrashMode = true;
    setJavaHome();
    ActivePassiveServerManager apServerManager = new ActivePassiveServerManager(
                                                                                mode()
                                                                                    .equals(
                                                                                            TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE),
                                                                                getMultipleServersDirectory(),
                                                                                portChooser,
                                                                                MultipleServersConfigCreator.DEV_MODE,
                                                                                (ActivePassiveTestSetupManager) currentTestMode
                                                                                    .getSetupManager(), javaHome,
                                                                                configFactory(), jvmArgs,
                                                                                canRunL2ProxyConnect(),
                                                                                canRunL1ProxyConnect(),
                                                                                createDsoApplicationConfig());

    apServerManager.addServersAndGroupToL1Config(configFactory());
    if (canRunL2ProxyConnect()) setupL2ProxyConnectTest(apServerManager.getL2ProxyManagers());
    if (canRunL1ProxyConnect()) setupL1ProxyConnectTest(apServerManager.getL1ProxyManagers());

    multipleServerManager = apServerManager;
  }

  private void setUpActiveActiveServers(PortChooser portChooser, List jvmArgs) throws Exception {
    controlledCrashMode = true;
    setJavaHome();
    ActiveActiveServerManager aaServerManager = new ActiveActiveServerManager(
                                                                              getMultipleServersDirectory(),
                                                                              portChooser,
                                                                              MultipleServersConfigCreator.DEV_MODE,
                                                                              (ActiveActiveTestSetupManager) currentTestMode
                                                                                  .getSetupManager(), javaHome,
                                                                              configFactory(), jvmArgs,
                                                                              canRunL2ProxyConnect(),
                                                                              canRunL1ProxyConnect(),
                                                                              createDsoApplicationConfig());
    aaServerManager.addGroupsToL1Config(configFactory());
    if (canRunL2ProxyConnect()) setupL2ProxyConnectTest(aaServerManager.getL2ProxyManagers());
    if (canRunL1ProxyConnect()) setupL1ProxyConnectTest(aaServerManager.getL1ProxyManagers());

    multipleServerManager = aaServerManager;
  }

  private File getMultipleServersDirectory() throws IOException {
    return new File(getTempDirectory().getAbsolutePath() + File.separator + this.getTestName());
  }

  protected void setupActiveActiveTest(ActiveActiveTestSetupManager setupManager) {
    throw new AssertionError("setupActiveActiveTest should be Overridden");
  }

  protected void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    throw new AssertionError("setupActivePassiveTest should be Overridden");
  }

  // to be override for L1 application config
  protected DSOApplicationConfigBuilder createDsoApplicationConfig() {
    return (new DSOApplicationConfigBuilderImpl());
  }

  public int getDsoPort() {
    if (isMultipleServerTest()) { return multipleServerManager.getDsoPort(); }
    return dsoPort;
  }

  public int getAdminPort() {
    if (isMultipleServerTest()) { return multipleServerManager.getJMXPort(); }
    return adminPort;
  }

  public int getGroupPort() {
    if (isMultipleServerTest()) { return multipleServerManager.getL2GroupPort(); }
    return this.groupPort;
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

  protected void setupConfig(TestTVSConfigurationSetupManagerFactory configFactory) {
    // do nothing
  }

  public File makeTmpDir(Class klass) {
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
      groupPort = helper.getGroupPort();
      // for crash+proxy, set crash interval to 60 sec
      helper.getServerCrasherConfig().setRestartInterval(60 * 1000);
    } else if (isMultipleServerTest()) {
      // not doing active-passive/active-active for proxy yet
      throw new AssertionError("Should never reach here");
    } else {
      dsoPort = portChooser.chooseRandomPort();
      adminPort = portChooser.chooseRandomPort();
      groupPort = portChooser.chooseRandomPort();
    }

    int dsoProxyPort = portChooser.chooseRandomPort();

    proxyMgr.setDsoPort(dsoPort);
    proxyMgr.setProxyPort(dsoProxyPort);
    proxyMgr.setupProxy();
    setupL1ProxyConnectTest(proxyMgr);

    BindPort dsoBindPort = BindPort.Factory.newInstance();
    dsoBindPort.setIntValue(dsoPort);
    dsoBindPort.setBind("0.0.0.0");
    ((SettableConfigItem) configFactory().l2DSOConfig().dsoPort()).setValue(dsoBindPort);

    BindPort jmxBindPort = BindPort.Factory.newInstance();
    jmxBindPort.setIntValue(adminPort);
    jmxBindPort.setBind("0.0.0.0");
    ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(jmxBindPort);

    BindPort groupBindPort = BindPort.Factory.newInstance();
    groupBindPort.setIntValue(groupPort);
    groupBindPort.setBind("0.0.0.0");
    ((SettableConfigItem) configFactory().l2DSOConfig().l2GroupPort()).setValue(groupBindPort);

    configFactory().addServerToL1Config(null, dsoProxyPort, -1);
    disableL1L2ConfigValidationCheck();
  }

  protected void setupL1ProxyConnectTest(ProxyConnectManager mgr) {
    /*
     * subclass can overwrite to change the test parameters.
     */
    mgr.setProxyWaitTime(20 * 1000);
    mgr.setProxyDownTime(100);
  }

  /**
   * When L1s are intended to connect to proxy ports, the config is different from that of L2's. Disabling the L1 config
   * validation check for proxy connect scenarios.
   */
  protected void disableL1L2ConfigValidationCheck() throws Exception {
    configFactory().addTcPropertyToConfig("l1.l2.config.validation.enabled", "false");
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

  protected void setupL1ProxyConnectTest(ProxyConnectManager[] managers) throws Exception {
    /*
     * subclass can overwrite to change the test parameters.
     */
    for (int i = 0; i < managers.length; ++i) {
      managers[i].setProxyWaitTime(20 * 1000);
      managers[i].setProxyDownTime(100);
    }
    disableL1L2ConfigValidationCheck();
  }

  private boolean useExternalProcess() {
    return true;// getTestConfigObject().isL2StartupModeExternal();
  }

  // only used by regular system tests (not crash or active-passive)
  protected final void setUpControlledServer(TestTVSConfigurationSetupManagerFactory factory,
                                             DSOClientConfigHelper helper, int serverPort, int adminPort,
                                             int groupPort, String configFile) throws Exception {
    setUpControlledServer(factory, helper, serverPort, adminPort, groupPort, configFile, null);
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
                                             int groupPort, String configFile, List jvmArgs) throws Exception {
    controlledCrashMode = true;
    if (jvmArgs == null) {
      jvmArgs = new ArrayList();
    }
    addTestTcPropertiesFile(jvmArgs);
    setUpExternalProcess(factory, helper, serverPort, adminPort, groupPort, configFile, jvmArgs);
  }

  protected void setUpExternalProcess(TestTVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper,
                                      int serverPort, int adminPort, int groupPort, String configFile, List jvmArgs)
      throws Exception {
    setJavaHome();
    assertNotNull(jvmArgs);
    serverControl = new ExtraProcessServerControl("localhost", serverPort, adminPort, configFile, true, javaHome,
                                                  jvmArgs);
    setUpTransparent(factory, helper);

    BindPort dsoBindPort = BindPort.Factory.newInstance();
    dsoBindPort.setIntValue(serverPort);
    dsoBindPort.setBind("0.0.0.0");
    ((SettableConfigItem) configFactory().l2DSOConfig().dsoPort()).setValue(dsoBindPort);

    BindPort jmxBindPort = BindPort.Factory.newInstance();
    jmxBindPort.setIntValue(adminPort);
    jmxBindPort.setBind("0.0.0.0");
    ((SettableConfigItem) configFactory().l2CommonConfig().jmxPort()).setValue(jmxBindPort);

    BindPort l2GroupPort = BindPort.Factory.newInstance();
    jmxBindPort.setIntValue(groupPort);
    jmxBindPort.setBind("0.0.0.0");
    ((SettableConfigItem) configFactory().l2DSOConfig().l2GroupPort()).setValue(l2GroupPort);

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
    if (currentTestMode == null) { return null; }
    return currentTestMode.getMode().toString();
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    // Nothing here, by default
  }

  private boolean isCrashy() {
    return TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH.equals(mode());
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

  public boolean isMultipleServerTest() {
    return TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_ACTIVE.equals(mode())
           || TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE.equals(mode());
  }

  public void initializeTestRunner() throws Exception {
    initializeTestRunner(false);
  }

  public void initializeTestRunner(boolean isMutateValidateTest) throws Exception {
    initializeTestRunner(isMutateValidateTest, transparentAppConfig, runnerConfig);
    loadPostActions();
    initPostActions();
  }

  public void addPostAction(PostAction postAction) {
    this.postActions.add(postAction);
  }

  protected void loadPostActions() {
    // do not removed.
  }

  private void initPostActions() {
    for (Iterator iter = postActions.iterator(); iter.hasNext();) {
      runner.addPostAction((PostAction) iter.next());
    }
  }

  public void initializeTestRunner(boolean isMutateValidateTest, TransparentAppConfig transparentAppCfg,
                                   DistributedTestRunnerConfig runnerCfg) throws Exception {
    if (!isMultipleServerTest()) {
      runner = new DistributedTestRunner(runnerCfg, configFactory(), this, getApplicationClass(),
                                         getOptionalAttributes(), getApplicationConfigBuilder().newApplicationConfig(),
                                         getStartServer(), isMutateValidateTest, isMultipleServerTest(), null,
                                         transparentAppCfg);
    } else {
      runner = new DistributedTestRunner(runnerCfg, configFactory(), this, getApplicationClass(),
                                         getOptionalAttributes(), getApplicationConfigBuilder().newApplicationConfig(),
                                         false, isMutateValidateTest, isMultipleServerTest(), multipleServerManager,
                                         transparentAppCfg);
    }

  }

  protected boolean canRun() {
    return mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_NORMAL)
           || (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_CRASH)) || isMultipleServerTest();
  }

  protected boolean isRunNormalMode() {
    return (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_NORMAL));
  }

  protected boolean canRunNormal() {
    return true;
  }

  protected boolean canRunCrash() {
    return false;
  }

  protected boolean canRunL1ProxyConnect() {
    if (this.currentTestMode != null) { return this.currentTestMode.getSetupManager().canRunL1ProxyConnect(); }
    return false;
  }

  protected boolean canSkipL1ReconnectCheck() {
    if (this.currentTestMode != null) { return this.currentTestMode.getSetupManager().canSkipL1ReconnectCheck(); }
    return false;
  }

  protected boolean enableManualProxyConnectControl() {
    if (this.currentTestMode != null) { return this.currentTestMode.getSetupManager().enableManualProxyConnectControl(); }
    return false;
  }

  protected boolean enableL1Reconnect() {
    if (this.currentTestMode != null) { return this.currentTestMode.getSetupManager().enableL1Reconnect(); }
    return false;
  }

  protected boolean canRunL2ProxyConnect() {
    if (this.currentTestMode != null) { return this.currentTestMode.getSetupManager().canRunL2ProxyConnect(); }
    return false;
  }

  protected boolean enableL2Reconnect() {
    if (this.currentTestMode != null) { return this.currentTestMode.getSetupManager().enableL2Reconnect(); }
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

  protected void duringRunningCluster() throws Exception {
    // do not delete this method, it is used by tests that override it
  }

  private Thread executeDuringRunningCluster() {
    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          duringRunningCluster();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    t.setName(getClass().getName() + " duringRunningCluster");
    t.start();
    return t;
  }

  public void test_1() throws Exception {
    startTest();
  }

  public void test_2() throws Exception {
    startTest();
  }

  public void test_3() throws Exception {
    startTest();
  }

  public void test_4() throws Exception {
    startTest();
  }

  public void test_5() throws Exception {
    startTest();
  }

  private void startTest() throws Exception {
    if (!isCurrentRunPossible) { return; }

    System.err.println("XXX Starting test run = " + testName);
    if (isMultipleServerTest()) runMultipleServersTest();

    if (canRun()) {
      if (controlledCrashMode && serverControls != null) {
        startServerControlsAndProxies();
      } else if (serverControl != null && crasher == null) {
        // normal mode tests
        serverControl.start();
      }
      // NOTE: for crash tests the server needs to be started by the ServerCrasher.. timing issue

      this.runner.startServer();
      if (canRunL1ProxyConnect() && !isMultipleServerTest()) {
        proxyMgr.proxyUp();

        if (!enableManualProxyConnectControl()) {
          proxyMgr.startProxyTest();
        }
      }
      final Thread duringRunningClusterThread = executeDuringRunningCluster();
      this.runner.run();
      duringRunningClusterThread.join();
      if (this.runner.executionTimedOut() || this.runner.startTimedOut()) {
        try {
          if (isCrashy()) {
            System.err.println("##### About to shutdown server crasher");
            synchronized (crashTestState) {
              crashTestState.setTestState(TestState.STOPPING);
            }
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

  protected void runMultipleServersTest() throws Exception {
    if (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE)) {
      customizeActivePassiveTest((ActivePassiveServerManager) multipleServerManager);
    } else if (mode().equals(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_ACTIVE)) {
      customizeActiveActiveTest((ActiveActiveServerManager) multipleServerManager);
    } else {
      throw new AssertionError("runMultipleServersTest mode=" + mode() + " not supported");
    }
  }

  protected void customizeActiveActiveTest(ActiveActiveServerManager manager) throws Exception {
    manager.startActiveActiveServers();
  }

  protected void customizeActivePassiveTest(ActivePassiveServerManager manager) throws Exception {
    manager.startActivePassiveServers();
  }

  protected void dumpServers() throws Exception {
    if (multipleServerManager != null) {
      multipleServerManager.dumpAllServers(pid, getThreadDumpCount(), getThreadDumpInterval());
    }

    if (serverControl != null && serverControl.isRunning()) {
      System.out.println("Dumping server=[" + serverControl.getDsoPort() + "]");
      dumpServerControl(serverControl);
    }

    if (serverControls != null) {
      for (ServerControl serverControl2 : serverControls) {
        dumpServerControl(serverControl2);
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

  @Override
  protected void tearDown() throws Exception {
    if (controlledCrashMode && isMultipleServerTest()) {
      System.out.println("Currently running java processes: " + ProcessInfo.ps_grep_java());
      multipleServerManager.stopAllServers();
    }

    if (isCrashy() && crashTestState != null) {
      synchronized (crashTestState) {
        crashTestState.setTestState(TestState.STOPPING);
        if (controlledCrashMode && serverControl != null && serverControl.isRunning()) {
          serverControl.shutdown();
        }
      }
    }

    if (serverControls != null) {
      for (ServerControl sc : serverControls) {
        if (sc.isRunning()) {
          sc.shutdown();
        }
      }
    }

    if (serverControl != null && serverControl.isRunning()) {
      serverControl.shutdown();
    }

    super.tearDown();

    if (isCurrentRunPossible && handler.hasMoreRuns(modesToRun)) {
      System.err.println("XXX Sleeping for 10 seconds before starting a new test");
      ThreadUtil.reallySleep(10 * 1000);
    }
  }

  @Override
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

  @Override
  protected String getTestName() {
    return this.testName;
  }

  public String getConfigFileLocation() {
    if (multipleServerManager != null) { return multipleServerManager.getConfigFileLocation(); }
    return null;
  }

  /**
   * Returns the modes associated with a setup manager
   */
  public TestMode[] getTestModes() {
    TestMode[] modes = new TestMode[2];
    modes[0] = new NormalTestMode();
    modes[1] = new CrashTestMode();
    return modes;
  }
}
