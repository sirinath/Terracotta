/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployer.URLDeployableMonitor;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.spi.deployer.DeployerWatchdog;
import org.codehaus.cargo.container.spi.util.ContainerUtils;
import org.codehaus.cargo.maven2.configuration.Configuration;
import org.codehaus.cargo.maven2.configuration.Container;
import org.codehaus.cargo.maven2.configuration.Deployable;
import org.codehaus.cargo.maven2.log.MavenLogger;
import org.codehaus.cargo.maven2.util.CargoProject;
import org.codehaus.cargo.util.log.FileLogger;
import org.codehaus.cargo.util.log.Logger;
import org.codehaus.plexus.util.cli.StreamConsumer;

import org.terracotta.maven.plugins.tc.cl.CommandLineException;
import org.terracotta.maven.plugins.tc.cl.CommandLineUtils;
import org.terracotta.maven.plugins.tc.cl.Commandline;

/**
 * @requiresDependencyResolution runtime
 * @configurator override
 */
public abstract class AbstractDsoRunMojo extends DsoLifecycleMojo {

  public static final String CONTEXT_KEY_STARTABLES = AbstractDsoRunMojo.class.getName() + "-Startables";

  /**
   * Configuration for the DSO-enabled processes
   * 
   * @parameter expression="${processes}"
   * @optional
   */
  private ProcessConfiguration[] processes;

  /**
   * @parameter expression="${className}"
   * @optional
   */
  private String className;

  /**
   * @parameter expression="${arguments}"
   * @optional
   */
  private String arguments;

  /**
   * @parameter expression="${jvmargs}"
   * @optional
   */
  private String jvmargs;

  /**
   * @parameter expression="${numberOfNodes}" default-value="1"
   */
  private int numberOfNodes;

  /**
   * @parameter expression="${activeNodes}"
   * @optional
   */
  protected String activeNodes;

  /**
   * @parameter expression="${modules}"
   * @optional
   */
  protected String modules;

  /**
   * @parameter expression="${project}"
   * @required
   * @readonly
   * @see #getProject()
   */
  protected MavenProject project;

  /**
   * @see org.codehaus.cargo.maven2.util.CargoProject
   */
  private CargoProject cargoProject;

  private int port = 8080;
  private int rmiPort = 9080;

  protected Set getActiveNodes() {
    if (activeNodes == null) {
      return Collections.EMPTY_SET;
    }
    return new HashSet(Arrays.asList(activeNodes.split(",")));
  }

  protected boolean waitForCompletion() {
    return true;
  }
  
  protected List getStartables() throws MojoExecutionException {
    List startables = null;

    Map context = getPluginContext();
    if (context != null) {
      startables = (List) context.get(CONTEXT_KEY_STARTABLES);
    }

    if (startables == null) {
      startables = createStartables();
      if (context != null) {
        context.put(CONTEXT_KEY_STARTABLES, startables);
      }
    }

    return startables;
  }

  private List createStartables() throws MojoExecutionException {
    List startables = new ArrayList();

    List processes = new ArrayList();
    if (className != null) {
      ProcessConfiguration process = new ProcessConfiguration();
      process.setNodeName("node");
      process.setClassName(className);
      process.setArgs(arguments);
      process.setJvmArgs(jvmargs);
      process.setCount(numberOfNodes);
      processes.add(process);
    }

    processes.addAll(Arrays.asList(this.processes));

    int totalNumberOfNodes = 0;
    for (Iterator it = processes.iterator(); it.hasNext();) {
      ProcessConfiguration process = (ProcessConfiguration) it.next();
      totalNumberOfNodes += process.getCount();
    }

    for (Iterator it = processes.iterator(); it.hasNext();) {
      ProcessConfiguration process = (ProcessConfiguration) it.next();

      for (int n = 0; n < process.getCount(); n++) {
        String nodeName = process.getCount() > 1 ? process.getNodeName() + n : process.getNodeName();

        Container container = process.getContainer();
        Startable startable;
        if (container == null) {
          startable = createCmdStartable(process, nodeName, totalNumberOfNodes);
        } else {
          startable = createCargoStartable(process, nodeName, totalNumberOfNodes, container);
        }
        startables.add(startable);
      }
    }
    return startables;
  }

