/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import org.apache.maven.plugin.AbstractMojo;

/**
 * @goal run
 * @phase dso-run
 * @execute phase="verify" lifecycle="dso-run"
 */
public class DsoRunLifecycleMojo extends AbstractMojo {

  public void execute() {
    // forked lifecycle is declared in /META-INF/maven/lifecycle.xml
  }

}
