/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.h2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementHandler {
  public void setParameters(PreparedStatement statement) throws SQLException;
}