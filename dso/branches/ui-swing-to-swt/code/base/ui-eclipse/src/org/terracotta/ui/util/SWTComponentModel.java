/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.util;

import com.tc.util.event.UpdateEventListener;


public interface SWTComponentModel {

  /**
   * Initializes data for this model - creates a state object which stores data and references as member fields - no
   * accessor methods).
   */
  void init(Object data);

  /**
   * Type arguments should be defined as <tt>public static final</tt> member fields of the implementing class
   * <p>
   * NOTE: needs synchronization
   */
  void addListener(UpdateEventListener listener, int type);

  /**
   * NOTE: needs synchronization
   */
  void removeListener(UpdateEventListener listener, int type);

  /**
   * Clears state information. This will set both <tt>isInit()</tt> and <tt>isActive()</tt> to <tt>false</tt>
   */
  void clearState();

  /**
   * Deactivates registered action listeners
   */
  void setActive(boolean activate);

  boolean isActive();
}
