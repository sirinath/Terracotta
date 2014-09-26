/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.appevent;

import javax.swing.tree.DefaultTreeModel;

/**
 * Abstract event context, common stuff for subclasses
 */
public abstract class AbstractApplicationEventContext implements ApplicationEventContext {

  private static final long      serialVersionUID = 4788562594133534828L;

  private final transient Object pojo;
  private final String           threadName;
  private final String           clientId;
  private final String           targetClassName;
  private DefaultTreeModel       treeModel;

  private String                 projectName;                            // optional: client Eclipse

  // project name

  /**
   * Construct new context
   * @param pojo Object of interest
   * @param threadName Thread name
   * @param clientId JVM client identifier
   */
  public AbstractApplicationEventContext(Object pojo, String threadName, String clientId) {
    this.pojo = pojo;
    this.targetClassName = pojo.getClass().getName();
    this.threadName = threadName;
    this.clientId = clientId;
    this.projectName = System.getProperty("project.name");
  }

  @Override
  public Object getPojo() {
    return pojo;
  }

  /**
   * @return Class name of object
   */
  public String getTargetClassName() {
    return targetClassName;
  }

  /**
   * @return Thread name
   */
  public String getThreadName() {
    return threadName;
  }

  /**
   * @return JVM client ID
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * @param treeModel Set model representing object hierarchy
   */
  @Override
  public void setTreeModel(DefaultTreeModel treeModel) {
    this.treeModel = treeModel;
  }

  /**
   * @return Model representing object hierarchy
   */
  @Override
  public DefaultTreeModel getTreeModel() {
    return treeModel;
  }

  /**
   * Set Eclipse project name
   * @param name
   */
  public void setProjectName(String name) {
    projectName = name;
  }

  /**
   * @return Eclipse project name
   */
  @Override
  public String getProjectName() {
    return projectName;
  }
}
