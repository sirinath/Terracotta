/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.h2;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.statistics.CaptureSession;
import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferCaptureSessionCreationErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferDatabaseCloseErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferDatabaseOpenErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferInstallationErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferSetupErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStartCapturingErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStatisticConsumptionErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStatisticStorageErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStopCapturingErrorException;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.database.StatisticsDatabase;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseException;
import com.tc.statistics.database.impl.H2StatisticsDatabase;
import com.tc.statistics.jdbc.CaptureChecksum;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.jdbc.PreparedStatementHandler;
import com.tc.statistics.jdbc.ResultSetHandler;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.statistics.retrieval.impl.StatisticsRetrieverImpl;
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
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class H2StatisticsBufferImpl implements StatisticsBuffer {
  public final static int DATABASE_STRUCTURE_VERSION = 1;
  
  private final static long DATABASE_STRUCTURE_CHECKSUM = 65402179L;

  public final static String H2_URL_SUFFIX = "statistics-buffer";

  private final static String SQL_NEXT_CAPTURESESSIONID = "SELECT nextval('seq_capturesession')";
  private final static String SQL_NEXT_STATISTICLOGID = "SELECT nextval('seq_statisticlog')";
  private final static String SQL_NEXT_CONSUMPTIONID = "SELECT nextval('seq_consumption')";
  private final static String SQL_MAKE_ALL_CONSUMABLE = "UPDATE statisticlog SET consumptionid = NULL";

  private final StatisticsConfig config;
  private final File lockFile;
  private final StatisticsDatabase database;

  private final Set listeners = new CopyOnWriteArraySet();

  public H2StatisticsBufferImpl(final StatisticsConfig config, final File dbDir) {
    Assert.assertNotNull("config", config);
    this.database = new H2StatisticsDatabase(dbDir, H2_URL_SUFFIX);
    this.config = config;
    this.lockFile = new File(dbDir.getParentFile(), dbDir.getName()+".lck");
  }

  public synchronized void open() throws TCStatisticsBufferException {
    try {
      FileLockGuard.guard(lockFile, new FileLockGuard.Guarded() {
        public void execute() throws FileLockGuard.InnerException {
          try {
            try {
              database.open();
            } catch (TCStatisticsDatabaseException e) {
              throw new TCStatisticsBufferDatabaseOpenErrorException(e);
            }

            install();
            setupPreparedStatements();
            makeAllDataConsumable();
          } catch (TCStatisticsBufferException e) {
            throw new FileLockGuard.InnerException(e);
          }
        }
      });
    } catch (FileLockGuard.InnerException e) {
      throw (TCStatisticsBufferException)e.getInnerException();
    } catch (IOException e) {
      throw new TCStatisticsBufferException("Unexpected error while obtaining or releasing lock file.", e);
    }
  }

  public synchronized void close() throws TCStatisticsBufferException {
    try {
      database.close();
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsBufferDatabaseCloseErrorException(e);
    }
  }

  protected void install() throws TCStatisticsBufferException {
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
              "CREATE SEQUENCE IF NOT EXISTS seq_capturesession");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE TABLE IF NOT EXISTS capturesession (" +
                "id BIGINT NOT NULL PRIMARY KEY, " +
                "start TIMESTAMP NULL, " +
                "stop TIMESTAMP NULL)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE SEQUENCE IF NOT EXISTS seq_statisticlog");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE SEQUENCE IF NOT EXISTS seq_consumption");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE TABLE IF NOT EXISTS statisticlog (" +
                "id BIGINT NOT NULL PRIMARY KEY, " +
                "sessionId BIGINT NOT NULL, " +
                "agentIp VARCHAR(39) NOT NULL, " +
                "moment TIMESTAMP NOT NULL, " +
                "statname VARCHAR(255) NOT NULL," +
                "statelement VARCHAR(255) NULL, " +
                "datanumber BIGINT NULL, " +
                "datatext TEXT NULL, " +
                "datatimestamp TIMESTAMP NULL, " +
                "datadecimal DECIMAL(8, 4) NULL, " +
                "consumptionid BIGINT NULL)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_sessionid ON statisticlog(sessionId)");
            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_consumptionid ON statisticlog(consumptionId)");

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
        throw new TCStatisticsBufferInstallationErrorException(e);
    }
  }

  private void setupPreparedStatements() throws TCStatisticsBufferException {
    try {
      database.createPreparedStatement(SQL_NEXT_CAPTURESESSIONID);
      database.createPreparedStatement(SQL_NEXT_STATISTICLOGID);
      database.createPreparedStatement(SQL_NEXT_CONSUMPTIONID);
      database.createPreparedStatement(SQL_MAKE_ALL_CONSUMABLE);
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsBufferSetupErrorException("Unexpected error while preparing the statements for the H2 statistics buffer.", e);
    }
  }

  private void makeAllDataConsumable() throws TCStatisticsBufferException {
    try {
      database.getPreparedStatement(SQL_MAKE_ALL_CONSUMABLE).executeUpdate();
    } catch (SQLException e) {
      throw new TCStatisticsBufferSetupErrorException("Unexpected error while making all the existing data consumable in the H2 statistics buffer.", e);
    }
  }

  public CaptureSession createCaptureSession() throws TCStatisticsBufferException {
    try {
      database.ensureExistingConnection();

      final long id = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_CAPTURESESSIONID));

      final int row_count = JdbcHelper.executeUpdate(database.getConnection(), "INSERT INTO capturesession (id) VALUES (?)", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, id);
        }
      });

      if (row_count != 1) {
        throw new TCStatisticsBufferCaptureSessionCreationErrorException(id);
      }

      StatisticsRetriever retriever = new StatisticsRetrieverImpl(config.createChild(), this, id);
      return new CaptureSession(id, retriever);
    } catch (Exception e) {
      throw new TCStatisticsBufferCaptureSessionCreationErrorException(e);
    }
  }

  public void startCapturing(final long sessionId) throws TCStatisticsBufferException {
    try {
      database.ensureExistingConnection();

      final int row_count = JdbcHelper.executeUpdate(database.getConnection(), "UPDATE capturesession SET start = ? WHERE id = ? AND start IS NULL", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setTimestamp(1, new Timestamp(new Date().getTime()));
          statement.setLong(2, sessionId);
        }
      });

      if (row_count != 1) {
        throw new TCStatisticsBufferStartCapturingErrorException(sessionId, null);
      }

      fireCapturingStarted(sessionId);
    } catch (Exception e) {
      throw new TCStatisticsBufferStartCapturingErrorException(sessionId, e);
    }
  }

  private void fireCapturingStarted(final long sessionId) {
    Iterator it = listeners.iterator();
    while (it.hasNext()) {
      ((StatisticsBufferListener)it.next()).capturingStarted(sessionId);
    }
  }

  public void stopCapturing(final long sessionId) throws TCStatisticsBufferException {
    try {
      database.ensureExistingConnection();

      fireCapturingStopped(sessionId);

      final int row_count = JdbcHelper.executeUpdate(database.getConnection(), "UPDATE capturesession SET stop = ? WHERE id = ? AND start IS NOT NULL", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setTimestamp(1, new Timestamp(new Date().getTime()));
          statement.setLong(2, sessionId);
        }
      });

      if (row_count != 1) {
        throw new TCStatisticsBufferStopCapturingErrorException(sessionId, null);
      }
    } catch (Exception e) {
      throw new TCStatisticsBufferStopCapturingErrorException(sessionId, e);
    }
  }

  private void fireCapturingStopped(final long sessionId) {
    Iterator it = listeners.iterator();
    while (it.hasNext()) {
      ((StatisticsBufferListener)it.next()).capturingStopped(sessionId);
    }
  }

  public long storeStatistic(final StatisticData data) throws TCStatisticsBufferException {
    Assert.assertNotNull("data", data);
    Assert.assertNotNull("sessionId property of data", data.getSessionId());
    Assert.assertNotNull("agentIp property of data", data.getAgentIp());
    Assert.assertNotNull("data property of data", data.getData());

    try {
      database.ensureExistingConnection();

      // obtain a new ID for the statistic data
      final long id = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_STATISTICLOGID));

      // insert the statistic data with the provided values
      final int row_count = JdbcHelper.executeUpdate(database.getConnection(), "INSERT INTO statisticlog (id, sessionId, agentIp, moment, statname, statelement, datanumber, datatext, datatimestamp, datadecimal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, id);
          statement.setLong(2, data.getSessionId().longValue());
          statement.setString(3, data.getAgentIp());
          statement.setTimestamp(4, new Timestamp(data.getMoment().getTime()));
          statement.setString(5, data.getName());
          if (null == data.getElement()) {
            statement.setNull(6, Types.VARCHAR);
          } else {
            statement.setString(6, data.getElement());
          }
          if (null == data.getData()) {
            statement.setNull(7, Types.BIGINT);
            statement.setNull(8, Types.VARCHAR);
            statement.setNull(9, Types.TIMESTAMP);
            statement.setNull(10, Types.NUMERIC);
          } else if (data.getData() instanceof Number) {
            statement.setLong(7, ((Number)data.getData()).longValue());
            statement.setNull(8, Types.VARCHAR);
            statement.setNull(9, Types.TIMESTAMP);
            statement.setNull(10, Types.NUMERIC);
          } else if (data.getData() instanceof CharSequence) {
            statement.setNull(7, Types.BIGINT);
            statement.setString(8, data.getData().toString());
            statement.setNull(9, Types.TIMESTAMP);
            statement.setNull(10, Types.NUMERIC);
          } else if (data.getData() instanceof Date) {
            statement.setNull(7, Types.BIGINT);
            statement.setNull(8, Types.VARCHAR);
            statement.setTimestamp(9, new java.sql.Timestamp(((Date)data.getData()).getTime()));
            statement.setNull(10, Types.NUMERIC);
          } else if (data.getData() instanceof BigDecimal) {
            statement.setNull(7, Types.BIGINT);
            statement.setNull(8, Types.VARCHAR);
            statement.setNull(9, Types.TIMESTAMP);
            statement.setBigDecimal(10, (BigDecimal)data.getData());
          }
        }
      });

      // ensure that a row was inserted
      if (row_count != 1) {
        throw new TCStatisticsBufferStatisticStorageErrorException(id, data);
      }

      return id;
    } catch (Exception e) {
      throw new TCStatisticsBufferStatisticStorageErrorException(data, e);
    }
  }

  public void consumeStatistics(final long sessionId, final StatisticsConsumer consumer) throws TCStatisticsBufferException {
    Assert.eval("sessionId must be bigger than 0", sessionId > 0);
    Assert.assertNotNull("consumer", consumer);

    try {
      database.ensureExistingConnection();

      // create a unique ID for this consumption phase
      final long consumption_id = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_CONSUMPTIONID));

      // reserve all existing statistic data with the provided session ID
      // for the consumption ID
      final int row_count = JdbcHelper.executeUpdate(database.getConnection(), "UPDATE statisticlog SET consumptionId = ? WHERE consumptionId IS NULL AND sessionId = ?", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, consumption_id);
          statement.setLong(2, sessionId);
        }
      });

      try {
        // consume all the statistic data in this capture session
        if (row_count > 0) {
          JdbcHelper.executeQuery(database.getConnection(), "SELECT * FROM statisticlog WHERE consumptionId = ? AND sessionId = ? ORDER BY moment ASC, id ASC", new PreparedStatementHandler() {
            public void setParameters(PreparedStatement statement) throws SQLException {
              statement.setLong(1, consumption_id);
              statement.setLong(2, sessionId);
            }
          }, new ResultSetHandler() {
            public void useResultSet(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                // obtain the statistics data
                StatisticData data = database.getStatisticsData(resultSet);

                // consume the data
                if (!consumer.consumeStatisticData(data)) {
                  return;
                }
                // delete the consumed statistic data from the log
                resultSet.deleteRow();
              }
            }
          });
        }
      } finally {
        // make the statistic data that wasn't consumed during this consumption phase
        // available again so that it can be picked up by another consumption operation
        JdbcHelper.executeUpdate(database.getConnection(), "UPDATE statisticlog SET consumptionId = NULL WHERE consumptionId = ?", new PreparedStatementHandler() {
          public void setParameters(PreparedStatement statement) throws SQLException {
            statement.setLong(1, consumption_id);
          }
        });
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TCStatisticsBufferStatisticConsumptionErrorException(sessionId, e);
    }
  }

  public void addListener(final StatisticsBufferListener listener) {
    if (null == listener) {
      return;
    }

    listeners.add(listener);
  }

  public void removeListener(final StatisticsBufferListener listener) {
    if (null == listener) {
      return;
    }

    listeners.remove(listener);
  }
}