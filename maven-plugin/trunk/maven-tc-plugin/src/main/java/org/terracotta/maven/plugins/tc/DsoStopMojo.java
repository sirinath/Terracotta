/**
 * 
 */
package org.terracotta.maven.plugins.tc;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @author Eugene Kuleshov
 * 
 * @goal stop
 * @requiresDependencyResolution runtime
 * @execute phase="validate"
 */
public class DsoStopMojo extends AbstractDsoServerMojo {

  private boolean startServer = true;

  public DsoStopMojo() {
  }
  
  public DsoStopMojo(AbstractDsoMojo mojo) {
    super(mojo);
  }
  
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!startServer) {
      getLog().info("Skipping stopping DSO Server");
      return;
    }

    stop(); 
  }
    
  public void setStartServer(boolean startServer) {
    this.startServer = startServer;
  }
}
