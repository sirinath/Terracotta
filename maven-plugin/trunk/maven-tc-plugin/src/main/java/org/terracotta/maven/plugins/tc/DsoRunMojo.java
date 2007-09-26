/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
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

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

/**
 * @goal run
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @execute phase="compile"
 * @configurator override
 */
public class DsoRunMojo extends DsoLifecycleMojo {

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
   * @parameter expression="${project}"
   * @required
   * @readonly
   * @see #getProject()
   */
  private MavenProject project;

  /**
   * @see org.codehaus.cargo.maven2.util.CargoProject
   */
  private CargoProject cargoProject;
  
  private int port = 8080;
  private int rmiPort = 9080;

  protected void onExecute() throws MojoExecutionException, MojoFailureException {
    getLog().info("------------------------------------------------------------------------");
    resolveModuleArtifacts(false);

    getLog().info("Starting DSO processes");

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
    if (processes != null) {
      processes.addAll(Arrays.asList(this.processes));
    }

    ArrayList startables = new ArrayList();
    
    int totalNumberOfNodes = 0;
    for (Iterator it = processes.iterator(); it.hasNext();) {
      ProcessConfiguration process = (ProcessConfiguration) it.next();
      totalNumberOfNodes += process.getCount();
    }

    for (Iterator it = processes.iterator(); it.hasNext();) {
      ProcessConfiguration process = (ProcessConfiguration) it.next();

      for (int n = 0; n < process.getCount(); n++) {
        String nodeName = getNodeName(process, n);

        Container container = process.getContainer();
        getLog().info("containerId: " + container.getContainerId());
        if (container != null) {
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
          
          startables.add(new CargoStartable(nodeName, cargoContainer));

        } else {
          try {
            startables.add(new CmdStartable(nodeName, createCommandLine(process, nodeName, totalNumberOfNodes)));
          } catch (IOException ex) {
            throw new MojoExecutionException("Unable to create process " + ex.toString(), ex);
          }
        
        }
      }
    }

    CyclicBarrier barrier = new CyclicBarrier(startables.size() + 1);
    for (Iterator it = startables.iterator(); it.hasNext();) {
      fork((Startable) it.next(), barrier);
    }
    
    getLog().info("------------------------------------------------------------------------");
    getLog().info("Waiting completion of the DSO process");
    try {
      barrier.barrier();
    } catch (BrokenBarrierException ex) {
      getLog().error(ex);
    } catch (InterruptedException ex) {
      getLog().error(ex);
    }

    getLog().info("DSO processes finished");
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
  
  private String getNodeName(ProcessConfiguration process, int n) {
    return process.getCount() > 1 ? process.getNodeName() + n : process.getNodeName();
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
    localConfiguration.setProperty(GeneralPropertySet.JVMARGS, createJvmArguments(process, nodeName, totalNumberOfNodes));
    localConfiguration.setProperty(GeneralPropertySet.LOGGING, "high");

    return localConfiguration;
  }

  private Commandline createCommandLine(ProcessConfiguration process, String nodeName, int totalNumberOfNodes) throws IOException {
    Commandline cmd = super.createCommandLine();

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

    return cmd;
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

  private void fork(final Startable startable, final CyclicBarrier barrier) {
    getLog().info("Starting node " + startable.getNodeName() + ": " + startable.toString());
    new Thread() {
      public void run() {
        try {
          startable.start();
        } finally {
          getLog().info("Finished node " + startable.getNodeName());
          try {
            barrier.barrier();
          } catch (BrokenBarrierException ex) {
            getLog().error(ex);
          } catch (InterruptedException ex) {
            getLog().error(ex);
          }
        }
      }
    }.start();

    // Thread.yield();
  }

  
  public static interface Startable {
    public void start();
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

    public String getNodeName() {
      return nodeName;
    }
    
    public String toString() {
      return cmd.toString();
    }
    
  }

  public class CargoStartable implements Startable {

    private final String nodeName;
    private final InstalledLocalContainer cargoContainer;

    public CargoStartable(String nodeName, InstalledLocalContainer cargoContainer) {
      this.nodeName = nodeName;
      this.cargoContainer = cargoContainer;
    }

    public void start() {
      cargoContainer.start();
      ContainerUtils.waitTillContainerIsStopped(cargoContainer);
    }
    
    public String getNodeName() {
      return nodeName;
    }
    
    public String toString() {
      return cargoContainer.toString();
    }
    
  }
  
}
