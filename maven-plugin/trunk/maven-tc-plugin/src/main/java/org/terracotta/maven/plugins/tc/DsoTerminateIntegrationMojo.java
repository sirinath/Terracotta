/*
 * 
 */
package org.terracotta.maven.plugins.tc;


/**
 * Terminate running DSO processes for the "integration-test" phase
 * 
 * @goal terminate-integration
 * @execute phase="post-integration-test"
 * 
 * @author Eugene Kuleshov
 */
public class DsoTerminateIntegrationMojo extends DsoTerminateMojo {

  protected boolean stopDsoServer() {
    return true;
  }
  
}
