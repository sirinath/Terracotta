/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

public class DatabaseOpenException extends TCDatabaseException {
  public DatabaseOpenException(String message) {
    super(message);
  }
}
