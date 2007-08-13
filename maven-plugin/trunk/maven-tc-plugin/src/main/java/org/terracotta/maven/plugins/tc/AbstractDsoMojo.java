/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.lang.ArrayUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.terracotta.maven.plugins.tc.cl.Commandline;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;

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
  protected List classpathElements;

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

  public AbstractDsoMojo() {
  }

  public AbstractDsoMojo(AbstractDsoMojo mojo) {
    setLog(mojo.getLog());
    setPluginContext(mojo.getPluginContext());

    modules = mojo.modules;
    config = mojo.config;
    jvm = mojo.jvm;
    classpathElements = mojo.classpathElements;
    pluginArtifacts = mojo.pluginArtifacts;
  }

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

    if (getLog().isDebugEnabled()) {
      cmd.createArgument().setValue("-Dtc.classloader.writeToDisk=true");
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

  public String getServerStatus(String serverName) {
    String host = "localhost";
    int port = 9520;
    
    JMXConnector jmxc = null;
    try {
      if(config!=null && config.exists()) {
        String args = "";
        if (serverName != null) {
          args += "-n " + serverName;
        }
        if (config != null) {
          args += " -f " + config.getName();
        }
    
        TVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(args.split(" "), //
            true, new MavenIllegalConfigurationChangeHandler());
    
        L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(serverName);
        
        NewCommonL2Config serverConfig = manager.commonL2ConfigFor(serverName);
    
        host = serverConfig.host().getString();
        if (host == null) {
          host = "localhost";
        }
        
        port = serverConfig.jmxPort().getInt();
      }
    
      String jmxUrl = "service:jmx:jmxmp://" + host + ":" + port;
      getLog().debug("Connecting to DSO server at " + jmxUrl);
      
      JMXServiceURL url = new JMXServiceURL(jmxUrl);
      jmxc = JMXConnectorFactory.connect(url, null);
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      TCServerInfoMBean serverMBean = (TCServerInfoMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc,
          L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      
      return serverMBean.getHealthStatus();
    } catch(Exception ex) {
      getLog().debug("Connection error: " + ex.toString(), ex);
      return null;
      
    } finally {
      if(jmxc!=null) {
        try {
          jmxc.close();
        } catch (IOException ex) {
          getLog().error("Error closing jmx connection", ex);
        }
      }
    }
  }

  private final class MavenIllegalConfigurationChangeHandler implements IllegalConfigurationChangeHandler {
    public void changeFailed(ConfigItem item, Object oldValue, Object newValue) {
      String text = "Inconsistent Terracotta configuration.\n\n"
          + "The configuration that this client is using is different from the one used by\n"
          + "the connected production server.\n\n" + "Specific information: " + item + " has changed.\n"
          + "   Old value: " + describe(oldValue) + "\n" + "   New value: " + describe(newValue) + "\n";
      getLog().error(text);
    }

    private String describe(Object o) {
      if (o == null) {
        return "<null>";
      } else if (o.getClass().isArray()) {
        return ArrayUtils.toString(o);
      } else {
        return o.toString();
      }
    }
  }

}
