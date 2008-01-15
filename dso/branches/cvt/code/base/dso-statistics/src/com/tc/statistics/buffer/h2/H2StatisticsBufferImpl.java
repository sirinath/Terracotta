/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.h2;

import com.tc.statistics.StatisticData;
import com.tc.statistics.CaptureSession;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.statistics.retrieval.impl.StatisticsRetrieverImpl;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferBackendNotFoundException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferCaptureSessionCreationErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferInstallationErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferNotReadyException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStartCapturingErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStatisticStorageErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStopCapturingErrorException;
import com.tc.util.Assert;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.math.BigDecimal;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

public class H2StatisticsBufferImpl implements StatisticsBuffer {
  private final static String H2_JDBC_DRIVER = "org.h2.Driver";
  private final static String H2_URL_PREFIX = "jdbc:h2:";
  private final static String H2_URL_SUFFIX = "statistics";
  private final static String H2_USER = "sa";
  private final static String H2_PASSWORD = "";

  private Set listeners = new CopyOnWriteArraySet();

  private File statDir;
  private Connection connection;
  private PreparedStatement psGetNextCaptureSessionId;
  private PreparedStatement psGetNextStatisticLogId;
  private PreparedStatement psGetNextConsumptionId;
  private PreparedStatement psMakeAllDataConsumable;

  public H2StatisticsBufferImpl(final File statDir) {
    if (null == statDir) Assert.fail("statDir can't be null");
    if (!statDir.exists()) Assert.fail("statDir '" + statDir.getAbsolutePath() + "' doesn't exist");
    if (!statDir.isDirectory()) Assert.fail("statDir '" + statDir.getAbsolutePath() + "' is not a directory");
    if (!statDir.canWrite()) Assert.fail("statDir '" + statDir.getAbsolutePath() + "' is not writable");
    this.statDir = statDir;
  }

  public synchronized void open() throws TCStatisticsBufferException {
    if (connection != null) return;

    try {
      Class.forName(H2_JDBC_DRIVER);
    } catch (ClassNotFoundException e) {
      throw new TCStatisticsBufferBackendNotFoundException("Unable to load JDBC driver '" + H2_JDBC_DRIVER + "'", e);
    }

    openConnection();
    install();
    setupPreparedStatements();
    makeAllDataConsumable();
  }

  private void openConnection() throws TCStatisticsBufferException {
    String url = H2_URL_PREFIX + new File(statDir, H2_URL_SUFFIX).getAbsolutePath();
    try {
      connection = DriverManager.getConnection(url, H2_USER, H2_PASSWORD);
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      throw new TCStatisticsBufferException("Can't connect to H2 database with URL '" + url + "', user '" + H2_USER + "' and password '" + H2_PASSWORD + "'", e);
    }
  }

  private void ensureExistingConnection() throws TCStatisticsBufferNotReadyException {
    if (null == connection) {
      throw new TCStatisticsBufferNotReadyException("Connection to H2 database not established beforehand, call open() before performing another operation on the StatisticsBuffer.");
    }
  }

  private void install() throws TCStatisticsBufferException {
    ensureExistingConnection();
    try {
      connection.setAutoCommit(false);

      executeUpdate("CREATE SEQUENCE IF NOT EXISTS seq_capturesession");

      executeUpdate("CREATE TABLE IF NOT EXISTS capturesession (" +
                    "id BIGINT NOT NULL PRIMARY KEY, " +
                    "start TIMESTAMP NULL, " +
                    "stop TIMESTAMP NULL)");

      executeUpdate("CREATE SEQUENCE IF NOT EXISTS seq_statisticlog");

      executeUpdate("CREATE SEQUENCE IF NOT EXISTS seq_consumption");

      executeUpdate("CREATE TABLE IF NOT EXISTS statisticlog (" +
                    "id BIGINT NOT NULL PRIMARY KEY, " +
                    "capturesessionid BIGINT NOT NULL, " +
                    "agentip VARCHAR(39) NOT NULL, " +
                    "moment TIMESTAMP NOT NULL, " +
                    "statname VARCHAR(255) NOT NULL," +
                    "statelement VARCHAR(255) NULL, " +
                    "datanumber BIGINT NULL, " +
                    "datatext TEXT NULL, " +
                    "datatimestamp TIMESTAMP NULL, " +
                    "datadecimal DECIMAL(8, 4) NULL, " +
                    "consumptionid BIGINT NULL)");

      executeUpdate("CREATE INDEX IF NOT EXISTS idx_statisticlog_capturesessionid ON statisticlog(capturesessionid)");
      executeUpdate("CREATE INDEX IF NOT EXISTS idx_statisticlog_consumptionid ON statisticlog(consumptionid)");

      connection.commit();
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      throw new TCStatisticsBufferInstallationErrorException("Unable to install the H2 database table structure.", e);
    }
  }

