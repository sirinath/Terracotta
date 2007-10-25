/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.io.IOUtils;
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
import com.tc.bundles.BundleSpecImpl;
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
   * @parameter expression="${jvmargs}"
   * @optional
   */
  protected String jvmargs;

  /**
   * @parameter expression="${workingDirectory}" default-value="${basedir}"
   */
  protected File workingDirectory;

  /**
   * @parameter expression="${config}" default-value="${basedir}/tc-config.xml"
   */
  protected File config;

  /**
   * @parameter expression="${modules}"
   * @optional
   */
  protected String modules;

  /**
   * Configuration for the DSO-enabled processes
   * 
   * @parameter expression="${processes}"
   * @optional
   */
  protected ProcessConfiguration[] processes;

  protected int debugPort = 5000;

  public AbstractDsoMojo() {
  }

  public AbstractDsoMojo(AbstractDsoMojo mojo) {
    setLog(mojo.getLog());
    setPluginContext(mojo.getPluginContext());

    modules = mojo.modules;
    config = mojo.config;
    workingDirectory = mojo.workingDirectory;
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

    cmd.createArgument().setLine(createJvmArguments());

    return cmd;
  }

  protected String createJvmArguments() {
    StringBuffer sb = new StringBuffer();

    String modulesRepository = getModulesRepository();
    sb.append("-Dcom.tc.l1.modules.repositories=" + modulesRepository);
    getLog().debug("com.tc.l1.modules.repositories = " + modulesRepository);

    // DSO debugging
    if (getLog().isDebugEnabled()) {
      // int port = ++debugPort;
      // cmd.createArgument().setValue("-Xdebug");
      // cmd.createArgument().setValue("-Xrunjdwp:transport=dt_socket,server=y,address=" + port);
      // cmd.createArgument().setValue("-Xrunjdwp:transport=dt_socket,suspend=n,server=y,address=" + port);

      sb.append(" -Dtc.classloader.writeToDisk=true");
    }

    return sb.toString();
  }

  protected String getModulesRepository() {
    return new File(localRepository.getBasedir()).toURI().toString();
  }

  protected String createProjectClasspath() {
    String classpath = "";
    for (Iterator it = classpathElements.iterator(); it.hasNext();) {
      classpath += ((String) it.next()) + File.pathSeparator;
    }
    return classpath;
  }

  protected String createPluginClasspath() {
    String classpath = "";
    for (Iterator it = pluginArtifacts.iterator(); it.hasNext();) {
      Artifact artifact = (Artifact) it.next();
      if (Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) {
        String groupId = artifact.getGroupId();
        // XXX workaround to shorten the classpath
        if (!groupId.startsWith("org.apache.maven") //
            && !"org.codehaus.cargo".equals(groupId) //
            && !"org.springframework".equals(groupId)) {
          classpath += artifact.getFile().getAbsolutePath() + File.pathSeparator;
        }
      }
    }
    return classpath;
  }

  protected String createPluginClasspathAsFile() {
    FileOutputStream fos = null;
    File tempClasspath = null;
    try {
      tempClasspath = File.createTempFile("tc-classpath", ".tmp");
      tempClasspath.deleteOnExit();
      fos = new FileOutputStream(tempClasspath);
      IOUtils.write(createPluginClasspath(), fos);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create tc.classpath", e);
    } finally {
      IOUtils.closeQuietly(fos);
    }
    return tempClasspath.toURI().toString();
  }

  protected String createSessionClasspath() {
    for (Iterator it = pluginArtifacts.iterator(); it.hasNext();) {
      Artifact artifact = (Artifact) it.next();
      if ("terracotta".equals(artifact.getArtifactId())) {
        String version = artifact.getVersion();
        URL url = resolveArtifact(artifact.getGroupId(), "tc-session", version);
        if (url == null) {
          throw new RuntimeException("Can't resolve " + artifact.getGroupId() + ":tc-session:" + version);
        }
        String path = url.getPath();
        if (!new File(path).exists() && path.startsWith("/")) {
          path = path.substring(1);
        }
        return path;
      }
    }
    return "";
  }

  protected String quoteIfNeeded(String path) {
    if (path.indexOf(" ") > 0)
      return "\"" + path + "\"";
    return path;
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

  public String getServerStatus(String jmxUrl) throws MalformedURLException, IOException {
    getLog().debug("Connecting to DSO server at " + jmxUrl);
    JMXServiceURL url = new JMXServiceURL(jmxUrl);
    JMXConnector jmxc = null;
    try {
      jmxc = JMXConnectorFactory.connect(url, null);
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      TCServerInfoMBean serverMBean = (TCServerInfoMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc,
          L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);

      return serverMBean.getHealthStatus();
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

    String args = "-f" + config.getAbsolutePath();
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

    return "service:jmx:jmxmp://" + host + ":" + port;
  }

  protected void resolveModuleArtifacts() throws MojoExecutionException {
    resolveModuleArtifacts(Collections.EMPTY_LIST);
  }

  protected void resolveModuleArtifacts(List additionalModules) throws MojoExecutionException {
    List modules = new ArrayList();

    try {
      String[] commandLine = new String[] { "-f", config.getAbsolutePath() };
      StandardTVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(
          commandLine, false, new MavenIllegalConfigurationChangeHandler());

      NullTCLogger tcLogger = new NullTCLogger();
      L1TVSConfigurationSetupManager config = factory.createL1TVSConfigurationSetupManager(tcLogger);

      if (config.commonL1Config().modules() != null && config.commonL1Config().modules().getModuleArray() != null) {
        Module[] moduleArray = config.commonL1Config().modules().getModuleArray();
        modules.addAll(Arrays.asList(moduleArray));
      }
    } catch (ConfigurationSetupException ex) {
      throw new MojoExecutionException("Configuration Error", ex);
    }

    modules.addAll(additionalModules);

    try {
      getLog().info("Resolving modules: " + modules);
      MavenResolver resolver = new MavenResolver();
      resolver.resolve((Module[]) modules.toArray(new Module[modules.size()]));
    } catch (MalformedURLException ex) {
      String msg = "Failed to create URL for local repository";
      log(msg, ex);
      throw new MojoExecutionException(msg, ex);
    } catch (BundleException ex) {
      String msg = "Can't resolve module artifacts";
      log(msg, ex);
      throw new MojoExecutionException(msg, ex);
    }
  }

  protected URL resolveArtifact(String groupId, String artifactId, String version) {
    // TODO why do we need to do this?
    if (version.startsWith("\"")) {
      version = version.substring(1, version.length() - 1);
    }
    // hack to align OSGi version to Maven versions
    if (version.endsWith(".SNAPSHOT")) {
      version = version.substring(0, version.indexOf(".SNAPSHOT")) + "-SNAPSHOT";
    }

    VersionRange versionRange = null;
    try {
      if ("(any-version)".equals(version) || version == null) {
        versionRange = VersionRange.createFromVersion("1.0.0");
      } else {
        versionRange = VersionRange.createFromVersionSpec(version);
      }
    } catch (InvalidVersionSpecificationException ex) {
      throw new RuntimeException("Invalid version spec " + version + " for " + groupId + ":" + artifactId, ex);
    }

    Artifact artifact = artifactFactory.createDependencyArtifact( // 
        groupId, artifactId.replace('_', '-'), versionRange, "jar", null, Artifact.SCOPE_RUNTIME);
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

  void log(String msg, Exception ex) {
    if (getLog().isDebugEnabled()) {
      getLog().error(msg, ex);
    } else {
      getLog().error(msg);
    }
  }

  /**
   * Collect additional modules from all processes
   * 
   * @return <code>List</code> of <code>Module</code> instances
   */
  protected List getAdditionalModules() {
    HashSet reqs = new HashSet();
    try {
      reqs.addAll(Arrays.asList(BundleSpecImpl.getRequirements(this.modules)));
      if (processes != null) {
        for (int i = 0; i < processes.length; i++) {
          ProcessConfiguration process = processes[i];
          reqs.addAll(Arrays.asList(BundleSpecImpl.getRequirements(process.getModules())));
        }
      }
    } catch (BundleException ex) {
      throw new RuntimeException("Unable to resolve additional modules", ex);
    }

    ArrayList moduleList = new ArrayList();
    for (Iterator it = reqs.iterator(); it.hasNext();) {
      String req = (String) it.next();
      BundleSpec spec = new BundleSpecImpl(req);
      Module module = Module.Factory.newInstance();
      module.setGroupId(spec.getGroupId());
      module.setName(spec.getName());
      module.setVersion(spec.getVersion());
      moduleList.add(module);
    }
    return moduleList;
  }

  // setters for the lifecycle simulation 

  public void setJvmargs(String jvmargs) {
    this.jvmargs = jvmargs;
  }

  public void setJvm(String jvm) {
    this.jvm = jvm;
  }

  /**
   * Special Resolver implementation that is using Maven mechanisms to download modules jars to the local Maven
   * repository using remote Maven repositories listed in the project pom
   */
  private class MavenResolver extends Resolver {

    public MavenResolver() throws BundleException, MalformedURLException {
      super(new URL[] { new URL(getModulesRepository()) });
    }

    protected URL resolveLocation(final String name, final String version, final String groupId) {
      getLog().info("Resolving location: " + groupId + ":" + name + ":" + version);
      return resolveArtifact(groupId, name, version);
    }

    protected URL resolveBundle(BundleSpec spec) {
      String version = spec.getVersion();
      String groupId = spec.getGroupId();
      String name = spec.getName();
      getLog().info("Resolving bundle: " + groupId + ":" + name + ":" + version);

      return resolveArtifact(groupId, name, version);
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
