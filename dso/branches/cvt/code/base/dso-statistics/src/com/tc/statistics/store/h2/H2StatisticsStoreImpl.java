/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store.h2;

import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.database.StatisticsDatabase;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseException;
import com.tc.statistics.database.impl.H2StatisticsDatabase;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.jdbc.PreparedStatementHandler;
import com.tc.statistics.jdbc.ResultSetHandler;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreStatisticStorageErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreOpenErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreCloseErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreInstallationErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreSetupErrorException;
import com.tc.statistics.store.exceptions.TCStatisticsStoreRetrievalErrorException;
import com.tc.util.Assert;

import java.io.File;
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

public class H2StatisticsStoreImpl implements StatisticsStore {
  private final static String SQL_NEXT_STATISTICLOGID = "SELECT nextval('seq_statisticlog')";

  private final StatisticsDatabase database;

  public H2StatisticsStoreImpl(final File dbDir) {
    database = new H2StatisticsDatabase(dbDir);
  }

  public synchronized void open() throws TCStatisticsStoreException {
    try {
      database.open();
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsStoreOpenErrorException("Unexpected error while opening the H2 database.", e);
    }

    install();
    setupPreparedStatements();
  }

  public synchronized void close() throws TCStatisticsStoreException {
    try {
      database.close();
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsStoreCloseErrorException("Unexpected error while closing the H2 database.", e);
    }
  }

  protected void install() throws TCStatisticsStoreException {
    try {
      database.ensureExistingConnection();

      database.getConnection().setAutoCommit(false);

      JdbcHelper.executeUpdate(database.getConnection(),
        "CREATE SEQUENCE IF NOT EXISTS seq_statisticlog");

      JdbcHelper.executeUpdate(database.getConnection(),
        "CREATE TABLE IF NOT EXISTS statisticlog (" +
          "id BIGINT NOT NULL PRIMARY KEY, " +
          "capturesessionid BIGINT NOT NULL, " +
          "agentip VARCHAR(39) NOT NULL, " +
          "moment TIMESTAMP NOT NULL, " +
          "statname VARCHAR(255) NOT NULL," +
          "statelement VARCHAR(255) NULL, " +
          "datanumber BIGINT NULL, " +
          "datatext TEXT NULL, " +
          "datatimestamp TIMESTAMP NULL, " +
          "datadecimal DECIMAL(8, 4) NULL)");

      database.getConnection().commit();
      database.getConnection().setAutoCommit(true);
    } catch (Exception e) {
      throw new TCStatisticsStoreInstallationErrorException("Unable to install the H2 database table structure.", e);
    }
  }

  private void setupPreparedStatements() throws TCStatisticsStoreException {
    try {
      database.createPreparedStatement(SQL_NEXT_STATISTICLOGID);
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsStoreSetupErrorException("Unexpected error while preparing the statements for the H2 statistics store.", e);
    }
  }

  public long storeStatistic(final long sessionId, final StatisticData data) throws TCStatisticsStoreException {
    Assert.assertNotNull("data", data);
    Assert.assertNotNull("agentIp property of data", data.getAgentIp());
    Assert.assertNotNull("data property of data", data.getData());

    try {
      database.ensureExistingConnection();

      // obtain a new ID for the statistic data
      final long id = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_STATISTICLOGID));
      
      // insert the statistic data with the provided values
      final int row_count = JdbcHelper.executeUpdate(database.getConnection(), "INSERT INTO statisticlog (id, capturesessionid, agentip, moment, statname, statelement, datanumber, datatext, datatimestamp, datadecimal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new PreparedStatementHandler() {
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
        throw new TCStatisticsStoreStatisticStorageErrorException("Unexpected error while storing the statistic with id '" + id + "' and data " + data + ".", null);
      }

      return id;
    } catch (Exception e) {
      throw new TCStatisticsStoreStatisticStorageErrorException("Unexpected error while storing the statistic data " + data + ".", e);
    }
  }


  public void retrieveStatistics(final StatisticsRetrievalCriteria criteria, final StatisticsConsumer consumer) throws TCStatisticsStoreException {
    Assert.assertNotNull("criteria", criteria);

    try {
      database.ensureExistingConnection();

      List sql_where = new ArrayList();
      if (criteria.getAgentIp() != null) {
        sql_where.add("agentIp = ?");
      }
      if (criteria.getSessionId() != null) {
        sql_where.add("captureSessionId = ?");
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
      sql.append(" ORDER BY captureSessionId ASC, moment ASC, id ASC");

      JdbcHelper.executeQuery(database.getConnection(), sql.toString(), new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          int param = 1;
          if (criteria.getAgentIp() != null) {
            statement.setString(param++, criteria.getAgentIp());
          }
          if (criteria.getSessionId() != null) {
            statement.setLong(param++, criteria.getSessionId().longValue());
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
            StatisticData data = database.getStatisticsData(resultSet);

            // consume the data
            if (!consumer.consumeStatisticData(resultSet.getLong("captureSessionId"), data)) {
              return;
            }
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TCStatisticsStoreRetrievalErrorException("Unexpected error while retrieving the statistic data for criteria '" + criteria + "'.", e);
    }
  }
}
