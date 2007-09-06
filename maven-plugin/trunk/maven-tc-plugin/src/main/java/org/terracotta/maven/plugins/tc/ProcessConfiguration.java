/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.util.Map;

/**
 * @author Eugene Kuleshov
 */
public class ProcessConfiguration {

  private final String nodeName;
  private final String className;
  private final String args;
  private final String jvmArgs;
  private final Map properties;
  private final int count;

  public ProcessConfiguration(String nodeName, String className, String args, String jvmArgs, Map properties, int count) {
    this.nodeName = nodeName;
    this.className = className;
    this.args = args;
    this.jvmArgs = jvmArgs;
    this.properties = properties;
    this.count = count;
  }

  public String getNodeName() {
    return nodeName;
  }

  public String getClassName() {
    return className;
  }

  public String getArgs() {
    return args;
  }

  public String getJvmArgs() {
    return jvmArgs;
  }
  
  public Map getProperties() {
    return properties;
  }

  public int getCount() {
    return count;
  }

}
