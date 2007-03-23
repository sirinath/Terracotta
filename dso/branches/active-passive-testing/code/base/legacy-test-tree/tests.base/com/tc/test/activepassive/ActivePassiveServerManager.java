/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.test.TestConfigObject;
import com.tc.util.PortChooser;

import java.io.File;
import java.io.FileNotFoundException;

public class ActivePassiveServerManager {
  private static final String                    HOST                       = "localhost";
  private static final String                    SERVER_NAME                = "testserver";
  private static final String                    CONFIG_FILE_NAME           = "active-passive-server-config.xml";
  private static final String                    MUTATE_VALIDATE_CRASH_MODE = "mutate-validate";

  private final File                             tempDir;
  private final PortChooser                      portChooser;
  private final String                           configModel;
  private final TestConfigObject                 testConfig;
  private final long                             startTimeout;

  private final int                              serverCount;
  private final String                           serverCrashMode;
  private final String                           serverCrashType;
  private final int                              serverCrashWaitTime;
  private final String                           serverPersistence;
  private final boolean                          serverDiskless;
  private final ActivePassiveServerConfigCreator serverConfigCreator;
  private final String                           configFileLocation;
  private final File                             configFile;

  private final ServerInfo[]                     servers;
  private final int[]                            dsoPorts;
  private final int[]                            jmxPorts;
  private final String[]                         serverNames;

  private int                                    activeIndex                = -1;

  public ActivePassiveServerManager(boolean isActivePassiveTest, File tempDir, PortChooser portChooser,
                                    String configModel, TestConfigObject testConfig, long startTimeout)
      throws Exception {
    if (!isActivePassiveTest) { throw new AssertionError("A non-ActivePassiveTest is trying to use this class."); }

    this.testConfig = testConfig;

    serverCount = this.testConfig.getActivePassiveServerCount();

    if (serverCount < 2) { throw new AssertionError("Active-passive tests involve 2 or more DSO servers: serverCount=["
                                                    + serverCount + "]"); }

    // TODO: remove this once JMX client code is implemented
    if (serverCount > 2) { throw new AssertionError(
                                                    "Active-passive tests for now involves 2 DSO servers: serverCount=["
                                                        + serverCount + "]"); }

    this.tempDir = tempDir;
    configFileLocation = this.tempDir + File.separator + CONFIG_FILE_NAME;
    configFile = new File(configFileLocation);

    this.portChooser = portChooser;
    this.configModel = configModel;
    this.startTimeout = startTimeout;

    // TODO: can be mutate-validate
    serverCrashMode = this.testConfig.getActivePassiveServerCrashMode();

    // TODO: implement time-based crash
    serverCrashWaitTime = this.testConfig.getActivePassiveServerCrashWaitTime();
    serverCrashType = this.testConfig.getActivePassiveServerCrashType();

    serverPersistence = this.testConfig.getActivePassiveServerPersistence();
    serverDiskless = this.testConfig.getActivePassiveServerDiskless();

    servers = new ServerInfo[this.serverCount];
    dsoPorts = new int[this.serverCount];
    jmxPorts = new int[this.serverCount];
    serverNames = new String[this.serverCount];
    createServers();

    serverConfigCreator = new ActivePassiveServerConfigCreator(this.serverCount, dsoPorts, jmxPorts, serverNames,
                                                               serverPersistence, serverDiskless, this.configModel,
                                                               configFile, this.tempDir);
    serverConfigCreator.writeL2Config();
  }

  private void createServers() throws FileNotFoundException {
    // TODO: get rid of this after L1 config has been worked out
    dsoPorts[0] = 9510;
    jmxPorts[0] = 9520;
    serverNames[0] = SERVER_NAME + 0;
    servers[0] = new ServerInfo(HOST, serverNames[0], dsoPorts[0], jmxPorts[0], getServerControl(dsoPorts[0],
                                                                                                 jmxPorts[0],
                                                                                                 serverNames[0]));
    dsoPorts[1] = 8510;
    jmxPorts[1] = 8520;
    serverNames[1] = SERVER_NAME + 1;
    servers[1] = new ServerInfo(HOST, serverNames[1], dsoPorts[1], jmxPorts[1], getServerControl(dsoPorts[1],
                                                                                                 jmxPorts[1],
                                                                                                 serverNames[1]));

    for (int i = 2; i < dsoPorts.length; i++) {
      dsoPorts[i] = getUnusedPort("dso");
      jmxPorts[i] = getUnusedPort("jmx");
      serverNames[i] = SERVER_NAME + i;
      servers[i] = new ServerInfo(HOST, serverNames[i], dsoPorts[i], jmxPorts[i], getServerControl(dsoPorts[i],
                                                                                                   jmxPorts[i],
                                                                                                   serverNames[i]));
    }
  }