  private Startable createCmdStartable(ProcessConfiguration process, String nodeName, int totalNumberOfNodes) {
    Commandline cmd = super.createCommandLine();

    if (process.getModules() != null) {
      cmd.createArgument().setValue("-Dtc.tests.configuration.modules=" + process.getModules());
    } else if (modules != null) {
      cmd.createArgument().setValue("-Dtc.tests.configuration.modules=" + modules);
    }

    if (workingDirectory != null) {
      cmd.setWorkingDirectory(workingDirectory);
    }

    cmd.createArgument().setLine(createJvmArguments(process, nodeName, totalNumberOfNodes));

    cmd.createArgument().setValue("-cp");
    cmd.createArgument().setValue(quoteIfNeeded(createProjectClasspath()));

    cmd.createArgument().setValue(process.getClassName());
    if (process.getArgs() != null) {
      cmd.createArgument().setValue(process.getArgs());
    }

    return new CmdStartable(nodeName, cmd);
  }

  private Startable createCargoStartable(ProcessConfiguration process, String nodeName, int totalNumberOfNodes,
      Container container) throws MojoExecutionException {
    LocalConfiguration cargoConfiguration = createCargoConfiguration(container, process, nodeName, totalNumberOfNodes);

    InstalledLocalContainer cargoContainer = (InstalledLocalContainer) container.createContainer( //
        cargoConfiguration, //
        createCargoLogger(container), getCargoProject());

    // cargoContainer.setSystemProperties(process.getProperties());

    // deploy auto-deployable
    if (getCargoProject().getPackaging() != null && getCargoProject().isJ2EEPackaging()) {
      // Has the auto-deployable already been specified as part of the <deployables> config element? 
      getLog().info("cargoConfiguration " + cargoConfiguration);
      if (process.getConfiguration() == null //
          || process.getConfiguration().getDeployables() == null
          || !containsAutoDeployable(process.getConfiguration().getDeployables())) {
        LocalConfiguration configuration = cargoContainer.getConfiguration();
        configuration.addDeployable(new Deployable().createDeployable(cargoContainer.getId(), getCargoProject()));
      }
    }

    getLog().debug(nodeName + " home:" + cargoContainer.getHome());
    getLog().debug(nodeName + " conf:" + cargoContainer.getConfiguration());
    getLog().debug(nodeName + " props:" + cargoConfiguration.getProperties());

    return new CargoStartable(nodeName, cargoContainer, waitForCompletion());
  }

  private boolean containsAutoDeployable(Deployable[] deployableElements) {
    for (int i = 0; i < deployableElements.length; i++) {
      Deployable deployableElement = deployableElements[i];
      if (deployableElement.getGroupId().equals(getCargoProject().getGroupId())
          && deployableElement.getArtifactId().equals(getCargoProject().getArtifactId())) {
        return true;
      }
    }
    return false;
  }

  private CargoProject getCargoProject() {
    if (cargoProject == null) {
      cargoProject = new CargoProject(project, getLog());
    }
    return cargoProject;
  }

  private Logger createCargoLogger(Container container) {
    Logger logger;
    if (container.getLog() != null) {
      container.getLog().getParentFile().mkdirs();
      logger = new FileLogger(container.getLog(), false);
    } else {
      logger = new MavenLogger(getLog());
    }
    if (container.getLogLevel() != null) {
      logger.setLevel(container.getLogLevel());
    }
    return logger;
  }

