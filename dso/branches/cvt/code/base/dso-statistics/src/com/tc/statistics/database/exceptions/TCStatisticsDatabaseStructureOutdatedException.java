/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.exceptions;

import java.util.Date;
import java.text.SimpleDateFormat;

public class TCStatisticsDatabaseStructureOutdatedException extends TCStatisticsDatabaseVersionCheckErrorException {
  private final int actual;
  private final int expected;
  private final Date created;

  public TCStatisticsDatabaseStructureOutdatedException(final int actual, final int expected, final Date created) {
    super("The structure of the database is outdated. It has version number "+actual+", while version "+expected+" was expected. It was created on "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS").format(created)+".", null);
    this.actual = actual;
    this.expected = expected;
    this.created = created;
  }

  public int getActualVersion() {
    return actual;
  }

  public int getExpectedVersion() {
    return expected;
  }

  public Date getCreationDate() {
    return created;
  }
}