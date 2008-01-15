/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import org.apache.commons.io.CopyUtils;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

public class StatisticsManagerTest extends TransparentTestBase {
  private int port;
  private File configFile;
  private int adminPort;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsManagerTestApp.NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(StatisticsManagerTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(StatisticsManagerTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(StatisticsManagerTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(StatisticsManagerTestApp.JMX_PORT, String.valueOf(adminPort));
  }

  protected Class getApplicationClass() {
    return StatisticsManagerTestApp.class;
  }

  protected void duringRunningCluster() throws Exception {
    List servers = MBeanServerFactory.findMBeanServer(null);
    MBeanServer server = (MBeanServer)servers.get(0);
    Set names = server.queryNames(null, null);
    Iterator names_it = names.iterator();
    while (names_it.hasNext()) {
      System.out.println("MBean : " + names_it.next());
    }
  }

  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    port = pc.chooseRandomPort();
    adminPort = pc.chooseRandomPort();
    configFile = getTempFile("config-file.xml");
    writeConfigFile();
    TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
      TestTVSConfigurationSetupManagerFactory.MODE_DISTRIBUTED_CONFIG,
      null,
      new FatalIllegalConfigurationChangeHandler());

    factory.addServerToL1Config(null, port, adminPort);
    L1TVSConfigurationSetupManager manager = factory.createL1TVSConfigurationSetupManager();
    setUpControlledServer(factory, new StandardDSOClientConfigHelperImpl(manager), port, adminPort, configFile.getAbsolutePath());
    doSetUp(this);
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig(port, adminPort);
      FileOutputStream out = new FileOutputStream(configFile);
      CopyUtils.copy(builder.toString(), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig(int port, int adminPort) {
    String testClassName = StatisticsManagerTestApp.class.getName();
    String testClassSuperName = AbstractTransparentApp.class.getName();

    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_TEMPORARY_SWAP_ONLY);

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testClassSuperName + "*");

    out.getApplication().getDSO().setInstrumentedClasses(
      new InstrumentedClassConfigBuilder[] { instrumented1, instrumented2 });

    return out;
  }
}