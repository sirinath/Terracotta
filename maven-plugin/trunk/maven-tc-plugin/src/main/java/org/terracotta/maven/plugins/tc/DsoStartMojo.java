/**
 * 
 */
package org.terracotta.maven.plugins.tc;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.terracotta.maven.plugins.tc.cl.CommandLineUtils;
import org.terracotta.maven.plugins.tc.cl.Commandline;

import com.tc.server.TCServerMain;

/**
 * @author Eugene Kuleshov
 * 
 * @goal start
 * @requiresDependencyResolution runtime
 * @execute phase="validate"
 * @requiresDependencyResolution runtime
 */
public class DsoStartMojo extends AbstractDsoMojo {

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
   * Only set by DsoLifecycleMojo 
   */
  private boolean startServer = true;

  public DsoStartMojo() {
  }
  
  public DsoStartMojo(AbstractDsoMojo mojo) {
    super(mojo);
  }
  
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!startServer) {
      getLog().info("Skipping starting DSO Server");
      return;
    }

    try {
      String status = getServerStatus(serverName);
      if(status!=null && status.startsWith("OK")) {
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

    if(config.exists()) {
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
      
      long time = System.currentTimeMillis();
      String status = null;
      while((System.currentTimeMillis()-time) < 30 * 1000L && status==null && isRunning(p)) {
        status = getServerStatus(serverName);
      }
      
      getLog().info("DSO Server status: " + status);
      
    } catch (Exception e) {
      getLog().error("Failed to start DSO server", e);
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
