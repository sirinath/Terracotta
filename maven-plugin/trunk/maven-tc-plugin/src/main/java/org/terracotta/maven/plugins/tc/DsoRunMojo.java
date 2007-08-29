/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
   * @parameter expression="${numberOfNodes}" default-value="1"
   */
  private int numberOfNodes;
  
    
  protected void onExecute() throws MojoExecutionException, MojoFailureException {
    getLog().info("------------------------------------------------------------------------");
    resolveModuleArtifacts(false);

    getLog().info("Starting DSO processes");

    List processes = new ArrayList();
    if (className != null) {
      processes.add(new ProcessConfiguration("node", className, arguments, Collections.EMPTY_MAP, numberOfNodes));
    }
    if(processes!=null) {
      processes.addAll(Arrays.asList(this.processes));
    }

    ArrayList names = new ArrayList();
    ArrayList cmds = new ArrayList();

    int totalNumberOfNodes = 0;
    for (Iterator it = processes.iterator(); it.hasNext();) {
      ProcessConfiguration process = (ProcessConfiguration) it.next();
      totalNumberOfNodes += process.getCount();
    }
      
    for (Iterator it = processes.iterator(); it.hasNext();) {
      ProcessConfiguration process = (ProcessConfiguration) it.next();
      for (int n = 0; n < process.getCount(); n++) {
        String nodeName = process.getCount() > 1 ? process.getNodeName() + n : process.getNodeName();
        names.add(nodeName);
        cmds.add(createCommandLine(process, nodeName, totalNumberOfNodes));
      }
    }

    CyclicBarrier barrier = new CyclicBarrier(names.size() + 1);

    for (int i = 0; i < names.size(); i++) {
      fork((String) names.get(i), (Commandline) cmds.get(i), barrier);
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

  private Commandline createCommandLine(ProcessConfiguration process, String nodeName, int totalNumberOfNodes) {
    Commandline cmd = super.createCommandLine();

    if (workingDirectory != null) {
      cmd.setWorkingDirectory(workingDirectory); 
    }
  
    cmd.createArgument().setValue("-Xbootclasspath/p:" + bootJar.getAbsolutePath());

    cmd.createArgument().setValue("-Dtc.nodeName=" + nodeName);
    cmd.createArgument().setValue("-Dtc.numberOfNodes=" + totalNumberOfNodes);

    // system properties      
    for (Iterator it = process.getProperties().entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      cmd.createArgument().setValue("-D" + e.getKey() + "=" + e.getValue());
    }

    cmd.createArgument().setValue("-Dtc.classpath=" + createPluginClasspath());

    cmd.createArgument().setValue("-cp");
    cmd.createArgument().setValue(createProjectClasspath());

    // arguments
    cmd.createArgument().setValue(process.getClassName());
    if (process.getArgs() != null) {
      cmd.createArgument().setValue(process.getArgs());
    }

    return cmd;
  }

  private void fork(final String nodeName, final Commandline cmd, final CyclicBarrier barrier) {
    new Thread() {
      public void run() {
        getLog().debug("Starting node " + nodeName + ": " + cmd.toString());
        try {
          StreamConsumer streamConsumer = new ForkedProcessStreamConsumer(nodeName);
          CommandLineUtils.executeCommandLine(cmd, streamConsumer, streamConsumer);
        } catch (CommandLineException e) {
          getLog().error("Failed to start node " + nodeName, e);
        } finally {
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
  
}
