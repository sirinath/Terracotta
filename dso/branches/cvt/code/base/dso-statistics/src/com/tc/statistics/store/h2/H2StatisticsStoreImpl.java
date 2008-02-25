/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store.h2;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.database.StatisticsDatabase;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseException;
import com.tc.statistics.database.impl.H2StatisticsDatabase;
import com.tc.statistics.jdbc.CaptureChecksum;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.jdbc.PreparedStatementHandler;
import com.tc.statistics.jdbc.ResultSetHandler;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.StatisticsStoreListener;
import com.tc.statistics.store.exceptions.TCStatisticsStoreClearAllStatisticsErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreClearStatisticsErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreCloseErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreInstallationErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreOpenErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreRetrievalErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreSessionIdsRetrievalErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreSetupErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreStatisticStorageErrorException;
import com.tc.util.Assert;
import com.tc.util.concurrent.FileLockGuard;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class H2StatisticsStoreImpl implements StatisticsStore {
  public final static int DATABASE_STRUCTURE_VERSION = 3;
  
  public final static String H2_URL_SUFFIX = "statistics-store";

  private final static long DATABASE_STRUCTURE_CHECKSUM = 436002553L;

  private final static String SQL_NEXT_STATISTICLOGID = "SELECT nextval('seq_statisticlog')";
  private final static String SQL_GET_AVAILABLE_SESSIONIDS = "SELECT sessionid FROM statisticlog GROUP BY sessionid ORDER BY sessionid ASC";

  private final StatisticsDatabase database;
  private final File lockFile;

  private final Set listeners = new CopyOnWriteArraySet();

  public H2StatisticsStoreImpl(final File dbDir) {
    this.database = new H2StatisticsDatabase(dbDir, H2_URL_SUFFIX);
    this.lockFile = new File(dbDir.getParentFile(), dbDir.getName()+".lck");
  }

  public void open() throws TCStatisticsStoreException {
    synchronized (this) {
      try {
        FileLockGuard.guard(lockFile, new FileLockGuard.Guarded() {
          public void execute() throws FileLockGuard.InnerException {
            try {
              try {
                database.open();
              } catch (TCStatisticsDatabaseException e) {
                throw new TCStatisticsStoreOpenErrorException(e);
              }

              install();
              setupPreparedStatements();
            } catch (TCStatisticsStoreException e) {
              throw new FileLockGuard.InnerException(e);
            }
          }
        });
      } catch (FileLockGuard.InnerException e) {
        throw (TCStatisticsStoreException)e.getInnerException();
      } catch (IOException e) {
        throw new TCStatisticsStoreException("Unexpected error while obtaining or releasing lock file.", e);
      }
    }

    fireOpened();
  }

  public void close() throws TCStatisticsStoreException {
    synchronized (this) {
      try {
        database.close();
      } catch (TCStatisticsDatabaseException e) {
        throw new TCStatisticsStoreCloseErrorException(e);
      }
    }

    fireClosed();
  }

  protected void install() throws TCStatisticsStoreException {
    try {
      database.ensureExistingConnection();

      JdbcHelper.calculateChecksum(new CaptureChecksum() {
        public void execute() throws Exception {
          database.getConnection().setAutoCommit(false);

          try {
            /*====================================================================
              == !!! IMPORTANT !!!
              ==
              == Any significant change to the structure of the database
              == should increase the version number of the database, which is
              == stored in the DATABASE_STRUCTURE_VERSION field of this class.
              == You will need to update the DATABASE_STRUCTURE_CHECKSUM field
              == also since it serves as a safeguard to ensure that the version is
              == always adapted. The correct checksum value will be given to you
              == when a checksum mismatch is detected.
              ====================================================================*/

            database.createVersionTable();

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE SEQUENCE IF NOT EXISTS seq_statisticlog");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE TABLE IF NOT EXISTS statisticlog (" +
                "id BIGINT NOT NULL PRIMARY KEY, " +
                "sessionid VARCHAR(255) NOT NULL, " +
                "agentip VARCHAR(39) NOT NULL, " +
                "agentdifferentiator VARCHAR(255) NULL, " +
                "moment TIMESTAMP NOT NULL, " +
                "statname VARCHAR(255) NOT NULL," +
                "statelement VARCHAR(255) NULL, " +
                "datanumber BIGINT NULL, " +
                "datatext TEXT NULL, " +
                "datatimestamp TIMESTAMP NULL, " +
                "datadecimal DECIMAL(8, 4) NULL)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_sessionid ON statisticlog(sessionid)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_agentip ON statisticlog(agentip)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_agentdifferentiator ON statisticlog(agentdifferentiator)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_moment ON statisticlog(moment)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_statname ON statisticlog(statname)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_statelement ON statisticlog(statelement)");

            database.getConnection().commit();
          } catch (Exception e) {
            database.getConnection().rollback();
            throw e;
          } finally {
            database.getConnection().setAutoCommit(true);
          }

          database.checkVersion(DATABASE_STRUCTURE_VERSION, DATABASE_STRUCTURE_CHECKSUM);
        }
      });
    } catch (Exception e) {
      throw new TCStatisticsStoreInstallationErrorException(e);
    }
  }

  private void setupPreparedStatements() throws TCStatisticsStoreException {
    try {
      database.createPreparedStatement(SQL_NEXT_STATISTICLOGID);
      database.createPreparedStatement(SQL_GET_AVAILABLE_SESSIONIDS);
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsStoreSetupErrorException(e);
    }
  }

  public void storeStatistic(final StatisticData data) throws TCStatisticsStoreException {
    Assert.assertNotNull("data", data);
    Assert.assertNotNull("sessionId property of data", data.getSessionId());
    Assert.assertNotNull("agentIp property of data", data.getAgentIp());
    Assert.assertNotNull("data property of data", data.getData());

    final long id;
    final int row_count;

    try {
      database.ensureExistingConnection();

      // obtain a new ID for the statistic data
      id = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_STATISTICLOGID));
      
      // insert the statistic data with the provided values
      row_count = JdbcHelper.executeUpdate(database.getConnection(), "INSERT INTO statisticlog (id, sessionid, agentip, agentdifferentiator, moment, statname, statelement, datanumber, datatext, datatimestamp, datadecimal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, id);
          statement.setString(2, data.getSessionId());
          statement.setString(3, data.getAgentIp());
          if (null == data.getAgentDifferentiator()) {
            statement.setNull(4, Types.VARCHAR);
          } else {
            statement.setString(4, data.getAgentDifferentiator());
          }
          statement.setTimestamp(5, new Timestamp(data.getMoment().getTime()));
          statement.setString(6, data.getName());
          if (null == data.getElement()) {
            statement.setNull(7, Types.VARCHAR);
          } else {
            statement.setString(7, data.getElement());
          }
          if (null == data.getData()) {
            statement.setNull(8, Types.BIGINT);
            statement.setNull(9, Types.VARCHAR);
            statement.setNull(10, Types.TIMESTAMP);
            statement.setNull(11, Types.NUMERIC);
          } else if (data.getData() instanceof BigDecimal) {
            statement.setNull(8, Types.BIGINT);
            statement.setNull(9, Types.VARCHAR);
            statement.setNull(10, Types.TIMESTAMP);
            statement.setBigDecimal(11, (BigDecimal)data.getData());
          } else if (data.getData() instanceof Number) {
            statement.setLong(8, ((Number)data.getData()).longValue());
            statement.setNull(9, Types.VARCHAR);
            statement.setNull(10, Types.TIMESTAMP);
            statement.setNull(11, Types.NUMERIC);
          } else if (data.getData() instanceof CharSequence) {
            statement.setNull(8, Types.BIGINT);
            statement.setString(9, data.getData().toString());
            statement.setNull(10, Types.TIMESTAMP);
            statement.setNull(11, Types.NUMERIC);
          } else if (data.getData() instanceof Date) {
            statement.setNull(8, Types.BIGINT);
            statement.setNull(9, Types.VARCHAR);
            statement.setTimestamp(10, new java.sql.Timestamp(((Date)data.getData()).getTime()));
            statement.setNull(11, Types.NUMERIC);
          }
        }
      });
    } catch (Exception e) {
      throw new TCStatisticsStoreStatisticStorageErrorException(data, e);
    }

    // ensure that a row was inserted
    if (row_count != 1) {
      throw new TCStatisticsStoreStatisticStorageErrorException(id, data, null);
    }
  }

  public void retrieveStatistics(final StatisticsRetrievalCriteria criteria, final StatisticsConsumer consumer) throws TCStatisticsStoreException {
    Assert.assertNotNull("criteria", criteria);

    try {
      database.ensureExistingConnection();

      List sql_where = new ArrayList();
      if (criteria.getAgentIp() != null) {
        sql_where.add("agentip = ?");
      }
      if (criteria.getAgentDifferentiator() != null) {
        sql_where.add("agentdifferentiator = ?");
      }
      if (criteria.getSessionId() != null) {
        sql_where.add("sessionid = ?");
      }
      if (criteria.getStart() != null) {
        sql_where.add("moment >= ?");
      }
      if (criteria.getStop() != null) {
        sql_where.add("moment <= ?");
      }
      if (criteria.getNames().size() > 0) {
        StringBuffer where_names = new StringBuffer();
        for (int i = 0 ; i < criteria.getNames().size(); i++) {
          if (where_names.length() > 0) {
            where_names.append(", ");
          }
          where_names.append("?");
        }
        sql_where.add("statname IN ("+where_names+")");
      }
      if (criteria.getElements().size() > 0) {
        StringBuffer where_elements = new StringBuffer();
        for (int i = 0 ; i < criteria.getElements().size(); i++) {
          if (where_elements.length() > 0) {
            where_elements.append(", ");
          }
          where_elements.append("?");
        }
        sql_where.add("statelement IN ("+where_elements+")");
      }

      StringBuffer sql = new StringBuffer("SELECT * FROM statisticlog");
      if (sql_where.size() > 0) {
        sql.append(" WHERE ");
        boolean first = true;
        Iterator it = sql_where.iterator();
        while (it.hasNext()) {
          if (first) {
            first = false;
          } else {
            sql.append(" AND ");
          }
          sql.append(it.next());
        }
      }
      sql.append(" ORDER BY sessionid ASC, moment ASC, id ASC");

      JdbcHelper.executeQuery(database.getConnection(), sql.toString(), new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          int param = 1;
          if (criteria.getAgentIp() != null) {
            statement.setString(param++, criteria.getAgentIp());
          }
          if (criteria.getAgentDifferentiator() != null) {
            statement.setString(param++, criteria.getAgentDifferentiator());
          }
          if (criteria.getSessionId() != null) {
            statement.setString(param++, criteria.getSessionId());
          }
          if (criteria.getStart() != null) {
            statement.setTimestamp(param++, new Timestamp(criteria.getStart().getTime()));
          }
          if (criteria.getStop() != null) {
            statement.setTimestamp(param++, new Timestamp(criteria.getStop().getTime()));
          }
          if (criteria.getNames().size() > 0) {
            Iterator it = criteria.getNames().iterator();
            while (it.hasNext()) {
              statement.setString(param++, (String)it.next());
            }
          }
          if (criteria.getElements().size() > 0) {
            Iterator it = criteria.getElements().iterator();
            while (it.hasNext()) {
              statement.setString(param++, (String)it.next());
            }
          }
        }
      }, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            StatisticData data = database.getStatisticsData(resultSet.getString("sessionid"), resultSet);

            // consume the data
            if (!consumer.consumeStatisticData(data)) {
              return;
            }
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TCStatisticsStoreRetrievalErrorException(criteria, e);
    }
  }

  public String[] getAvailableSessionIds() throws TCStatisticsStoreException {
    final List results = new ArrayList();
    try {
      database.ensureExistingConnection();

      JdbcHelper.executeQuery(database.getPreparedStatement(SQL_GET_AVAILABLE_SESSIONIDS), new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            results.add(resultSet.getString("sessionid"));
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TCStatisticsStoreSessionIdsRetrievalErrorException(e);
    }

    String[] result_array = new String[results.size()];
    int i = 0;
    Iterator it = results.iterator();
    while (it.hasNext()) {
      result_array[i++] = (String)it.next();
    }

    return result_array;
  }

  public void clearStatistics(final String sessionId) throws TCStatisticsStoreException {
    try {
      database.ensureExistingConnection();

      // remove statistics, based on the provided session Id
      JdbcHelper.executeUpdate(database.getConnection(), "DELETE FROM statisticlog WHERE sessionid = ?", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setString(1, sessionId);
        }
      });
    } catch (Exception e) {
      throw new TCStatisticsStoreClearStatisticsErrorException(sessionId, e);
    }

    fireSessionCleared(sessionId);
  }

  public void clearAllStatistics() throws TCStatisticsStoreException {
    try {
      database.ensureExistingConnection();
      
      JdbcHelper.executeUpdate(database.getConnection(), "DELETE FROM statisticlog");
    } catch (Exception e) {
      throw new TCStatisticsStoreClearAllStatisticsErrorException(e);
    }

    fireAllSessionsCleared();
  }

  public void addListener(final StatisticsStoreListener listener) {
    if (null == listener) {
      return;
    }

    listeners.add(listener);
  }

  public void removeListener(final StatisticsStoreListener listener) {
    if (null == listener) {
      return;
    }

    listeners.remove(listener);
  }

  private void fireOpened() {
    Iterator it = listeners.iterator();
    while (it.hasNext()) {
      ((StatisticsStoreListener)it.next()).opened();
    }
  }

  private void fireClosed() {
    Iterator it = listeners.iterator();
    while (it.hasNext()) {
      ((StatisticsStoreListener)it.next()).closed();
    }
  }

  private void fireSessionCleared(final String sessionId) {
    Iterator it = listeners.iterator();
    while (it.hasNext()) {
      ((StatisticsStoreListener)it.next()).sessionCleared(sessionId);
    }
  }

  private void fireAllSessionsCleared() {
    Iterator it = listeners.iterator();
    while (it.hasNext()) {
      ((StatisticsStoreListener)it.next()).allSessionsCleared();
    }
  }
}