/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server.util;

import org.apache.xmlbeans.XmlException;

import com.tc.cli.CommandLineBuilder;
import com.tc.config.Loader;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class ServerStat {
  private static final String   UNKNOWN          = "unknown";
  private static final String   NEWLINE          = System.getProperty("line.separator");
  static final int      DEFAULT_JMX_PORT = 9520;

  private final String          host;
  private final String          hostName;
  private final int             port;
  private final String          username;
  private final String          password;
  private final boolean         secured;
  private JMXConnector          jmxc;

  private TCServerInfoMBean     infoBean;
  private boolean               connected;
  private String                errorMessage     = "";

  public ServerStat(String username, String password, boolean secured, String host, String hostAlias, int port) {
    this.username = username;
    this.password = password;
    this.secured = secured;
    this.host = host;
    this.hostName = hostAlias;
    this.port = port;
    connect();
  }

  private static void closeQuietly(JMXConnector connector) {
    if (connector != null) {
      try {
        connector.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  private void connect() {
    closeQuietly(jmxc);
    try {
      jmxc = CommandLineBuilder.getJMXConnector(username, password, host, port, secured);
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      infoBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.TC_SERVER_INFO,
                                                               TCServerInfoMBean.class, false);
      connected = true;
    } catch (Exception e) {
      String rootCauseMessage = e.getMessage() != null ? e.getMessage() : e.getCause().getMessage();
      errorMessage = "Failed to connect to " + host + ":" + port + ". "
                     + (rootCauseMessage != null ? rootCauseMessage : "");
      closeQuietly(jmxc);
      connected = false;
    }
  }

  public String getState() {
    if (!connected) return UNKNOWN;
    return infoBean.getState();
  }

  public String getRole() {
    if (!connected) return UNKNOWN;
    return infoBean.isActive() ? "ACTIVE" : "PASSIVE";
  }

  public String getHealth() {
    if (!connected) return "FAILED";
    return infoBean.getHealthStatus();
  }

  @Override
  public String toString() {
    String serverId = hostName != null ? hostName : host;
    StringBuilder sb = new StringBuilder();
    sb.append(serverId).append(".health: ").append(getHealth()).append(NEWLINE);
    sb.append(serverId).append(".role: ").append(getRole()).append(NEWLINE);
    sb.append(serverId).append(".state: ").append(getState()).append(NEWLINE);
    sb.append(serverId).append(".jmxport: ").append(port).append(NEWLINE);
    if (!connected) {
      sb.append(serverId).append(".error: ").append(errorMessage).append(NEWLINE);
    }
    return sb.toString();
  }

  /**
   * Dispose any active JMX Connection properly
   */
  public void dispose() {
    closeQuietly(jmxc);
    connected = false;
    errorMessage = "jmx connection was closed";
  }

  public static void main(String[] args) throws Exception {
    String usage = " server-stat -s host1,host2" + NEWLINE + "       server-stat -s host1:9520,host2:9520" + NEWLINE
                   + "       server-stat -f /path/to/tc-config.xml" + NEWLINE;

    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ServerStat.class.getName(), args);

    commandLineBuilder.addOption("s", true, "Terracotta server instance list (comma separated)", String.class, false,
                                 "list");
    commandLineBuilder.addOption("f", true, "Terracotta tc-config file", String.class, false, "file");
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.addOption(null, "secured", false, "secured", String.class, false);
    commandLineBuilder.addOption("u", "username", true, "username", String.class, false);
    commandLineBuilder.addOption("w", "password", true, "password", String.class, false);
    commandLineBuilder.setUsageMessage(usage);
    commandLineBuilder.parse();

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie();
    }

    boolean secured = false;
    if (commandLineBuilder.hasOption("secured")) {
      initSecurityManager();
      secured = true;
    }

    String username = null;
    String password = null;
    if (commandLineBuilder.hasOption('u')) {
      username = commandLineBuilder.getOptionValue('u');
      if (commandLineBuilder.hasOption('w')) {
        password = commandLineBuilder.getOptionValue('w');
      } else {
        password = CommandLineBuilder.readPassword();
      }
    }

    String hostList = commandLineBuilder.getOptionValue('s');
    String configFile = commandLineBuilder.getOptionValue('f');

    try {
      if (configFile != null) {
        handleConfigFile(username, password, secured, configFile);
      } else {
        handleList(username, password, secured, hostList);
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static void initSecurityManager() throws Exception {
    final Class<?> securityManagerClass = Class.forName("com.tc.net.core.security.TCClientSecurityManager");
    securityManagerClass.getConstructor(boolean.class).newInstance(true);
  }

  private static void handleConfigFile(String username, String password, boolean secured, String configFile) throws Exception {
    TcConfigDocument tcConfigDocument;
    try {
      tcConfigDocument = new Loader().parse(new File(configFile));
    } catch (IOException e) {
      throw new RuntimeException("Error reading " + configFile + ": " + e.getMessage());
    } catch (XmlException e) {
      throw new RuntimeException("Error parsing " + configFile + ": " + e.getMessage());
    }
    TcConfig tcConfig = tcConfigDocument.getTcConfig();
    Servers tcConfigServers = tcConfig.getServers();
    Server[] servers = tcConfigServers.getServerArray();
    for (Server server : servers) {
      String host = server.getHost();
      String hostName = server.getName();
      int jmxPort = computeJMXPort(server);
      if (!secured && tcConfigServers.isSetSecure() && tcConfigServers.getSecure()) {
        initSecurityManager();
        secured = true;
      }
      ServerStat stat = new ServerStat(username, password, secured, host, hostName, jmxPort);
      System.out.println(stat.toString());
      stat.dispose();
    }
  }

  static int computeJMXPort(Server server) {
    if (server.isSetJmxPort()) {
      return server.getJmxPort().getIntValue() == 0 ? DEFAULT_JMX_PORT : server.getJmxPort().getIntValue();
    } else {
      return L2DSOConfigObject.computeJMXPortFromTSAPort(server.getDsoPort().getIntValue());
    }
  }

  private static void handleList(String username, String password, boolean secured, String hostList) {
    if (hostList == null) {
      printStat(username, password, secured, "localhost:9520");
    } else {
      String[] pairs = hostList.split(",");
      for (String info : pairs) {
        printStat(username, password, secured, info);
        System.out.println();
      }
    }
  }

  // info = host | host:port
  private static void printStat(String username, String password, boolean secured, String info) {
    String host = info;
    int port = DEFAULT_JMX_PORT;
    if (info.indexOf(':') > 0) {
      String[] args = info.split(":");
      host = args[0];
      try {
        port = Integer.valueOf(args[1]);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Failed to parse jmxport: " + info);
      }
    }
    ServerStat stat = new ServerStat(username, password, secured, host, null, port);
    System.out.println(stat.toString());
    stat.dispose();
  }
}
