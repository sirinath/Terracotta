/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 */
public abstract class DsoLifecycleMojo extends AbstractDsoMojo {

  // bootjar
  
  /**
   * @parameter expression="${verbose}" default-value="false"
   */
  protected boolean verbose;

  /**
   * @parameter expression="${overwriteBootjar}" default-value="false"
   */
  protected boolean overwriteBootjar;

  /**
   * @parameter expression="${bootjar}" default-value="${project.build.directory}/dso-boot.jar"
   */
  protected File bootJar;

  
  // start/stop

  /**
   * @parameter expression="${spawnServer}" default-value="true"
   */
  protected boolean spawnServer;

  /**
   * @parameter expression="${serverName}"
   * @optional
   */
  protected String serverName;

  /**
   * @parameter expression="${startServer}" default-value="true"
   */
  protected boolean startServer;

  
  public final void execute() throws MojoExecutionException, MojoFailureException {
    BootjarMojo bootjarMojo = new BootjarMojo(this);
    bootjarMojo.setBootJar(bootJar);
    bootjarMojo.setVerbose(verbose);
    bootjarMojo.setOverwriteBootjar(overwriteBootjar);
    bootjarMojo.execute();
    
    DsoStartMojo dsoStartMojo = new DsoStartMojo(this);
    dsoStartMojo.setSpawnServer(spawnServer);
    dsoStartMojo.setServerName(serverName);
    dsoStartMojo.setStartServer(startServer);
    dsoStartMojo.execute();
    
    try {
      onExecute();
      
    } finally {
      DsoStopMojo dsoStopMojo = new DsoStopMojo(this);
      dsoStopMojo.setSpawnServer(spawnServer);
      dsoStopMojo.setServerName(serverName);
      dsoStopMojo.setStartServer(startServer);
      dsoStopMojo.execute();
    }
  }

  protected abstract void onExecute()  throws MojoExecutionException, MojoFailureException;

}
