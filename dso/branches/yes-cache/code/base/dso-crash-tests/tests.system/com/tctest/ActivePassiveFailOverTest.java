/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.BaseDSOTestCase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.util.Assert;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;
import com.tctest.process.ExternalDsoClient;
import com.tctest.process.ExternalDsoServer;

import java.io.File;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class ActivePassiveFailOverTest extends BaseDSOTestCase {

  private TcConfigBuilder configBuilder;
  private ExternalDsoServer server_1, server_2;
  private int               jmxPort_1, jmxPort_2;

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tc/active-passive-fail-over-test.xml");
     configBuilder.randomizePorts();

    jmxPort_1 = configBuilder.getJmxPort(0);
    jmxPort_2 = configBuilder.getJmxPort(1);

    server_1 = createServer("server-1");
    server_2 = createServer("server-2");

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(jmxPort_1);
    System.out.println("server1 became active");
    server_2.start();
    System.out.println("server2 started");
    waitTillBecomePassiveStandBy(jmxPort_2);
    System.out.println("server2 became passive");

  }

  public void testFailover() throws Exception {
    ExternalDsoClient client = createClient("client");
    client.start();
    ThreadUtil.reallySleep(5000);

    server_1.stop();
    System.out.println("server1 stopped");
    waitTillBecomeActive(jmxPort_2);
    System.out.println("server2 became active");
    server_1.start();
    System.out.println("server1 started");
    if (Os.isSolaris()) {
      ThreadUtil.reallySleep(20000);
    } else {
      ThreadUtil.reallySleep(10000);
    }
    if (server_1.isRunning()) {
      server_1.dumpServerControl();
    }
    Assert.assertFalse(server_1.isRunning());
  }

  private boolean isActive(int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = CommandLineBuilder.getJMXConnector(null, null, "localhost", jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isActive = mbean.isActive();
    } catch (Exception e) {
      return false;
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
           System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }

    return isActive;
  }

  private boolean isPassiveStandBy(int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = CommandLineBuilder.getJMXConnector(null, null, "localhost", jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isActive = mbean.isPassiveStandby();
    } catch (Exception e) {
      return false;
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
           System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }

    return isActive;
  }

  private void waitTillBecomeActive(int jmxPort) {
    while (true) {
      if (isActive(jmxPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private void waitTillBecomePassiveStandBy(int jmxPort) {
    while (true) {
      if (isPassiveStandBy(jmxPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private ExternalDsoServer createServer(final String serverName) throws IOException {
    ExternalDsoServer server = new ExternalDsoServer(getWorkDir(serverName), configBuilder.newInputStream(), serverName);
    return server;
  }

  @Override
  protected void tearDown() throws Exception {
    System.err.println("in tearDown");
    if (server_1 != null) server_1.stop();
    if (server_2 != null) server_2.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

  private ExternalDsoClient createClient(final String name) throws IOException {
    ExternalDsoClient client = new ExternalDsoClient(name, getWorkDir(name), configBuilder.newInputStream(), L1.class);
    client.addJvmArg("-Dl1.name=" + name);
    client.addJvmArg("-Dcom.tc." + TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED + "=false");
    return client;
  }

  public static class L1 {
    public static void main(final String[] args) {
      System.out.println(System.getProperty("l1.name") + ": started");
      ThreadUtil.reallySleep(10 * 60 * 1000);
      System.out.println(System.getProperty("l1.name") + ": stopped");
    }
  }
}
