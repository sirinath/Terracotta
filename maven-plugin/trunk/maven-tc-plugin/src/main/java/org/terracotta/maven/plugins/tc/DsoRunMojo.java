/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
 */
public class DsoRunMojo extends DsoLifecycleMojo {

  //  /**
  //   * Configuration for the DSO-enabled processes
  //   * 
  //   * @parameter expression="${processes}"
  //   * @required
  //   */
  //  private ProcessConfiguration[] processes;

  /**
   * @parameter expression="${className}"
   * @required
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
    getLog().info("Starting DSO processes");

    ProcessConfiguration process = new ProcessConfiguration("node", className, arguments, Collections.EMPTY_MAP,
        numberOfNodes);

    ArrayList names = new ArrayList();
    ArrayList cmds = new ArrayList();

    for (int n = 0; n < process.getCount(); n++) {
      String nodeName = process.getCount() > 0 ? process.getNodeName() + n : process.getNodeName();
      names.add(nodeName);
      cmds.add(createCommandLine(process, nodeName));
    }

    //    for (int i = 0; i < processes.length; i++) {
    //      ProcessConfiguration process = processes[i];
    //      getLog().info(process.getClassName());
    //      for (int j = 0; j < process.getCount(); j++) {
    //        createCommandLine(process, j, names, cmds);
    //      }
    //    }

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

  private Commandline createCommandLine(ProcessConfiguration process, String nodeName) {
    Commandline cmd = super.createCommandLine();

    cmd.createArgument().setValue("-Xbootclasspath/p:" + bootJar.getAbsolutePath());

    cmd.createArgument().setValue("-Dtc.nodeName=" + nodeName);
    cmd.createArgument().setValue("-Dtc.numberOfNodes=" + numberOfNodes);

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
        getLog().debug("Starting process: " + cmd.toString());
        try {
          StreamConsumer streamConsumer = new ForkedProcessStreamConsumer(nodeName);
          CommandLineUtils.executeCommandLine(cmd, streamConsumer, streamConsumer);
        } catch (CommandLineException e) {
          getLog().error("Failed to start process", e);
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
