/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.impl;

import com.tc.statistics.StatisticData;
import com.tc.statistics.database.StatisticsDatabase;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseCloseErrorException;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseException;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseNotFoundException;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseNotReadyException;
import com.tc.util.Assert;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStatisticsDatabase implements StatisticsDatabase {
  protected Connection connection;
  protected final Map preparedStatements = new HashMap();

  protected synchronized void open(String driver) throws TCStatisticsDatabaseException {
    if (connection != null) return;

    try {
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      throw new TCStatisticsDatabaseNotFoundException("Unable to load JDBC driver '" + driver + "'", e);
    }

    openConnection();
  }

  protected abstract void openConnection() throws TCStatisticsDatabaseException;
  
  public void ensureExistingConnection() throws TCStatisticsDatabaseException {
    if (null == connection) {
      throw new TCStatisticsDatabaseNotReadyException("Connection to database not established beforehand, call open() before performing another operation.");
    }
  }

  public Connection getConnection() {
    return connection;
  }

  public PreparedStatement createPreparedStatement(String sql) throws TCStatisticsDatabaseException {
    ensureExistingConnection();

    try {
      PreparedStatement stmt = connection.prepareStatement(sql);
      synchronized (preparedStatements) {
        PreparedStatement previous = (PreparedStatement)preparedStatements.put(sql, stmt);
        if (previous != null) {
          previous.close();
        }
      }
      return stmt;
    } catch (SQLException e) {
      throw new TCStatisticsDatabaseException("Unexpected error while preparing a statement for SQL '"+sql+"'.", e);
    }
  }

  public PreparedStatement getPreparedStatement(String sql) {
    synchronized (preparedStatements) {
      return (PreparedStatement)preparedStatements.get(sql);
    }
  }

  public StatisticData getStatisticsData(ResultSet resultSet) throws SQLException {
    StatisticData data;// obtain the statistics data
    data = new StatisticData()
      .sessionId(new Long(resultSet.getLong("sessionId")))
      .agentIp(resultSet.getString("agentIp"))
      .moment(resultSet.getTimestamp("moment"))
      .name(resultSet.getString("statname"))
      .element(resultSet.getString("statelement"));

    long datanumber = resultSet.getLong("datanumber");
    if (!resultSet.wasNull()) {
      data.data(new Long(datanumber));
    } else {
      String datatext = resultSet.getString("datatext");
      if (!resultSet.wasNull()) {
        data.data(datatext);
      } else {
        Timestamp datatimestamp = resultSet.getTimestamp("datatimestamp");
        if (!resultSet.wasNull()) {
          data.data(datatimestamp);
        } else {
          BigDecimal datadecimal = resultSet.getBigDecimal("datadecimal");
          Assert.eval("All the data elements of the statistic data were NULL, this shouldn't be possible.",
            !resultSet.wasNull());
          data.data(datadecimal);
        }
      }
    }
    return data;
  }

  public synchronized void close() throws TCStatisticsDatabaseException {
    if (null == connection) return;

    SQLException exception = null;

    try {
      try {
        synchronized (preparedStatements) {
          Set entries = preparedStatements.entrySet();
          Iterator entries_it = entries.iterator();
          while (entries_it.hasNext()) {
            Map.Entry entry = (Map.Entry)entries_it.next();
            PreparedStatement stmt = (PreparedStatement)entry.getValue();
            try {
              stmt.close();
            } catch (SQLException e) {
              if (exception != null) {
                e.setNextException(exception);
              }
              exception = e;
            }
          }

          preparedStatements.clear();
        }
      } finally {
        connection.close();
      }
    } catch (SQLException e) {
      if (exception != null) {
        e.setNextException(exception);
      }
      exception = e;
    } finally {
      connection = null;
    }

    if (exception != null) {
      throw new TCStatisticsDatabaseCloseErrorException("Unexpected error while closing the connection with the database.", exception);
    }
  }
}
