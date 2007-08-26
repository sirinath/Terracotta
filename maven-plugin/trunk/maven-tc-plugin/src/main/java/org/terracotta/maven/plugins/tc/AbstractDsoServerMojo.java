package org.terracotta.maven.plugins.tc;

import org.terracotta.maven.plugins.tc.cl.CommandLineException;
import org.terracotta.maven.plugins.tc.cl.CommandLineUtils;
import org.terracotta.maven.plugins.tc.cl.Commandline;

import com.tc.admin.TCStop;
import com.tc.server.TCServerMain;

public abstract class AbstractDsoServerMojo extends AbstractDsoMojo {
  /**
   * @parameter expression="${spawnServer}" default-value="true"
   */
  private boolean spawnServer;

  /**
   * @parameter expression="${serverName}"
   * @optional
   */
  private String serverName;

  public AbstractDsoServerMojo() {
  }

  public AbstractDsoServerMojo(AbstractDsoMojo mojo) {
    super(mojo);
  }

  protected void start() {
    String jmxUrl = null;
    try {
      jmxUrl = getJMXUrl(serverName);

      String status = getServerStatus(jmxUrl);
      if (status != null && status.startsWith("OK")) {
        getLog().info("Server already started: " + status);
        return;
      }
    } catch (Exception e) {
      getLog().error("Failed to verify DSO server status", e);
    }

    Commandline cmd = createCommandLine();

    cmd.createArgument().setValue("-Dtc.classpath=" + createPluginClasspath());

    cmd.createArgument().setValue("-cp");
    cmd.createArgument().setValue(createPluginClasspath());

    cmd.createArgument().setValue(TCServerMain.class.getName());

    if (config.exists()) {
      getLog().debug("tc-config file " + config.getAbsolutePath());
      cmd.createArgument().setValue("-f");
      cmd.createArgument().setFile(config);
    } else {
      getLog().debug("tc-config file doesn't exists " + config.getAbsolutePath());
    }

    if (serverName != null && serverName.length() > 0) {
      cmd.createArgument().setValue("-n");
      cmd.createArgument().setValue(serverName);
      getLog().debug("server serverName = " + serverName);
    }

    ForkedProcessStreamConsumer streamConsumer = new ForkedProcessStreamConsumer("dso start");

    getLog().info("------------------------------------------------------------------------");
    getLog().info("Starting DSO Server");
    try {
      Process p = CommandLineUtils.executeCommandLine(cmd, null, streamConsumer, streamConsumer, spawnServer);
      getLog().info("OK");

      if (jmxUrl != null) {
        long time = System.currentTimeMillis();
        String status = null;
        while ((System.currentTimeMillis() - time) < 30 * 1000L && status == null && isRunning(p)) {
          status = getServerStatus(jmxUrl);
        }
        getLog().info("DSO Server status: " + status);
      }

    } catch (Exception e) {
      getLog().error("Failed to start DSO server", e);
    }
  }

  protected void stop() { stop(false); }
  
  protected void stop(boolean wait) {
    Commandline cmd = createCommandLine();

    cmd.createArgument().setValue("-Dtc.classpath=" + createPluginClasspath());

    cmd.createArgument().setValue("-cp");
    cmd.createArgument().setValue(createPluginClasspath());

    cmd.createArgument().setValue(TCStop.class.getName());

    if(config.exists()) {
      cmd.createArgument().setValue("-f");
      cmd.createArgument().setFile(config);
      getLog().debug("tc-config file  = " + config.getAbsolutePath());
    }

    if (serverName != null && serverName.length() > 0) {
      cmd.createArgument().setValue("-n");
      cmd.createArgument().setValue(serverName);
      getLog().debug("server name = " + serverName);
    }

    ForkedProcessStreamConsumer streamConsumer = new ForkedProcessStreamConsumer("dso start");

    getLog().info("------------------------------------------------------------------------");
    getLog().info("Stopping DSO Server");
    try {
      CommandLineUtils.executeCommandLine(cmd, null, streamConsumer, streamConsumer, spawnServer);
      
      if (wait) {
        getLog().info("Waiting for server to stop...");
        try {
          String jmxUrl = getJMXUrl(serverName);
          String status = null;
          long time = System.currentTimeMillis();
          do {
            status = getServerStatus(jmxUrl);
          }  while ((System.currentTimeMillis() - time) < 30 * 1000L && status != null);
        } catch (Exception e) {
          getLog().warn(e);
        }
      }

      getLog().info("OK");
  
    } catch (CommandLineException e) {
      getLog().error("Failed to stop DSO server", e);
    }
  }

  private boolean isRunning(Process p) {
    try {
      p.exitValue();
      return false;
    } catch (IllegalThreadStateException e) {
      return true;
    }
  }
  
  //setters for the lifecycle simulation 
  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  public void setSpawnServer(boolean spawn) {
    this.spawnServer = spawn;
  }
}
