/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCLongToBytesDatabase;
import com.tc.objectserver.storage.api.TCTransactionStoreDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.stats.counter.sampled.SampledCounter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

class DerbyTCLongToBytesDatabase extends AbstractDerbyTCDatabase implements TCLongToBytesDatabase,
    TCTransactionStoreDatabase {
  private final SampledCounter l2FaultFromDisk;

  private final String         deleteQuery;
  private final String         getQuery;
  private final String         updateQuery;
  private final String         insertQuery;
  private final String         openCursorQuery;

  public DerbyTCLongToBytesDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    this(tableName, connection, queryProvider, SampledCounter.NULL_SAMPLED_COUNTER);
  }

  public DerbyTCLongToBytesDatabase(String tableName, Connection connection, QueryProvider queryProvider,
                                    SampledCounter l2FaultFromDisk) throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    this.l2FaultFromDisk = l2FaultFromDisk;
    deleteQuery = "DELETE FROM " + tableName + " WHERE " + KEY + " = ?";
    getQuery = "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?";
    updateQuery = "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + KEY + " = ?";
    insertQuery = "INSERT INTO " + tableName + " (" + KEY + ", " + VALUE + ") VALUES (?, ?)";
    openCursorQuery = "SELECT " + KEY + ", " + VALUE + " FROM " + tableName;
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createLongToBytesTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public Status delete(long id, PersistenceTransaction tx) {
    try {
      // "DELETE FROM " + tableName + " WHERE " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, deleteQuery);
      psUpdate.setLong(1, id);
      if (psUpdate.executeUpdate() > 0) {
        return Status.SUCCESS;
      } else {
        return Status.NOT_FOUND;
      }
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public byte[] get(long id, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?"
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, getQuery);
      psSelect.setLong(1, id);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return null; }
      byte[] temp = rs.getBytes(1);
      l2FaultFromDisk.increment();
      return temp;
    } catch (SQLException e) {
      throw new DBException("Error retrieving object id: " + id + "; error: " + e.getMessage());
    } finally {
      closeResultSet(rs);
    }
  }

  public Status update(long id, byte[] b, PersistenceTransaction tx) {
    try {
      // "UPDATE " + tableName + " SET " + VALUE + " = ? "
      // + " WHERE " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, updateQuery);
      psUpdate.setBytes(1, b);
      psUpdate.setLong(2, id);
      if (psUpdate.executeUpdate() > 0) { return Status.SUCCESS; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not update with key: " + id);
  }

  public Status insert(long id, byte[] b, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      // "INSERT INTO " + tableName + " VALUES (?, ?)"
      psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setLong(1, id);
      psPut.setBytes(2, b);
      if (psPut.executeUpdate() > 0) { return Status.SUCCESS; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not insert with key: " + id);
  }

  public Status put(long id, byte[] b, PersistenceTransaction tx) {
    if (get(id, tx) == null) {
      return insert(id, b, tx);
    } else {
      return update(id, b, tx);
    }
  }

  public TCDatabaseCursor<Long, byte[]> openCursor(PersistenceTransaction tx) {
    try {
      // "SELECT " + KEY + "," + VALUE + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, openCursorQuery);
      return new DerbyTCLongToBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  static class DerbyTCLongToBytesCursor extends AbstractDerbyTCDatabaseCursor<Long, byte[]> {
    private TCDatabaseEntry<Long, byte[]> entry    = null;
    private boolean                       finished = false;

    public DerbyTCLongToBytesCursor(ResultSet rs) {
      super(rs);
    }

    public boolean hasNext() {
      if (entry != null) { return true; }
      if (finished) { return false; }

      boolean hasNext = false;
      try {
        hasNext = rs.next();
        if (hasNext) {
          entry = new TCDatabaseEntry<Long, byte[]>();
          entry.setKey(rs.getLong(1)).setValue(rs.getBytes(2));
        }
      } catch (SQLException e) {
        throw new DBException(e);
      }

      if (!hasNext) {
        finished = true;
      }
      return hasNext;
    }

    public TCDatabaseEntry<Long, byte[]> next() {
      if (entry == null) {
        if (!hasNext()) { throw new NoSuchElementException("No Element left. Please do hasNext before calling next"); }
      }

      TCDatabaseEntry<Long, byte[]> temp = entry;
      entry = null;
      return temp;
    }
  }
}
