/**
 * 
 */
package org.terracotta.maven.plugins.tc;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.terracotta.maven.plugins.tc.cl.CommandLineException;
import org.terracotta.maven.plugins.tc.cl.CommandLineUtils;
import org.terracotta.maven.plugins.tc.cl.Commandline;

import com.tc.server.TCServerMain;

/**
 * @author Eugene Kuleshov
 * 
 * @goal start
 * @requiresDependencyResolution runtime
 * @execute phase="validate"
 */
public class DsoStartMojo extends AbstractDsoMojo {

  /**
   * @parameter expression="${spawn}" default-value="true"
   */
  private boolean spawn;

  /**
   * @parameter expression="${name}"
   * @optional
   */
  private String name;

  /**
   * @parameter expression="${startServer}" default-value="true"
   */
  boolean startServer;

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!startServer) {
      getLog().info("Skipping starting DSO Server");
      return;
    }

    Commandline cmd = createCommandLine();

    cmd.createArgument().setValue("-Dtc.classpath=" + createPluginClasspath());

    cmd.createArgument().setValue("-cp");
    cmd.createArgument().setValue(createPluginClasspath());

    cmd.createArgument().setValue(TCServerMain.class.getName());

    cmd.createArgument().setValue("-f");
    cmd.createArgument().setFile(config);
    getLog().debug("tc-config file  = " + config.getAbsolutePath());

    if (name != null && name.length() > 0) {
      cmd.createArgument().setValue("-n");
      cmd.createArgument().setValue(name);
      getLog().debug("server name = " + name);
    }

    ForkedProcessStreamConsumer streamConsumer = new ForkedProcessStreamConsumer("dso start");

    getLog().info("------------------------------------------------------------------------");
    getLog().info("Starting DSO Server");
    try {
      CommandLineUtils.executeCommandLine(cmd, null, streamConsumer, streamConsumer, spawn);
      getLog().info("OK");
    } catch (CommandLineException e) {
      getLog().error("Failed to start DSO server", e);
    }
  }

}
