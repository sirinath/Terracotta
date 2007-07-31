/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

public interface ILogLevel {
  public static final String   DEBUG_NAME = "DEBUG";
  public static final String   INFO_NAME  = "INFO";
  public static final String   WARN_NAME  = "WARN";
  public static final String   ERROR_NAME = "ERROR";
  public static final String   FATAL_NAME = "FATAL";

  public abstract int getLevel();

  public abstract boolean isInfo();

  public abstract String toString();

}