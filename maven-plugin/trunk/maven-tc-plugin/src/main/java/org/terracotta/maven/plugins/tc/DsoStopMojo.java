/**
 * 
 */
package org.terracotta.maven.plugins.tc;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.terracotta.maven.plugins.tc.cl.CommandLineException;
import org.terracotta.maven.plugins.tc.cl.CommandLineUtils;
import org.terracotta.maven.plugins.tc.cl.Commandline;

import com.tc.admin.TCStop;

/**
 * @author Eugene Kuleshov
 * 
 * @goal stop
 * @requiresDependencyResolution runtime
 * @execute phase="validate"
 */
public class DsoStopMojo extends AbstractDsoMojo {

  /**
   * @parameter expression="${spawnServer}" default-value="true"
   */
  private boolean spawnServer;

  /**
   * @parameter expression="${serverName}"
   * @optional
   */
  private String serverName;

  /**
   * @parameter expression="${startServer}" default-value="true"
   */
  boolean startServer;

  public DsoStopMojo() {
  }
  
  public DsoStopMojo(AbstractDsoMojo mojo) {
    super(mojo);
  }
  
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!startServer) {
      getLog().info("Skipping stopping DSO Server");
      return;
    }

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
      getLog().info("OK");
    } catch (CommandLineException e) {
      getLog().error("Failed to stop DSO server", e);
    }
  }

  
  // setters for the lifecycle simulation 
  
  public void setServerName(String serverName) {
    this.serverName = serverName;
  }
  
  public void setSpawnServer(boolean spawn) {
    this.spawnServer = spawn;
  }
  
  public void setStartServer(boolean startServer) {
    this.startServer = startServer;
  }
  
}
