/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.cargo.maven2.configuration.Configuration;
import org.codehaus.cargo.maven2.configuration.Container;

/**
 * @author Eugene Kuleshov
 */
public class ProcessConfiguration {

  private String nodeName;
  private String className;
  private String args;
  private String jvmArgs;
  private String modules;
  private int count;
  private Map properties = new HashMap();
  private Container container;
  private Configuration configuration;

  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }
  
  public String getArgs() {
    return args;
  }

  public void setArgs(String args) {
    this.args = args;
  }

  public String getJvmArgs() {
    return jvmArgs;
  }
  
  public void setJvmArgs(String jvmArgs) {
    this.jvmArgs = jvmArgs;
  }

  public int getCount() {
    return count;
  }
  
  public void setCount(int count) {
    this.count = count;
  }

  public Map getProperties() {
    return properties;
  }
  
  public void setProperties(Map properties) {
    this.properties = properties;
  }
  
  public void addProperty(String key, String value) {
    properties.put(key, value);
  }

  public Container getContainer() {
    return container;
  }

  public void setContainer(Container container) {
    this.container = container;
  }

  public Configuration getConfiguration() {
    return configuration;
  }
  
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public String getModules() {
    return modules;
  }

  public void setModules(String modules) {
    this.modules = modules;
  }
  
}