  private void executeUpdate(String sql) throws SQLException {
    Statement stmt = connection.createStatement();
    try {
      stmt.executeUpdate(sql);
    } finally {
      stmt.close();
    }
  }

  private void setupPreparedStatements() throws TCStatisticsBufferException {
    try {
      psGetNextCaptureSessionId = connection.prepareStatement("SELECT nextval('seq_capturesession')");
      psGetNextStatisticLogId = connection.prepareStatement("SELECT nextval('seq_statisticlog')");
      psGetNextConsumptionId = connection.prepareStatement("SELECT nextval('seq_consumption')");
      psMakeAllDataConsumable = connection.prepareStatement("UPDATE statisticlog SET consumptionid = NULL");
    } catch (SQLException e) {
      throw new TCStatisticsBufferException("Unexpected error while preparing the statements for the H2 statitistics buffer.", e);
    }
  }

  private void makeAllDataConsumable() throws TCStatisticsBufferException {
    try {
      psMakeAllDataConsumable.executeUpdate();
    } catch (SQLException e) {
      throw new TCStatisticsBufferException("Unexpected error while making all the existing data consumable in the H2 statitistics buffer.", e);
    }
  }

  public synchronized void close() throws TCStatisticsBufferException {
    if (null == connection) return;

    try {
      try {
        psGetNextCaptureSessionId.close();
        psGetNextStatisticLogId.close();
        psGetNextConsumptionId.close();
      } finally {
        connection.close();
      }
    } catch (SQLException e) {
      throw new TCStatisticsBufferException("Unexpected error while closing the connection with the H2 database.", e);
    } finally {
      psGetNextCaptureSessionId = null;
      psGetNextStatisticLogId = null;
      connection = null;
    }
  }

