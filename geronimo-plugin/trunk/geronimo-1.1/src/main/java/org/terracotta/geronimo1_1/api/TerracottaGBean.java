/**
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.geronimo1_1.api;

import java.util.ResourceBundle;

/**
 * This is the interface users will have to configure their Terracotta environment via the Geronimo console.
 */
public interface TerracottaGBean {

  /**
   * Localized messages for implementations of {@link TerracottaGBean} to use for logging interesting events.
   */
  static class Messages {

    private static ResourceBundle rb           = ResourceBundle.getBundle(TerracottaGBean.class.getName());

    private static String         INSTALLING   = "installing";
    private static String         INSTALLED    = "installed";
    private static String         INSTANTIATE  = "init";
    private static String         STARTING     = "starting";
    private static String         STARTED      = "started";
    private static String         ENABLING     = "enabling";
    private static String         ENABLED      = "enabled";
    private static String         DISABLING    = "disabling";
    private static String         DISABLED     = "disabled";
    private static String         STOPPING     = "stopping";
    private static String         STOPPED      = "stopped";
    private static String         UNINSTALLING = "uninstalling";
    private static String         UNINSTALLED  = "uninstalled";
    private static String         FAILURE      = "failure";

    public static String installing() {
      return rb.getString(INSTALLING);
    }

    public static String installed() {
      return rb.getString(INSTALLED);
    }

    public static String instanted() {
      return rb.getString(INSTANTIATE);
    }

    public static String starting() {
      return rb.getString(STARTING);
    }

    public static String started() {
      return rb.getString(STARTED);
    }

    public static String enabling() {
      return rb.getString(ENABLING);
    }

    public static String enabled() {
      return rb.getString(ENABLED);
    }

    public static String disabling() {
      return rb.getString(DISABLING);
    }

    public static String disabled() {
      return rb.getString(DISABLED);
    }

    public static String stopping() {
      return rb.getString(STOPPING);
    }

    public static String stopped() {
      return rb.getString(STOPPED);
    }

    public static String uninstalling() {
      return rb.getString(UNINSTALLING);
    }

    public static String uninstalled() {
      return rb.getString(UNINSTALLED);
    }

    public static String failure() {
      return rb.getString(FAILURE);
    }

  }

  /**
   * Installs the Terracotta software into the Geronimo environment.
   */
  void install();

  /**
   * Uninstalls the Terracotta software from the Geronimo environment.
   */
  void uninstall();

  /**
   * Enables Terracotta in the Geronimo environment, requires a Geronimo restart.
   */
  void enable();

  /**
   * Disables Terracotta in the Geronimo environment, requires a Geronimo restart.
   */
  void disable();

}