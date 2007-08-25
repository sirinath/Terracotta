/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.lang.ArrayUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.terracotta.maven.plugins.tc.cl.Commandline;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.logging.NullTCLogger;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.terracottatech.config.Module;

/**
 * @author Eugene Kuleshov
 */
public abstract class AbstractDsoMojo extends AbstractMojo {

  /**
   * ArtifactRepository of the localRepository. To obtain the directory of localRepository in unit tests use
   * System.setProperty( "localRepository").
   * 
   * @parameter expression="${localRepository}"
   * @required
   * @readonly
   */
  protected ArtifactRepository localRepository;
  
  /**
   * Creates the artifact
   * 
   * @component
   */
  protected ArtifactFactory artifactFactory;
  
  /**
   * Resolves the artifacts needed.
   * 
   * @component
   */
  protected ArtifactResolver artifactResolver;

  /**
   * The plugin remote repositories declared in the pom.
   * 
   * @parameter expression="${project.remoteArtifactRepositories}"
   */
  protected List remoteRepositories;

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
    
    localRepository = mojo.localRepository;
    remoteRepositories = mojo.remoteRepositories;
    artifactFactory = mojo.artifactFactory;
    artifactResolver = mojo.artifactResolver;
  }

  protected Commandline createCommandLine() {
    Commandline cmd = new Commandline();

    if (jvm != null && jvm.length() > 0) {
      cmd.setExecutable(jvm);
    } else {
      cmd.setExecutable(System.getProperty("java.home") + "/bin/java");
    }

    String modulesRepository = getModulesRepository(); 
    cmd.createArgument().setValue("-Dtc.tests.configuration.modules.url=" + modulesRepository);
    getLog().debug("tc.tests.configuration.modules.url = " + modulesRepository);

    // DSO debugging
    if (getLog().isDebugEnabled()) {
      int port = ++debugPort;
      cmd.createArgument().setValue("-Xdebug");
      cmd.createArgument().setValue("-Xrunjdwp:transport=dt_socket,suspend=y,server=y,address=" + port);
//      cmd.createArgument().setValue("-Xrunjdwp:transport=dt_socket,server=y,address=" + port);

      cmd.createArgument().setValue("-Dtc.classloader.writeToDisk=true");
    }

    getLog().debug("cmd: " + cmd);

    return cmd;
  }

  protected String getModulesRepository() {
    return new File(localRepository.getBasedir()).toURI().toString();
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

  public String getServerStatus(String serverName) throws ConfigurationSetupException, MalformedURLException {
    String host = "localhost";
    int port = 9520;

    if (config != null && config.exists()) {
      String args = "";
      if (config != null) {
        args += "-f " + config.getAbsolutePath();
      }
      if (serverName != null) {
        args += " -n " + serverName;
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
    
    JMXConnector jmxc = null;
    try {
      jmxc = JMXConnectorFactory.connect(url, null);
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      TCServerInfoMBean serverMBean = (TCServerInfoMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc,
          L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);

      return serverMBean.getHealthStatus();

    } catch (IOException ex) {
      getLog().debug("Connection error: " + ex.toString(), ex);
      return null;
      
    } finally {
      if (jmxc != null) {
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

  public void resolveModuleArtifacts(boolean addSurefireModule) {
    try {
      String[] commandLine = config == null ? new String[] {} : new String[] { "-f", config.getAbsolutePath() };
      StandardTVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(
          commandLine, false, new FatalIllegalConfigurationChangeHandler());

      NullTCLogger tcLogger = new NullTCLogger();
      L1TVSConfigurationSetupManager config = factory.createL1TVSConfigurationSetupManager(tcLogger);

      if (config.commonL1Config().modules() != null && config.commonL1Config().modules().getModuleArray() != null) {
        Module[] moduleArray = config.commonL1Config().modules().getModuleArray();
        for (int i = 0; i < moduleArray.length; i++) {
          Module module = moduleArray[i];
          String groupId = module.getGroupId() == null ? "org.terracotta.modules" : module.getGroupId();
          String artifactId = module.getName();
          VersionRange version = VersionRange.createFromVersionSpec(module.getVersion());

          resolveArtifact(groupId, artifactId, version);
        }
      }
      
      if(addSurefireModule) {
        VersionRange version = VersionRange.createFromVersionSpec("[1.0.0,)");

        resolveArtifact("org.terracotta.modules", "modules-common-1.0", version);
        resolveArtifact("org.terracotta.modules", "clustered-surefire-2.3", version);
      }

    } catch (ConfigurationSetupException ex) {
      getLog().error(ex);
    } catch (InvalidVersionSpecificationException ex) {
      getLog().error(ex);
    }
  }

  private Artifact resolveArtifact(String groupId, String artifactId, VersionRange version) {
    Artifact artifact = artifactFactory.createDependencyArtifact( // 
        groupId, artifactId, version, "jar", null, Artifact.SCOPE_RUNTIME);
    getLog().info("Resolving module " + artifact.toString());
    try {
      artifactResolver.resolve(artifact, remoteRepositories, localRepository);
    } catch (ArtifactResolutionException ex) {
      getLog().error(ex);
    } catch (ArtifactNotFoundException ex) {
      getLog().error(ex);
    }
    return artifact;
  }

}
