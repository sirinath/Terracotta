/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

import com.tc.exception.TCException;

public class TCStatisticsGathererException extends TCException {
  public TCStatisticsGathererException(String message, Throwable cause) {
    super(message, cause);
  }
}