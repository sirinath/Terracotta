/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.osgi.framework.BundleException;

import com.tc.bundles.BundleSpec;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

/**
 * Run DSO processes
 * 
 * @goal run
 * @execute phase="package"
 * 
 * @author Eugene Kuleshov
 */
public class DsoRunMojo extends AbstractDsoRunMojo {
  
  protected void onExecute() throws MojoExecutionException, MojoFailureException {
    getLog().info("------------------------------------------------------------------------");
    resolveModuleArtifacts(getAdditionalModules());

    Set activeNodes = getActiveNodes();

    getLog().info("Starting DSO nodes " + (activeNodes.size()==0 ? "" : this.activeNodes));

    List startables = getStartables();

    int numberofNodes = activeNodes.size() > 0 ? activeNodes.size() : startables.size();
    CyclicBarrier barrier = new CyclicBarrier(numberofNodes + 1);

    for (Iterator it = startables.iterator(); it.hasNext();) {
      Startable startable = (Startable) it.next();
      if (activeNodes.size() == 0 || activeNodes.contains(startable.getNodeName())) {
        fork(startable, barrier);
      }
    }

    if(waitForCompletion()) {
      getLog().info("------------------------------------------------------------------------");
      getLog().info("Waiting completion of the DSO process");
    }
    
    try {
      barrier.barrier();
    } catch (BrokenBarrierException ex) {
      getLog().error(ex);
    } catch (InterruptedException ex) {
      getLog().error(ex);
    }

    if(waitForCompletion()) {
      getLog().info("DSO processes finished");
    }
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

}
