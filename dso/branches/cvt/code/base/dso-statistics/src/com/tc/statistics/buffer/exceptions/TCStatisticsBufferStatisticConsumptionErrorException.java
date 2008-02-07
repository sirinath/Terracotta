/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class TCStatisticsBufferStatisticConsumptionErrorException extends TCStatisticsBufferException {
  private final long sessionId;

  public TCStatisticsBufferStatisticConsumptionErrorException(final long sessionId, final Throwable cause) {
    super("Unexpected error while consuming the statistic data for session with ID '" + sessionId + "'.", cause);
    this.sessionId = sessionId;
  }

  public long getSessionId() {
    return sessionId;
  }
}