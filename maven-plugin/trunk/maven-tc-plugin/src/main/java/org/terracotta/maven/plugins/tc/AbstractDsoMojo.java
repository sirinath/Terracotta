/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.osgi.framework.BundleException;
import org.terracotta.maven.plugins.tc.cl.Commandline;

import com.tc.bundles.BundleSpec;
import com.tc.bundles.Resolver;
import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
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
   * @parameter expression="${project.pluginArtifactRepositories}"
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

  public String getServerStatus(String jmxUrl) throws ConfigurationSetupException, MalformedURLException {
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
      log("Connection error: " + ex.toString(), ex);
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

  protected String getJMXUrl(String serverName) throws ConfigurationSetupException {
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

    return "service:jmx:jmxmp://" + host + ":" + port;
  }

  protected void resolveModuleArtifacts(boolean addSurefireModule) throws MojoExecutionException {
    List modules = new ArrayList();

    try {
      String[] commandLine = config == null ? new String[] {} : new String[] { "-f", config.getAbsolutePath() };
      StandardTVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(
          commandLine, false, new MavenIllegalConfigurationChangeHandler());

      NullTCLogger tcLogger = new NullTCLogger();
      L1TVSConfigurationSetupManager config = factory.createL1TVSConfigurationSetupManager(tcLogger);

      if (config.commonL1Config().modules() != null && config.commonL1Config().modules().getModuleArray() != null) {
        Module[] moduleArray = config.commonL1Config().modules().getModuleArray();
        modules.addAll(Arrays.asList(moduleArray));
      }
    } catch (ConfigurationSetupException ex) {
      String msg = "Can't read Terracotta configuration from " + config.getAbsolutePath();
      log(msg, ex);
      throw new MojoExecutionException(msg, ex);
    }
     
    if(addSurefireModule) {
      Module surefireModule = Module.Factory.newInstance();
      surefireModule.setGroupId("org.terracotta.modules");
      surefireModule.setName("clustered-surefire-2.3");
      surefireModule.setVersion("1.0.0");
      
      modules.add(surefireModule);
    }

    try {
      MavenResolver resolver = new MavenResolver();
      resolver.resolve((Module[]) modules.toArray(new Module[modules.size()]));
    } catch (BundleException ex) {
      String msg = "Can't resolve module artifacts";
      log(msg, ex);
      throw new MojoExecutionException(msg, ex);
    }
  }

  void log(String msg, Exception ex) {
    if(getLog().isDebugEnabled()) {
      getLog().error(msg, ex);
    } else {
      getLog().error(msg);
    }
  }

  
  private class MavenResolver extends Resolver {

    public MavenResolver() {
      super(new URL[0]);
    }
    
    protected URL resolveLocation(final String name, final String version, final String groupId) {
      return resolveArtifact(groupId, name, VersionRange.createFromVersion(version));
    }
    
    protected URL resolveBundle(BundleSpec spec) {
      String groupId = spec.getGroupId();
      String name = spec.getName().replace('_', '-');
      String versionSpec = spec.getVersion();
      try {
        VersionRange version;
        if("(any-version)".equals(versionSpec)) {
          version = VersionRange.createFromVersion("1.0.0");
        } else {
          version = VersionRange.createFromVersionSpec(versionSpec);
        }
        return resolveArtifact(groupId, name, version);
      } catch (InvalidVersionSpecificationException ex) {
        log("Invalid version spec " + versionSpec + " for " + spec, ex);
      }
      return null;
    }

    private URL resolveArtifact(String groupId, String artifactId, VersionRange version) {
      Artifact artifact = artifactFactory.createDependencyArtifact( // 
          groupId, artifactId, version, "jar", null, Artifact.SCOPE_RUNTIME);
      getLog().info("Resolving module " + artifact.toString());

      try {
        artifactResolver.resolve(artifact, remoteRepositories, localRepository);
        return artifact.getFile().toURL();
        
      } catch (MalformedURLException ex) {
        log("Malformed URL for " + artifact.toString(), ex);
      } catch (ArtifactResolutionException ex) {
        log("Can't resolve artifact " + artifact.toString(), ex);
      } catch (ArtifactNotFoundException ex) {
        log("Can't find artifact " + artifact.toString(), ex);
      }    
      return null;
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
