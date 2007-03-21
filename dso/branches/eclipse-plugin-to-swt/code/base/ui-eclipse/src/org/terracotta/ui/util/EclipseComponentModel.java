/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.util;

import org.eclipse.core.resources.IProject;

import java.util.EventListener;

/**
 * NOTE: All Implementing Methods Must be Synchronized
 */
public interface EclipseComponentModel {

  /**
   * Initializes data for this model - creates a memento which stores data and references as member fields (with no
   * accessor methods).
   */
  void init(IProject project);

  /**
   * Type arguments should be defined as <tt>public static final</tt> member fields of the implementing class
   */
  void addListener(EventListener listener, int type);

  void removeListener(EventListener listener);

  /**
   * Clears state information. This will set both <tt>isInit()</tt> and <tt>isActive()</tt> to <tt>false</tt>
   */
  void clearState();

  /**
   * This will unregister all action listeners. References to listener classes will be saved.
   */
  void disable();

  /**
   * Re-registers action listeners
   */
  void activate();

  boolean isActive();
}
