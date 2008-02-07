/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class TCStatisticsBufferStopCapturingErrorException extends TCStatisticsBufferException {
  private final long sessionId;

  public TCStatisticsBufferStopCapturingErrorException(final long sessionId, final Throwable cause) {
    super("The capture session with ID '" + sessionId + "' could not be stopped.", cause);
    this.sessionId = sessionId;
  }

  public long getSessionId() {
    return sessionId;
  }
}