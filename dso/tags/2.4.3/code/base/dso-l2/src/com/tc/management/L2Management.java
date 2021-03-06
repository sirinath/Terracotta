/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2Dumper;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCDumper;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitor;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.PortChooser;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;

public class L2Management extends TerracottaManagement {

  private MBeanServer                          mBeanServer;
  private JMXConnectorServer                   jmxConnectorServer;
  private final L2TVSConfigurationSetupManager configurationSetupManager;
  private final TCServerInfoMBean              tcServerInfo;
  private final TCDumper                       tcDumper;
  private final ObjectManagementMonitor        objectManagementBean;
  private static final Map                     rmiRegistryMap = new HashMap();

  public L2Management(TCServerInfoMBean tcServerInfo, L2TVSConfigurationSetupManager configurationSetupManager,
                      TCDumper tcDumper) throws MBeanRegistrationException, NotCompliantMBeanException,
      InstanceAlreadyExistsException {
    this.tcServerInfo = tcServerInfo;
    this.configurationSetupManager = configurationSetupManager;
    this.tcDumper = tcDumper;

    try {
      objectManagementBean = new ObjectManagementMonitor();
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException(
                                   "Unable to construct one of the L2 MBeans: this is a programming error in one of those beans",
                                   ncmbe);
    }

    // LKC-2990 and LKC-3171: Remove the JMX generic optional logging
    java.util.logging.Logger jmxLogger = java.util.logging.Logger.getLogger("javax.management.remote.generic");
    jmxLogger.setLevel(java.util.logging.Level.OFF);

    final List jmxServers = MBeanServerFactory.findMBeanServer(null);
    if (jmxServers != null && !jmxServers.isEmpty()) {
      mBeanServer = (MBeanServer) jmxServers.get(0);
    } else {
      mBeanServer = MBeanServerFactory.createMBeanServer();
    }
    registerMBeans();
  }

  /**
   * Keep track of RMI Registries by jmxPort. In 1.5 and forward you can create multiple RMI Registries in a single VM.
   */
  private static Registry getRMIRegistry(int jmxPort) throws RemoteException {
    Integer key = new Integer(jmxPort);
    Registry registry = (Registry) rmiRegistryMap.get(key);
    if (registry == null) {
      rmiRegistryMap.put(key, registry = LocateRegistry.createRegistry(jmxPort));
    }
    return registry;
  }

  public synchronized void start() throws Exception {
    int jmxPort = configurationSetupManager.commonl2Config().jmxPort().getInt();
    if (jmxPort == 0) {
      jmxPort = new PortChooser().chooseRandomPort();
    }
    JMXServiceURL url;
    Map env = new HashMap();
    String authMsg = "Authentication OFF";
    String credentialsMsg = "";
    env.put("jmx.remote.x.server.connection.timeout", new Long(Long.MAX_VALUE));
    if (configurationSetupManager.commonl2Config().authentication()) {
      String pwd = configurationSetupManager.commonl2Config().authenticationPasswordFile();
      String access = configurationSetupManager.commonl2Config().authenticationAccessFile();
      if (!new File(pwd).exists()) CustomerLogging.getConsoleLogger().error("Password file does not exist: " + pwd);
      if (!new File(access).exists()) CustomerLogging.getConsoleLogger().error("Access file does not exist: " + access);
      env.put("jmx.remote.x.password.file", pwd);
      env.put("jmx.remote.x.access.file", access);
      authMsg = "Authentication ON";
      credentialsMsg = "Credentials: " + configurationSetupManager.commonl2Config().authenticationPasswordFile() + " "
                       + configurationSetupManager.commonl2Config().authenticationAccessFile();
      url = new JMXServiceURL("service:jmx:rmi://");
      RMIJRMPServerImpl server = new RMIJRMPServerImpl(jmxPort, null, null, env);
      jmxConnectorServer = new RMIConnectorServer(url, env, server, mBeanServer);
      jmxConnectorServer.start();
      getRMIRegistry(jmxPort).bind("jmxrmi", server);
      CustomerLogging.getConsoleLogger().info(
                                              "JMX Server started. " + authMsg + " - Available at URL["
                                                  + "service:jmx:rmi:///jndi/rmi://localhost:" + jmxPort + "/jmxrmi"
                                                  + "]");
      if (!credentialsMsg.equals("")) CustomerLogging.getConsoleLogger().info(credentialsMsg);
    } else {
      url = new JMXServiceURL("jmxmp", "localhost", jmxPort);
      jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mBeanServer);
      jmxConnectorServer.start();
      CustomerLogging.getConsoleLogger().info("JMX Server started. Available at URL[" + url + "]");
    }
  }

  public synchronized void stop() throws IOException, InstanceNotFoundException, MBeanRegistrationException {
    unregisterMBeans();
    if (jmxConnectorServer != null) {
      jmxConnectorServer.stop();
    }
  }

  public Object findMBean(ObjectName objectName, Class mBeanInterface) throws IOException {
    return findMBean(objectName, mBeanInterface, mBeanServer);
  }

  public MBeanServer getMBeanServer() {
    return mBeanServer;
  }

  public ObjectManagementMonitor findObjectManagementMonitorMBean() {
    return objectManagementBean;
  }

  private void registerMBeans() throws MBeanRegistrationException, NotCompliantMBeanException,
      InstanceAlreadyExistsException {
    mBeanServer.registerMBean(tcServerInfo, L2MBeanNames.TC_SERVER_INFO);
    mBeanServer.registerMBean(TCLogging.getJMXAppender().getMBean(), L2MBeanNames.LOGGER);
    mBeanServer.registerMBean(objectManagementBean, L2MBeanNames.OBJECT_MANAGEMENT);

    if (TCPropertiesImpl.getProperties().getBoolean("tc.management.test.mbeans.enabled")) {
      mBeanServer.registerMBean(new L2Dumper(tcDumper), L2MBeanNames.DUMPER);
    }
  }

  private void unregisterMBeans() throws InstanceNotFoundException, MBeanRegistrationException {
    mBeanServer.unregisterMBean(L2MBeanNames.TC_SERVER_INFO);
    mBeanServer.unregisterMBean(L2MBeanNames.LOGGER);
    mBeanServer.unregisterMBean(L2MBeanNames.OBJECT_MANAGEMENT);

    if (TCPropertiesImpl.getProperties().getBoolean("tc.management.test.mbeans.enabled")) {
      mBeanServer.unregisterMBean(L2MBeanNames.DUMPER);
    }
  }
}