  private LocalConfiguration createCargoConfiguration(Container container, ProcessConfiguration process,
      String nodeName, int totalNumberOfNodes) throws MojoExecutionException {
    Configuration configuration = process.getConfiguration();
    if (configuration == null) {
      configuration = new Configuration();
    }

    // If no configuration element has been specified create one with default values.
    //    if (getConfigurationElement() == null) {
    //      Configuration configurationElement = new Configuration();
    //
    //      if (getContainerElement().getType().isLocal()) {
    //        configurationElement.setType(ConfigurationType.STANDALONE);
    //        configurationElement.setHome(new File(getCargoProject().getBuildDirectory(), getContainerElement()
    //            .getContainerId()).getPath());
    //      } else {
    //        configurationElement.setType(ConfigurationType.RUNTIME);
    //      }
    //
    //      setConfigurationElement(configurationElement);
    //    }

    // XXX
    //    configuration.setHome(home);
    //    configuration.setProperties(properties);
    //    configuration.setDeployables(deployables);

    configuration.setHome("target/" + nodeName);

    LocalConfiguration localConfiguration = (LocalConfiguration) configuration.createConfiguration( //
        container.getContainerId(), container.getType(), getCargoProject());

    localConfiguration.setProperty(ServletPropertySet.PORT, "" + port++);
    localConfiguration.setProperty(GeneralPropertySet.RMI_PORT, "" + rmiPort++);
    localConfiguration.setProperty(GeneralPropertySet.JVMARGS,
        createJvmArguments(process, nodeName, totalNumberOfNodes));
    localConfiguration.setProperty(GeneralPropertySet.LOGGING, "high");

    return localConfiguration;
  }

  protected String createJvmArguments(ProcessConfiguration process, String nodeName, int totalNumberOfNodes) {
    StringBuffer sb = new StringBuffer();

    sb.append("-Dtc.nodeName=" + nodeName);
    sb.append(" -Dtc.numberOfNodes=" + totalNumberOfNodes);

    if (config != null) {
      sb.append(" -Dtc.config=" + config.getAbsolutePath());
    }
    sb.append(" -Dtc.classpath=" + createPluginClasspathAsFile());
    sb.append(" -Dtc.session.classpath=" + createSessionClasspath());

    sb.append(' ').append(super.createJvmArguments());

    sb.append(" -Xbootclasspath/p:" + bootJar.getAbsolutePath());

    // system properties      
    for (Iterator it = process.getProperties().entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      sb.append(" -D" + e.getKey() + "=" + e.getValue());
    }

    if (process.getJvmArgs() != null) {
      sb.append(' ').append(process.getJvmArgs());
    }

    return sb.toString();
  }

  public static interface Startable {
    public void start();

    public void stop();

    public String getNodeName();
  }

  public class CmdStartable implements Startable {

    private final String nodeName;
    private final Commandline cmd;

    public CmdStartable(String nodeName, Commandline cmd) {
      this.nodeName = nodeName;
      this.cmd = cmd;
    }

    public void start() {
      try {
        StreamConsumer streamConsumer = new ForkedProcessStreamConsumer(nodeName);
        CommandLineUtils.executeCommandLine(cmd, streamConsumer, streamConsumer);
      } catch (CommandLineException e) {
        getLog().error("Failed to start node " + nodeName, e);
      }
    }

    public void stop() {
      // ignore
    }

    public String getNodeName() {
      return nodeName;
    }

    public String toString() {
      return cmd.toString();
    }

  }

  public class CargoStartable implements Startable {

    private final String nodeName;
    private final InstalledLocalContainer container;
    private boolean wait;

    public CargoStartable(String nodeName, InstalledLocalContainer container, boolean wait) {
      this.nodeName = nodeName;
      this.container = container;
      this.wait = wait;
    }

    public void start() {
      container.start();
      
      URL cpcUrl = ContainerUtils.getCPCURL(container.getConfiguration());
      
      // ContainerUtils.waitTillContainerIsStopped(container);

      DeployerWatchdog watchdog1 = new DeployerWatchdog(new URLDeployableMonitor(cpcUrl, container.getTimeout()));
      watchdog1.watchForAvailability();
      getLog().info("[" + nodeName + "] started");

      try {
        Thread.sleep(2000L);
      } catch (InterruptedException ex) {
        // ignore
      }
      
      if (wait) {
        DeployerWatchdog watchdog2 = new DeployerWatchdog(new URLDeployableMonitor(cpcUrl, Long.MAX_VALUE));
        watchdog2.watchForUnavailability();
        getLog().info("[" + nodeName + "] stopped");

        try {
          Thread.sleep(2000L);
        } catch (InterruptedException ex) {
          // ignore
        }
      }
    }

    public void stop() {
      container.stop();
    }

    public String getNodeName() {
      return nodeName;
    }

    public String toString() {
      return container.toString();
    }

  }

}
