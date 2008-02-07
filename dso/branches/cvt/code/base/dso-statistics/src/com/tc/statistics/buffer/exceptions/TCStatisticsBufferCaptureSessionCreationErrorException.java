/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class TCStatisticsBufferCaptureSessionCreationErrorException extends TCStatisticsBufferException {
  private final Long sessionId;

  public TCStatisticsBufferCaptureSessionCreationErrorException(final long sessionId) {
    super("A new capture session could not be created with ID '" + sessionId + "'.", null);
    this.sessionId = new Long(sessionId);
  }

  public TCStatisticsBufferCaptureSessionCreationErrorException(final Throwable cause) {
    super("Unexpected error while creating a new capture session.", cause);
    this.sessionId = null;
  }

  public Long getSessionId() {
    return sessionId;
  }
}