  private int getUnusedPort(String type) {
    if (type == null || (!type.equalsIgnoreCase("dso") && !type.equalsIgnoreCase("jmx"))) { throw new AssertionError(
                                                                                                                     "Unrecognizable type=["
                                                                                                                         + type
                                                                                                                         + "]"); }
    int port = -1;
    while (port < 0) {
      int newPort = portChooser.chooseRandomPort();
      boolean used = false;
      for (int i = 0; i < dsoPorts.length; i++) {
        if (dsoPorts[i] == newPort) {
          used = true;
        }
      }
      if (used) {
        continue;
      }
      for (int i = 0; i < jmxPorts.length; i++) {
        if (jmxPorts[i] == newPort) {
          used = true;
        }
      }
      if (!used) {
        port = newPort;
      }
    }
    return port;
  }

  private ServerControl getServerControl(int dsoPort, int jmxPort, String serverName) throws FileNotFoundException {
    return new ExtraProcessServerControl(HOST, serverName, dsoPort, jmxPort, configFileLocation, true);
  }

  public void startServers() throws Exception {
    if (activeIndex < 0) {
      activeIndex = 0;
    }
    startActive();
    Thread.sleep(500);
    startPassives();
  }

  private void startActive() throws Exception {
    servers[activeIndex].getServerControl().start(startTimeout);
  }

  private void startPassives() throws Exception {
    for (int i = 0; i < servers.length; i++) {
      if (i != activeIndex) {
        servers[i].getServerControl().start(startTimeout);
      }
    }
  }

  // TODO: remove the debuggin comments in this method
  public void crashActive() throws Exception {
    System.err.println("***** Crashing active server ");

    servers[activeIndex].getServerControl().crash();
    // TODO: figure out which is next active and then set activeIndex accordingly
    // for now assume that there are only 2 servers, 1 active and 1 passive
    activeIndex = 1;

    System.err.println("***** Sleeping after crashing active server ");
    Thread.sleep(2000);
    System.err.println("***** Done sleeping after crashing active server ");
  }

  public int getServerCount() {
    return serverCount;
  }

  public int[] getDsoPorts() {
    return dsoPorts;
  }

  public int[] getJmxPorts() {
    return jmxPorts;
  }

  public boolean crashActiveServerAfterMutate() {
    if (serverCrashMode.equals(MUTATE_VALIDATE_CRASH_MODE)) { return true; }
    return false;
  }

  public void addServersToConfig(TestTVSConfigurationSetupManagerFactory configFactory) {
    for (int i = 0; i < serverCount; i++) {
      configFactory.addServerToL1Config(serverNames[i], dsoPorts[i], jmxPorts[i]);
    }
  }

  /*
   * Server inner class
   */
  private static class ServerInfo {
    private final String        server_host;
    private final String        server_name;
    private final int           server_dsoPort;
    private final int           server_jmxPort;
    private final ServerControl serverControl;
    private String              dataLocation;
    private String              logLocation;

    ServerInfo(String host, String name, int dsoPort, int jmxPort, ServerControl serverControl) {
      this.server_host = host;
      this.server_name = name;
      this.server_dsoPort = dsoPort;
      this.server_jmxPort = jmxPort;
      this.serverControl = serverControl;
    }

    public String getHost() {
      return server_host;
    }

    public String getName() {
      return server_name;
    }

    public int getDsoPort() {
      return server_dsoPort;
    }

    public int getJmxPort() {
      return server_jmxPort;
    }

    public ServerControl getServerControl() {
      return serverControl;
    }

    public void setDataLocation(String location) {
      dataLocation = location;
    }

    public String getDataLocation() {
      return dataLocation;
    }

    public void setLogLocation(String location) {
      logLocation = location;
    }

    public String getLogLocation() {
      return logLocation;
    }
  }

}