  public CaptureSession createCaptureSession() throws TCStatisticsBufferException {
    ensureExistingConnection();

    try {
      final long id = JdbcHelper.fetchNextSequenceValue(psGetNextCaptureSessionId);

      final int row_count = JdbcHelper.executeUpdateQuery(connection, "INSERT INTO capturesession (id) VALUES (?)", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, id);
        }
      });

      if (row_count != 1) {
        throw new TCStatisticsBufferCaptureSessionCreationErrorException("A new capture session could not be created with ID '" + id + "'.", null);
      }

      StatisticsRetriever retriever = new StatisticsRetrieverImpl(this, id);
      return new CaptureSession(id, retriever);
    } catch (SQLException e) {
      throw new TCStatisticsBufferCaptureSessionCreationErrorException("Unexpected error while creating a new capture session", e);
    }
  }

  public void startCapturing(final long sessionId) throws TCStatisticsBufferException {
    ensureExistingConnection();

    try {
      final int row_count = JdbcHelper.executeUpdateQuery(connection, "UPDATE capturesession SET start = ? WHERE id = ? AND start IS NULL", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setTimestamp(1, new Timestamp(new Date().getTime()));
          statement.setLong(2, sessionId);
        }
      });

      if (row_count != 1) {
        throw new TCStatisticsBufferStartCapturingErrorException("The capture session with ID '" + sessionId + "' could not be started", null);
      }

      fireCapturingStarted(sessionId);
    } catch (SQLException e) {
      throw new TCStatisticsBufferStartCapturingErrorException("The capture session with ID '" + sessionId + "' could not be started", e);
    }
  }

  private void fireCapturingStarted(long sessionId) {
    Iterator it = listeners.iterator();
    while (it.hasNext()) {
      ((StatisticsBufferListener)it.next()).capturingStarted(sessionId);
    }
  }

  public void stopCapturing(final long sessionId) throws TCStatisticsBufferException {
    ensureExistingConnection();

    try {
      fireCapturingStopped(sessionId);

      final int row_count = JdbcHelper.executeUpdateQuery(connection, "UPDATE capturesession SET stop = ? WHERE id = ? AND start IS NOT NULL", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setTimestamp(1, new Timestamp(new Date().getTime()));
          statement.setLong(2, sessionId);
        }
      });

      if (row_count != 1) {
        throw new TCStatisticsBufferStopCapturingErrorException("The capture session with ID '" + sessionId + "' could not be stopped", null);
      }
    } catch (SQLException e) {
      throw new TCStatisticsBufferStopCapturingErrorException("The capture session with ID '" + sessionId + "' could not be stopped", e);
    }
  }

  private void fireCapturingStopped(long sessionId) {
    Iterator it = listeners.iterator();
    while (it.hasNext()) {
      ((StatisticsBufferListener)it.next()).capturingStopped(sessionId);
    }
  }

  public long storeStatistic(final long sessionId, final StatisticData data) throws TCStatisticsBufferException {
    Assert.assertNotNull("data", data);
    Assert.assertNotNull("agentIp property of data", data.getAgentIp());
    Assert.assertNotNull("data property of data", data.getData());

    ensureExistingConnection();

    try {
      // obtain a new ID for the statistic data
      final long id = JdbcHelper.fetchNextSequenceValue(psGetNextStatisticLogId);

      // insert the statistic data with the provided values
      final int row_count = JdbcHelper.executeUpdateQuery(connection, "INSERT INTO statisticlog (id, capturesessionid, agentip, moment, statname, statelement, datanumber, datatext, datatimestamp, datadecimal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, id);
          statement.setLong(2, sessionId);
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
        throw new TCStatisticsBufferStatisticStorageErrorException("Unexpected error while storing the statistic with id '" + id + "' and data " + data + ".", null);
      }

      return id;
    } catch (SQLException e) {
      throw new TCStatisticsBufferStatisticStorageErrorException("Unexpected error while storing the statistic data " + data + ".", e);
    }
  }

  public void consumeStatistics(final long sessionId, final StatisticsConsumer consumer) throws TCStatisticsBufferException {
    Assert.eval("sessionId must be bigger than 0", sessionId > 0);
    Assert.assertNotNull("consumer", consumer);

    ensureExistingConnection();

    try {
      // create a unique ID for this consumption phase
      final long consumption_id = JdbcHelper.fetchNextSequenceValue(psGetNextConsumptionId);

      // reserve all existing statistic data with the provided session ID
      // for the consumption ID
      final int row_count = JdbcHelper.executeUpdateQuery(connection, "UPDATE statisticlog SET consumptionid = ? WHERE consumptionid IS NULL AND capturesessionid = ?", new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, consumption_id);
          statement.setLong(2, sessionId);
        }
      });

      try {
        // consume all the statistic data in this capture session
        if (row_count > 0) {
          JdbcHelper.executeQuery(connection, "SELECT * FROM statisticlog WHERE consumptionid = ? AND capturesessionid = ? ORDER BY moment ASC, id ASC", new PreparedStatementHandler() {
            public void setParameters(PreparedStatement statement) throws SQLException {
              statement.setLong(1, consumption_id);
              statement.setLong(2, sessionId);
            }
          }, new ResultSetHandler() {
            public void useResultSet(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                // obtain the statistics data
                StatisticData data = new StatisticData()
                  .agentIp(resultSet.getString("agentip"))
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

                // consume the data
                if (!consumer.consumeStatisticData(sessionId, data)) {
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
        JdbcHelper.executeUpdateQuery(connection, "UPDATE statisticlog SET consumptionid = NULL WHERE consumptionid = ?", new PreparedStatementHandler() {
          public void setParameters(PreparedStatement statement) throws SQLException {
            statement.setLong(1, consumption_id);
          }
        });
      }
    } catch (SQLException e) {
      throw new TCStatisticsBufferStatisticStorageErrorException("Unexpected error while consuming the statistic data for session with ID '" + sessionId + "'.", e);
    }
  }

  public void addListener(StatisticsBufferListener listener) {
    if (null == listener) {
      return;
    }

    listeners.add(listener);
  }

  public void removeListener(StatisticsBufferListener listener) {
    if (null == listener) {
      return;
    }

    listeners.remove(listener);
  }
}