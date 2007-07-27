/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.terracotta.maven.plugins.tc.cl.Commandline;

/**
 * @author Eugene Kuleshov
 */
public abstract class AbstractDsoMojo extends AbstractMojo {

  /**
   * Project classpath.
   * 
   * @parameter expression="${project.compileClasspathElements}"
   * @required
   * @readonly
   */
  private List classpathElements;

  /**
   * Plugin artifacts
   * 
   * @parameter expression="${plugin.artifacts}"
   * @required
   * @readonly
   */
  protected List pluginArtifacts;

  /**
   * @parameter expression="${jvm}"
   * @optional
   */
  protected String jvm;

  /**
   * @parameter expression="${config}" default-value="tc-config.xml"
   */
  protected File config;

  /**
   * @parameter expression="${modules}"
   * @optional
   */
  protected String modules;

  protected int debugPort = 5000;

  protected Commandline createCommandLine() {
    Commandline cmd = new Commandline();

    if (jvm != null && jvm.length() > 0) {
      cmd.setExecutable(jvm);
    } else {
      cmd.setExecutable(System.getProperty("java.home") + "/bin/java");
    }

    if (modules != null && modules.trim().length() > 0) {
      String location = modules.trim();
      File modulesDir = new File(location);
      if (modulesDir.isDirectory() && modulesDir.exists()) {
        location = modulesDir.toURI().toString();
      }
      cmd.createArgument().setValue("-Dtc.tests.configuration.modules.url=" + location);
      getLog().debug("tc.tests.configuration.modules.url = " + location);
    }

    // DSO debugging
    // if(getLog().isDebugEnabled()) {
    // int port = ++debugPort;
    // cmd.createArgument().setValue("-Xdebug
    // -Xrunjdwp:transport=dt_socket,server=y,address=" + port);
    // // args += " -agentlib:jdwp=transport=dt_socket,address=localhost:" +
    // port;
    // cmd.createArgument().setValue("-Dtc.classloader.writeToDisk=true");
    // }

    return cmd;
  }

  protected String createProjectClasspath() {
    String classpath = "";
    for (Iterator it = classpathElements.iterator(); it.hasNext();) {
      classpath += File.pathSeparator + ((String) it.next());
    }
    return classpath;
  }

  protected String createPluginClasspath() {
    String classpath = "";
    for (Iterator it = pluginArtifacts.iterator(); it.hasNext();) {
      Artifact artifact = (Artifact) it.next();
      classpath += File.pathSeparator + artifact.getFile().getAbsolutePath();
    }
    return classpath;
  }

  class ForkedProcessStreamConsumer implements StreamConsumer {
    private String prefix;

    public ForkedProcessStreamConsumer(String prefix) {
      this.prefix = prefix;
    }

    public void consumeLine(String msg) {
      getLog().info("[" + prefix + "] " + msg);
    }
  }

}
