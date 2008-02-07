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
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseStatementPreparationErrorException;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseStoreVersionErrorException;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseStructureFuturedatedException;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseStructureOutdatedException;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseVersionCheckErrorException;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.jdbc.PreparedStatementHandler;
import com.tc.statistics.jdbc.ResultSetHandler;
import com.tc.statistics.jdbc.ChecksumCalculator;
import com.tc.util.Assert;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStatisticsDatabase implements StatisticsDatabase {
  protected Connection connection;
  protected final Map preparedStatements = new HashMap();

  protected synchronized void open(final String driver) throws TCStatisticsDatabaseException {
    if (connection != null) return;

    try {
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      throw new TCStatisticsDatabaseNotFoundException(driver, e);
    }

    openConnection();
  }

  protected abstract void openConnection() throws TCStatisticsDatabaseException;
  
  public void ensureExistingConnection() throws TCStatisticsDatabaseException {
    if (null == connection) {
      throw new TCStatisticsDatabaseNotReadyException();
    }
  }

  public Connection getConnection() {
    return connection;
  }

  public PreparedStatement createPreparedStatement(final String sql) throws TCStatisticsDatabaseException {
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
      throw new TCStatisticsDatabaseStatementPreparationErrorException(sql, e);
    }
  }

  public PreparedStatement getPreparedStatement(final String sql) {
    synchronized (preparedStatements) {
      return (PreparedStatement)preparedStatements.get(sql);
    }
  }

  public void createVersionTable() throws SQLException {
    JdbcHelper.executeUpdate(getConnection(),
        "CREATE TABLE IF NOT EXISTS dbstructureversion (" +
          "version INT NOT NULL PRIMARY KEY, "+
          "created TIMESTAMP NOT NULL)");
  }

  // TODO: Currently version checks just fail hard when they don't match, in the future this should
  // be made more intelligent to automatically migrate from older versions to newer ones.
  public void checkVersion(final int currentVersion, long currentChecksum, ChecksumCalculator csc) throws TCStatisticsDatabaseException {
    long checksum = csc.checksum();
    Assert.assertTrue("The checksum of the SQL that creates the database structure doesn't correspond to the checksum that corresponds to the version number of the database structure. Any significant change to the database structure should increase the version number and adapt the SQL checksum. The current checksum is "+checksum+"L.", currentChecksum == checksum);

    final Integer[] version = new Integer[1];
    final Date[] created = new Date[1];

    try {
      JdbcHelper.executeQuery(getConnection(), "SELECT version, created FROM dbstructureversion", new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          if (resultSet.next()) {
            version[0] = new Integer(resultSet.getInt("version"));
            created[0] = resultSet.getTimestamp("created");
          }
        }
      });
    } catch (SQLException e) {
      throw new TCStatisticsDatabaseVersionCheckErrorException("Unexpected error while checking the version.", e);
    }

    if (null == version[0]) {
      storeCurrentVersion(currentVersion);
    } else {
      if (version[0].intValue() < currentVersion) {
        throw new TCStatisticsDatabaseStructureOutdatedException(version[0].intValue(), currentVersion, created[0]);
      } else if (version[0].intValue() > currentVersion) {
        throw new TCStatisticsDatabaseStructureFuturedatedException(version[0].intValue(), currentVersion, created[0]);
      }
    }
  }

  private void storeCurrentVersion(final int currentVersion) throws TCStatisticsDatabaseException {
    try {
      JdbcHelper.executeUpdate(getConnection(), "INSERT INTO dbstructureversion (version, created) VALUES (?, now())", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setInt(1, currentVersion);
        }
      });
    } catch (SQLException e) {
      throw new TCStatisticsDatabaseStoreVersionErrorException(currentVersion, e);
    }
  }

  public StatisticData getStatisticsData(final ResultSet resultSet) throws SQLException {
    StatisticData data = new StatisticData()
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
      throw new TCStatisticsDatabaseCloseErrorException(exception);
    }
  }
}