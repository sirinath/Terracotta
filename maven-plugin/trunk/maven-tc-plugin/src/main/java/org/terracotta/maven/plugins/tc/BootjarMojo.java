/**
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.terracotta.maven.plugins.tc.cl.CommandLineException;
import org.terracotta.maven.plugins.tc.cl.CommandLineUtils;
import org.terracotta.maven.plugins.tc.cl.Commandline;

import com.tc.object.tools.BootJarTool;

/**
 * @author Eugene Kuleshov
 * 
 * @goal bootjar
 * @execute phase="validate"
 * @phase integration-test
 * @requiresDependencyResolution runtime
 */
public class BootjarMojo extends AbstractDsoMojo {

  /**
   * @parameter expression="${verbose}" default-value="false"
   */
  private boolean verbose;

  /**
   * @parameter expression="${overwriteBootjar}" default-value="false"
   */
  private boolean overwriteBootjar;

  /**
   * @parameter expression="${bootjar}" default-value="${project.build.directory}/dso-boot.jar"
   */
  private File bootJar;

  public BootjarMojo() {
  }

  public BootjarMojo(AbstractDsoMojo mojo) {
    super(mojo);
  }
  
  /**
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  public void execute() throws MojoExecutionException {
    resolveModuleArtifacts(false);
    
    if (!overwriteBootjar && bootJar.exists()) {
      getLog().info("BootJar already exist " + bootJar.getAbsolutePath());
      return;
    }

    Commandline cmd = createCommandLine();

    cmd.createArgument().setValue("-Dtc.classpath=" + createPluginClasspath());

    cmd.createArgument().setValue("-cp");
    cmd.createArgument().setValue(createPluginClasspath());

    cmd.createArgument().setValue(BootJarTool.class.getName());

    if (verbose) {
      cmd.createArgument().setValue("-v");
    }

    if (overwriteBootjar) {
      cmd.createArgument().setValue("-w");
    }

    cmd.createArgument().setValue("-o");
    cmd.createArgument().setFile(bootJar);
    getLog().debug("bootjar file  = " + bootJar.getAbsolutePath());

    cmd.createArgument().setValue("-f");
    cmd.createArgument().setFile(config);
    getLog().debug("tc-config file  = " + config.getAbsolutePath());

    ForkedProcessStreamConsumer streamConsumer = new ForkedProcessStreamConsumer("bootjar");

    getLog().info("------------------------------------------------------------------------");
    getLog().info("Statrting bootjar tool");
    try {
      CommandLineUtils.executeCommandLine(cmd, null, streamConsumer, streamConsumer, false);
      getLog().info("OK");
    } catch (CommandLineException e) {
      getLog().error("Failed to execute bootjar tool", e);
    }
  }

  
  // setters for the lifecycle simulation 
  
  public void setBootJar(File bootJar) {
    this.bootJar = bootJar;
  }
  
  public void setOverwriteBootjar(boolean overwriteBootjar) {
    this.overwriteBootjar = overwriteBootjar;
  }
  
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }
  
}
