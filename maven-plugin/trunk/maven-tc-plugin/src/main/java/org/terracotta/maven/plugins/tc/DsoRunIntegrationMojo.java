/*
 * 
 */
package org.terracotta.maven.plugins.tc;


/**
 * Run DSO processes for the "integration-test" phase
 * 
 * @goal run-integration
 * @execute phase="pre-integration-test"
 * 
 * @author Eugene Kuleshov
 */
public class DsoRunIntegrationMojo extends DsoRunMojo {
  
  protected boolean waitForCompletion() {
    return false;
  }

  protected boolean stopDsoServer() {
    return false;
  }
  
}
