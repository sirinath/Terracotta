/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class Log4JAppenderToTCAppender extends AppenderSkeleton {

  private final TCAppender appender;

  public Log4JAppenderToTCAppender(TCAppender appender) {
    this.appender = appender;
  }

  @Override
  protected void append(LoggingEvent event) {
    ThrowableInformation throwableInformation = event.getThrowableInformation();
    Throwable t = (throwableInformation == null) ? null : throwableInformation.getThrowable();
    appender.append(LogLevelImpl.fromLog4JLevel(event.getLevel()), event.getMessage(), t);
  }

  @Override
  public void close() {
    //
  }

  @Override
  public boolean requiresLayout() {
    return false;
  }

}
