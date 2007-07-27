/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import org.apache.maven.plugin.AbstractMojo;

/**
 * @goal test
 * @phase dso-test
 * @execute phase="verify" lifecycle="dso-test"
 */
public class DsoTestLifecycleMojo extends AbstractMojo {

  public void execute() {
    // forked lifecycle is declared in /META-INF/maven/lifecycle.xml
  }

}
