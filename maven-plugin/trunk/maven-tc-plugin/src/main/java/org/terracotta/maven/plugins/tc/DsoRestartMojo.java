package org.terracotta.maven.plugins.tc;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal restart
 * @requiresDependencyResolution runtime
 * @execute phase="validate"
 * @requiresDependencyResolution runtime
 */
public class DsoRestartMojo extends AbstractDsoServerMojo {

  public DsoRestartMojo() {
    super();
  }
  
  public DsoRestartMojo(AbstractDsoMojo mojo) {
    super(mojo);
  }

  public void execute() throws MojoExecutionException, MojoFailureException {
      setSpawnServer(true);
      stop(true); 
      start();
  } 
}