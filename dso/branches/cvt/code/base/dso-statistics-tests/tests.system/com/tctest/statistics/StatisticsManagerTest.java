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
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.runner.TransparentAppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.NotificationListener;
import javax.management.Notification;

public class StatisticsManagerTest extends TransparentTestBase {
  private int port;
  private File configFile;
  private int adminPort;

  protected void duringRunningCluster() throws Exception {
    JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", adminPort);
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    List data = new ArrayList();
    final boolean[] shutdown = new boolean[] {false};
    NotificationListener listener = new NotificationListener() {
      public void handleNotification(Notification notification, Object o) {
        StatisticData data = (StatisticData)notification.getUserData();
        ((List)o).add(data);
        if (SRAShutdownTimestamp.ACTION_NAME.equals(data.getName())) {
          shutdown[0] = true;
          synchronized (this) {
            this.notifyAll();
          }
        }
      }
    };

    StatisticsManagerMBean stat_manager = (StatisticsManagerMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, L2MBeanNames.STATISTICS_MANAGER, StatisticsManagerMBean.class, false);
    StatisticsEmitterMBean stat_emitter = (StatisticsEmitterMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, L2MBeanNames.STATISTICS_EMITTER, StatisticsEmitterMBean.class, false);
    mbsc.addNotificationListener(L2MBeanNames.STATISTICS_EMITTER, listener, null, data);
    stat_emitter.enable();

    long sessionid = stat_manager.createCaptureSession();

    // register all the supported statistics
    String[] statistics = stat_manager.getSupportedStatistics();
    for (int i = 0; i < statistics.length; i++) {
      stat_manager.enableStatistic(sessionid, statistics[i]);
    }

    // start capturing
    stat_manager.startCapturing(sessionid);

    // wait for 10 seconds
    Thread.sleep(10000);

    // stop capturing and wait for the last data
    synchronized (listener) {
      stat_manager.stopCapturing(sessionid);
      while (!shutdown[0]) {
        listener.wait(2000);
      }
    }

    // disable the notification and detach the listener
    stat_emitter.disable();
    mbsc.removeNotificationListener(L2MBeanNames.STATISTICS_EMITTER, listener);

    // check the data
    assertTrue(data.size() > 2);
    assertEquals(SRAStartupTimestamp.ACTION_NAME, ((StatisticData)data.get(0)).getName());
    assertEquals(SRAShutdownTimestamp.ACTION_NAME, ((StatisticData)data.get(data.size() - 1)).getName());
    Set received_data_names = new HashSet();
    for (int i = 1; i < data.size() - 1; i++) {
      StatisticData stat_data = (StatisticData)data.get(i);
      received_data_names.add(stat_data.getName());
    }
    // check that there's at least one data element name per registered statistic
    assertTrue(received_data_names.size() > statistics.length);
  }

  /**
  /* below is all setup logic to create an appropriate configuration file
  /* and store the useful values as fields
   */
  protected Class getApplicationClass() {
    return StatisticsManagerTestApp.class;
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

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsManagerTestApp.NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(StatisticsManagerTestApp.CONFIG_FILE, configFile.getAbsolutePath());
    cfg.setAttribute(StatisticsManagerTestApp.PORT_NUMBER, String.valueOf(port));
    cfg.setAttribute(StatisticsManagerTestApp.HOST_NAME, "localhost");
    cfg.setAttribute(StatisticsManagerTestApp.JMX_PORT, String.valueOf(adminPort));
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