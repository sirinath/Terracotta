/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;

public class DatabaseDirtyException extends TCDatabaseException implements com.tc.exception.DatabaseException {

  public DatabaseDirtyException(DatabaseException cause) {
    super(cause);
  }

  public DatabaseDirtyException(String message) {
    super(message);
  }

}
