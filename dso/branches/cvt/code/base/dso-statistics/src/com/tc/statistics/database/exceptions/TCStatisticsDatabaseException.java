/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.exceptions;

import com.tc.exception.TCException;

public class TCStatisticsDatabaseException extends TCException {
  public TCStatisticsDatabaseException(String message, Throwable cause) {
    super(message, cause);
  }
}