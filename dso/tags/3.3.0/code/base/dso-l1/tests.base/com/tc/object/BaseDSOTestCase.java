/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import org.apache.commons.lang.ArrayUtils;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.BindPort;

import java.io.IOException;

/**
 * The base of all DSO tests that use config.
 */
public class BaseDSOTestCase extends TCTestCase implements TestClientConfigHelperFactory {

  static {
    // ensure that the databases that are created for the CVT are unique for
    // each test run and for all the nodes that are started within a test
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.CVT_BUFFER_RANDOM_SUFFIX_ENABLED, "true");
    tcProps.setProperty(TCPropertiesConsts.CVT_STORE_RANDOM_SUFFIX_ENABLED, "true");
  }

  private Exception failTestException;

  private class TestFailingIllegalConfigChangeHandler implements IllegalConfigurationChangeHandler {
    public void changeFailed(ConfigItem item, Object oldValue, Object newValue) {
      failTestException = new Exception("An attempt was made to illegally change the config item " + item + " from "
                                        + ArrayUtils.toString(oldValue) + " to " + ArrayUtils.toString(newValue));
    }
  }

  @Override
  public void runBare() throws Throwable {
    super.runBare();
    if (this.failTestException != null) throw this.failTestException;
  }

  private TestTVSConfigurationSetupManagerFactory configFactory;
  private L1TVSConfigurationSetupManager          l1ConfigManager;
  private DSOClientConfigHelper                   configHelper;

  protected synchronized final void setUp(TestTVSConfigurationSetupManagerFactory factory, DSOClientConfigHelper helper)
      throws Exception {
    super.setUp();
    this.configFactory = factory;
    this.configHelper = helper;
  }

  protected synchronized final TestTVSConfigurationSetupManagerFactory configFactory()
      throws ConfigurationSetupException {
    if (this.configFactory == null) this.configFactory = createDistributedConfigFactory();
    return this.configFactory;
  }

  protected synchronized final TestTVSConfigurationSetupManagerFactory createDistributedConfigFactory()
      throws ConfigurationSetupException {
    TestTVSConfigurationSetupManagerFactory out;
    out = new TestTVSConfigurationSetupManagerFactory(TestTVSConfigurationSetupManagerFactory.MODE_DISTRIBUTED_CONFIG,
                                                      null, new TestFailingIllegalConfigChangeHandler());

    prepareFactory(out);
    return out;
  }

  private synchronized void prepareFactory(TestTVSConfigurationSetupManagerFactory out)
      throws ConfigurationSetupException {
    // We add a root to make sure there's at least *some* application config. Otherwise, the config system will wait for
    // it on system startup.
    /*
     * Roots roots = Roots.Factory.newInstance(); Root dummyRoot = roots.addNewRoot();
     * dummyRoot.setFieldName("com.dummy.whatever.Bar.x");
     */

    AdditionalBootJarClasses classes = AdditionalBootJarClasses.Factory.newInstance();
    classes.setIncludeArray(new String[] { "com.dummy.whatever.Bar" });

    ((SettableConfigItem) out.dsoApplicationConfig().additionalBootJarClasses()).setValue(classes);
    // ((SettableConfigItem) out.dsoApplicationConfig().roots()).setValue(roots);

    setupConfigLogDataStatisticsPaths(out);

    out.activateConfigurationChange();
  }

  protected synchronized void setupConfigLogDataStatisticsPaths(TestTVSConfigurationSetupManagerFactory out)
      throws ConfigurationSetupException {
    try {
      ((SettableConfigItem) out.l2CommonConfig().dataPath()).setValue(getTempFile("l2-data").toString());
      ((SettableConfigItem) out.l2CommonConfig().logsPath()).setValue(getTempFile("l2-logs").toString());
      ((SettableConfigItem) out.l2CommonConfig().statisticsPath()).setValue(getTempFile("l2-statistics").toString());
      ((SettableConfigItem) out.l1CommonConfig().logsPath()).setValue(getTempFile("l1-logs").toString());
    } catch (IOException ioe) {
      throw new ConfigurationSetupException("Can't set up log, data and statistics paths", ioe);
    }
  }

  protected synchronized final TestTVSConfigurationSetupManagerFactory createCentralizedConfigFactory()
      throws ConfigurationSetupException {
    TestTVSConfigurationSetupManagerFactory out;
    out = new TestTVSConfigurationSetupManagerFactory(new TestFailingIllegalConfigChangeHandler());

    prepareFactory(out);
    return out;
  }

  protected synchronized final L1TVSConfigurationSetupManager l1Manager() throws ConfigurationSetupException {
    if (this.l1ConfigManager == null) this.l1ConfigManager = createL1ConfigManager();
    return this.l1ConfigManager;
  }

  protected synchronized final L1TVSConfigurationSetupManager createL1ConfigManager()
      throws ConfigurationSetupException {
    return configFactory().createL1TVSConfigurationSetupManager();
  }

  protected synchronized final DSOClientConfigHelper configHelper() throws ConfigurationSetupException {
    if (this.configHelper == null) this.configHelper = createClientConfigHelper();
    return this.configHelper;
  }

  public synchronized final DSOClientConfigHelper createClientConfigHelper() throws ConfigurationSetupException {
    return new StandardDSOClientConfigHelperImpl(true, createL1ConfigManager());
  }

  // TODO: fix this
  protected synchronized final void makeClientUsePort(int whichPort) throws ConfigurationSetupException {
    BindPort bindPort = BindPort.Factory.newInstance();
    bindPort.setIntValue(whichPort);
    ((SettableConfigItem) configFactory().l2DSOConfig().dsoPort()).setValue(bindPort);
  }

  public BaseDSOTestCase() {
    super();
  }

  public BaseDSOTestCase(String arg0) {
    super(arg0);
  }

  @Override
  protected synchronized void tearDown() throws Exception {
    this.configFactory = null;
    this.configHelper = null;
    this.l1ConfigManager = null;
  }
}
