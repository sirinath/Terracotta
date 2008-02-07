/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class TCStatisticsBufferStartCapturingErrorException extends TCStatisticsBufferException {
  private final long sessionId;

  public TCStatisticsBufferStartCapturingErrorException(final long sessionId, final Throwable cause) {
    super("The capture session with ID '" + sessionId + "' could not be started.", cause);
    this.sessionId = sessionId;
  }

  public long getSessionId() {
    return sessionId;
  }
}