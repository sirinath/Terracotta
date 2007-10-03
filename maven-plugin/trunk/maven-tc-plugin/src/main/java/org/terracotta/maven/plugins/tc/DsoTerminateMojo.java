/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.util.Iterator;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Terminate running DSO processes
 * 
 * @goal terminate
 * 
 * @author Eugene Kuleshov
 * @see DsoRunMojo
 */
public class DsoTerminateMojo extends AbstractDsoRunMojo {

  protected void onExecute() throws MojoExecutionException, MojoFailureException {
    getLog().info("------------------------------------------------------------------------");
    
    Set activeNodes = getActiveNodes();
    
    getLog().info("Terminating DSO nodes " + (activeNodes.size()==0 ? "" : this.activeNodes));
    
    for (Iterator it = getStartables().iterator(); it.hasNext();) {
      Startable startable = (Startable) it.next();
      if (activeNodes.size() == 0 || activeNodes.contains(startable.getNodeName())) {
        startable.stop();
      }
    }
  }

  protected boolean generateBootJar() {
    return false;
  }

  protected boolean startDsoServer() {
    return false;
  }

  protected boolean stopDsoServer() {
    return false;
  }
  
}
