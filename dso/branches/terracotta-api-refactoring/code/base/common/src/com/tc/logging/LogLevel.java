/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;

import com.tc.util.Assert;

/**
 * Defines constants for various logging levels
 * 
 * @author teck
 */
public class LogLevel implements ILogLevel {

  static final int  LEVEL_DEBUG = 4;
  static final int  LEVEL_INFO  = 3;
  static final int  LEVEL_WARN  = 2;
  static final int  LEVEL_ERROR = 1;
  static final int  LEVEL_FATAL = 0;
  
  public static final ILogLevel DEBUG      = new LogLevel(LEVEL_DEBUG);
  public static final ILogLevel INFO       = new LogLevel(LEVEL_INFO);
  public static final ILogLevel WARN       = new LogLevel(LEVEL_WARN);
  public static final ILogLevel ERROR      = new LogLevel(LEVEL_ERROR);
  public static final ILogLevel FATAL      = new LogLevel(LEVEL_FATAL);

  private final int level;

  private LogLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }

  public boolean isInfo() {
    return level == LEVEL_INFO;
  }

  static Level toLog4JLevel(ILogLevel level) {
    if (level == null) return null;

    switch (level.getLevel()) {
      case LEVEL_DEBUG:
        return Level.DEBUG;
      case LEVEL_INFO:
        return Level.INFO;
      case LEVEL_WARN:
        return Level.WARN;
      case LEVEL_ERROR:
        return Level.ERROR;
      case LEVEL_FATAL:
        return Level.FATAL;
      default:
        throw Assert.failure("Logic Error: Invalid Level: " + level);
    }
  }

  static ILogLevel fromLog4JLevel(Level level) {
    if (level == null) return null;
    switch (level.toInt()) {
      case Priority.DEBUG_INT:
        return DEBUG;
      case Priority.INFO_INT:
        return INFO;
      case Priority.WARN_INT:
        return WARN;
      case Priority.ERROR_INT:
        return ERROR;
      case Priority.FATAL_INT:
        return FATAL;
      default:
        throw Assert.failure("Unsupported Level" + level);
    }
  }

  public String toString() {
    switch (getLevel()) {
      case LEVEL_DEBUG:
        return DEBUG_NAME;
      case LEVEL_INFO:
        return INFO_NAME;
      case LEVEL_WARN:
        return WARN_NAME;
      case LEVEL_ERROR:
        return ERROR_NAME;
      case LEVEL_FATAL:
        return FATAL_NAME;
      default:
        return "Unknown";
    }
  }

  public static ILogLevel valueOf(String v) {
    if (DEBUG_NAME.equals(v)) {
      return DEBUG;
    } else if (INFO_NAME.equals(v)) {
      return INFO;
    } else if (WARN_NAME.equals(v)) {
      return WARN;
    } else if (ERROR_NAME.equals(v)) {
      return ERROR;
    } else if (FATAL_NAME.equals(v)) {
      return FATAL;
    } else {
      return null;
    }
  }

}
