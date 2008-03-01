/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet2;
import com.tc.util.OidLongArray;
import com.tc.util.SyncObjectIdSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public final class OidBitsArrayMapManagerImpl extends SleepycatPersistorBase implements OidBitsArrayMapManager {
  private static final TCLogger                logger                   = TCLogging
                                                                            .getTestingLogger(OidBitsArrayMapManagerImpl.class);

  private final static byte                    LOG_ACTION_ADD           = 1;
  private final static byte                    LOG_ACTION_DELETE        = 2;
  private final static byte[]                  LOG_DB_VALUE_ADD         = new byte[] { LOG_ACTION_ADD };
  private final static byte[]                  LOG_DB_VALUE_DELETE      = new byte[] { LOG_ACTION_DELETE };

  // property
  private final static String                  LOAD_OBJECTID_PROPERTIES = "l2.objectmanager.loadObjectID";
  private final static String                  LONGS_PER_DISK_ENTRY     = "longsPerDiskEntry";
  private final static String                  MEASURE_PERF             = "measure.performance";                                // hidden
  private final static String                  CHCKPOINT_CHANGES        = "checkpoint.changes";
  private final static String                  CHCKPOINT_TIMEPERIOD     = "checkpoint.timeperiod";
  private final static String                  CHCKPOINT_MAXLIMIT       = "checkpoint.maxlimit";
  private final int                            CHECKPOINT_CHANGES;
  private final int                            CHECKPOINT_MAXLIMIT;
  private final int                            CHECKPOINT_PERIOD;
  private boolean                              isMeasurePerf;

  private final Database                       oidDB;
  private final Database                       oidLogDB;
  private final PersistenceTransactionProvider ptp;
  private final OidBitsArrayMap                oidBitsArrayMap;
  private final CursorConfig                   oidDBCursorConfig;
  private final int                            BitsPerLong              = OidLongArray.BitsPerLong;
  private final boolean                        paranoid;
  private final AtomicInteger                  changesCount             = new AtomicInteger(0);
  private CheckpointRunner                     checkpointThread         = null;

  public OidBitsArrayMapManagerImpl(boolean paranoid, Database oidDB, Database oidLogDB,
                                    PersistenceTransactionProvider ptp, CursorConfig oidDBCursorConfig) {
    this.oidDB = oidDB;
    this.oidLogDB = oidLogDB;
    this.paranoid = paranoid;
    this.ptp = ptp;
    this.oidDBCursorConfig = oidDBCursorConfig;

    TCProperties loadObjProp = TCPropertiesImpl.getProperties().getPropertiesFor(LOAD_OBJECTID_PROPERTIES);
    CHECKPOINT_CHANGES = loadObjProp.getInt(CHCKPOINT_CHANGES);
    CHECKPOINT_MAXLIMIT = loadObjProp.getInt(CHCKPOINT_MAXLIMIT);
    CHECKPOINT_PERIOD = loadObjProp.getInt(CHCKPOINT_TIMEPERIOD);
    isMeasurePerf = loadObjProp.getBoolean(MEASURE_PERF, false);
    oidBitsArrayMap = new OidBitsArrayMap(loadObjProp.getInt(LONGS_PER_DISK_ENTRY), this.oidDB);
  }

  /*
   * A thread to read in ObjectIDs from compressed DB at server restart
   */
  public Thread objectIDReaderThread(SyncObjectIdSet rv) {
    return new Thread(new OidObjectIdReader(rv), "OidObjectIdReaderThread");
  }

  public synchronized void startCheckpointThread() {
    if (checkpointThread != null) return;
    checkpointThread = new CheckpointRunner(CHECKPOINT_PERIOD);
    checkpointThread.setDaemon(true);
    checkpointThread.start();
  }

  public synchronized void stopCheckpointRunner() {
    if (checkpointThread != null) checkpointThread.quit();
  }

  /*
   * changesCount: the amount of changes to trigger checkpoint.
   */
  private void incChangesCount() {
    if (changesCount.incrementAndGet() > CHECKPOINT_CHANGES) {
      synchronized (changesCount) {
        changesCount.notifyAll();
      }
      logger.debug("Checkpoint waked up by " + changesCount.get() + " changes");
      resetChangesCount();
    }
  }

  private void resetChangesCount() {
    changesCount.set(0);
  }

  /*
   * Log the change of an ObjectID, added or deleted. Later, flush to BitArray OidDB by checkpoint thread.
   */
  private OperationStatus logObjectID(PersistenceTransaction tx, ObjectID objectID, boolean isAdd)
      throws DatabaseException {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.long2Bytes(objectID.toLong()));
    DatabaseEntry value = new DatabaseEntry();
    byte[] action = isAdd ? LOG_DB_VALUE_ADD : LOG_DB_VALUE_DELETE;
    value.setData(action);
    OperationStatus rtn = this.oidLogDB.put(pt2nt(tx), key, value);
    incChangesCount();
    return (rtn);
  }

  private OperationStatus logAddObjectID(PersistenceTransaction tx, ObjectID objectID) throws DatabaseException {
    return (logObjectID(tx, objectID, true));
  }

  private OperationStatus logDeleteObjectID(PersistenceTransaction tx, ObjectID objectID) throws DatabaseException {
    return (logObjectID(tx, objectID, false));
  }

  /*
   * Flush out oidLogDB to bitsArray on disk.
   */
  private void oidFlushLogToBitsArray(StoppedFlag stoppedFlag, ObjectIDSet2 tmp, boolean isNoLimit) {
    synchronized (oidBitsArrayMap) {
      if (stoppedFlag.isStopped()) return;
      SortedSet<Long> sortedOnDiskIndexSet = new TreeSet<Long>();
      PersistenceTransaction tx = null;
      try {
        tx = ptp.newTransaction();
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        int changes = 0;
        oidBitsArrayMap.clear();
        Cursor cursor = oidLogDB.openCursor(pt2nt(tx), CursorConfig.READ_COMMITTED);
        try {
          while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {

            if (stoppedFlag.isStopped()) {
              cursor.close();
              cursor = null;
              abortOnError(tx);
              return;
            }

            long oidValue = Conversion.bytes2Long(key.getData());

            ObjectID objectID = new ObjectID(oidValue);
            byte action = value.getData()[0];
            switch (action) {
              case LOG_ACTION_ADD:
                oidBitsArrayMap.getAndSet(objectID);
                if ((tmp != null) && !tmp.add(objectID)) { throw new RuntimeException("Error add " + objectID); }
                break;
              case LOG_ACTION_DELETE:
                oidBitsArrayMap.getAndClr(objectID);
                if ((tmp != null) && !tmp.remove(objectID)) { throw new RuntimeException("Error remove " + objectID); }
                break;
              default:
                throw new RuntimeException("Unknown object log action " + action);
            }

            sortedOnDiskIndexSet.add(new Long(oidBitsArrayMap.oidIndex(oidValue)));
            cursor.delete();

            ++changes;

            if (!isNoLimit && (changes >= CHECKPOINT_MAXLIMIT)) {
              cursor.close();
              cursor = null;
              break;
            }
          }
        } catch (DatabaseException e) {
          throw e;
        } finally {
          if (cursor != null) cursor.close();
          cursor = null;
        }

        resetChangesCount();

        for (Long onDiskIndex : sortedOnDiskIndexSet) {
          if (stoppedFlag.isStopped()) {
            abortOnError(tx);
            return;
          }
          OidLongArray bits = oidBitsArrayMap.getBitsArray(onDiskIndex);
          key.setData(bits.keyToBytes());
          if (!bits.isZero()) {
            value.setData(bits.arrayToBytes());
            if (!OperationStatus.SUCCESS.equals(this.oidDB.put(pt2nt(tx), key, value))) {
              //
              throw new DatabaseException("Failed to update oidDB at " + onDiskIndex);
            }
          } else {
            OperationStatus status = this.oidDB.delete(pt2nt(tx), key);
            if (!OperationStatus.SUCCESS.equals(status) && !OperationStatus.NOTFOUND.equals(status)) {
              //
              throw new DatabaseException("Failed to delete oidDB at " + onDiskIndex);
            }
          }
        }

        tx.commit();
        logger.debug("Checkpoint updated " + changes + " objectIDs");
      } catch (DatabaseException e) {
        logger.error("Error ojectID checkpoint: " + e);
        abortOnError(tx);
      } finally {
        oidBitsArrayMap.clear();
      }
    }
  }

  private void processPreviousRunOidLog(StoppedFlag stoppedFlag, ObjectIDSet2 tmp) {
    oidFlushLogToBitsArray(stoppedFlag, tmp, true);
  }

  private void processCheckpoint(StoppedFlag stoppedFlag) {
    oidFlushLogToBitsArray(stoppedFlag, null, false);
  }

  private static class StoppedFlag {
    private volatile boolean isStopped = false;

    public synchronized boolean isStopped() {
      return isStopped;
    }

    public synchronized void setStopped(boolean stopped) {
      this.isStopped = stopped;
    }
  }

  /*
   * Periodically flush oid from oidLogDB to oidDB
   */
  private class CheckpointRunner extends Thread {
    private final int         timeperiod;
    private final StoppedFlag stoppedFlag = new StoppedFlag();

    public CheckpointRunner(int timeperiod) {
      super("ObjectID-Checkpoint");
      this.timeperiod = timeperiod;
    }

    public void quit() {
      stoppedFlag.setStopped(true);
    }

    public void run() {
      while (!stoppedFlag.isStopped()) {
        // Wait for enough changes or specified time-period
        synchronized (changesCount) {
          try {
            changesCount.wait(timeperiod);
          } catch (InterruptedException e) {
            //
          }
        }
        if (stoppedFlag.isStopped()) break;
        processCheckpoint(stoppedFlag);
      }
    }
  }

  /*
   * fast way to load object-Ids at server restart by reading them from bits array
   */
  private class OidObjectIdReader implements Runnable {
    private long                    startTime;
    private int                     counter     = 0;
    private final StoppedFlag       stoppedFlag = new StoppedFlag();
    protected final SyncObjectIdSet set;

    public OidObjectIdReader(SyncObjectIdSet set) {
      this.set = set;
    }

    public void stop() {
      stoppedFlag.setStopped(true);
    }

    public void run() {
      Assert.assertTrue("Shall be in persistent mode to refresh Object IDs at startup", paranoid);
      if (isMeasurePerf) startTime = System.currentTimeMillis();

      ObjectIDSet2 tmp = new ObjectIDSet2();
      PersistenceTransaction tx = null;
      Cursor cursor = null;
      try {
        cursor = oidDB.openCursor(pt2nt(tx), oidDBCursorConfig);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          OidLongArray bitsArray = new OidLongArray(key.getData(), value.getData());
          makeObjectIDFromBitsArray(bitsArray, tmp);
        }

        // process anything left in oidLogDB from previous run
        processPreviousRunOidLog(stoppedFlag, tmp);

        if (isMeasurePerf) {
          logger.info("MeasurePerf: done");
        }
      } catch (Throwable t) {
        logger.error("Error Reading Object IDs", t);
      } finally {
        safeClose(cursor);
        safeCommit(tx);
        set.stopPopulating(tmp);
        tmp = null;
      }

      startCheckpointThread();
    }

    private void makeObjectIDFromBitsArray(OidLongArray entry, ObjectIDSet2 tmp) {
      long oid = entry.getKey();
      long[] ary = entry.getArray();
      for (int j = 0; j < ary.length; ++j) {
        long bit = 1L;
        long bits = ary[j];
        for (int i = 0; i < BitsPerLong; ++i) {
          if ((bits & bit) != 0) {
            tmp.add(new ObjectID(oid));

            if (isMeasurePerf && ((++counter % 1000) == 0)) {
              long elapse_time = System.currentTimeMillis() - startTime;
              long avg_time = elapse_time / (counter / 1000);
              logger.info("MeasurePerf: reading " + counter + " OIDs took " + elapse_time + "ms avg(1000 objs):"
                          + avg_time + " ms");
            }

          }
          bit <<= 1;
          ++oid;
        }
      }
    }

    protected void safeCommit(PersistenceTransaction tx) {
      if (tx == null) return;
      try {
        tx.commit();
      } catch (Throwable t) {
        logger.error("Error Committing Transaction", t);
      }
    }

    protected void safeClose(Cursor c) {
      if (c == null) return;

      try {
        c.close();
      } catch (Throwable e) {
        logger.error("Error closing cursor", e);
      }
    }
  }

  public OperationStatus oidPut(PersistenceTransaction tx, ObjectID objectID) throws DatabaseException {
    if (!paranoid) return OperationStatus.SUCCESS;

    return (logAddObjectID(tx, objectID));
  }

  public OperationStatus oidPutAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    if (!paranoid) return OperationStatus.SUCCESS;

    OperationStatus status = OperationStatus.SUCCESS;
    for (ObjectID objectID : oidSet) {
      try {
        status = logAddObjectID(tx, objectID);
        incChangesCount();
      } catch (DatabaseException de) {
        throw new TCDatabaseException(de);
      }
      if (!OperationStatus.SUCCESS.equals(status)) break;
    }
    return (status);
  }

  public OperationStatus oidDeleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    if (!paranoid) return OperationStatus.SUCCESS;

    OperationStatus status = OperationStatus.SUCCESS;
    for (ObjectID objectID : oidSet) {
      try {
        status = logDeleteObjectID(tx, objectID);
        incChangesCount();
      } catch (DatabaseException de) {
        throw new TCDatabaseException(de);
      }
      if (!OperationStatus.SUCCESS.equals(status)) break;
    }
    return (status);
  }

  public static class OidBitsArrayMap {
    private final Database oidDB;
    private final HashMap  map;
    private final int      bitsLength;
    private final int      longsPerDiskUnit;

    OidBitsArrayMap(int longsPerDiskUnit, Database oidDB) {
      this.oidDB = oidDB;
      this.longsPerDiskUnit = longsPerDiskUnit;
      this.bitsLength = longsPerDiskUnit * OidLongArray.BitsPerLong;
      map = new HashMap();
    }

    public void clear() {
      map.clear();
    }

    private Long oidIndex(long oid) {
      return new Long(oid / bitsLength * bitsLength);
    }

    public OidLongArray getBitsArray(long oid) {
      Long mapIndex = oidIndex(oid);
      OidLongArray longAry = null;
      synchronized (map) {
        if (map.containsKey(mapIndex)) {
          longAry = (OidLongArray) map.get(mapIndex);
        }
      }
      return longAry;
    }

    private OidLongArray getOrLoadBitsArray(long oid) {
      Long mapIndex = oidIndex(oid);
      OidLongArray longAry;
      synchronized (map) {
        if (map.containsKey(mapIndex)) {
          longAry = (OidLongArray) map.get(mapIndex);
        } else {
          longAry = readDiskEntry(oid);
          if (longAry == null) longAry = new OidLongArray(longsPerDiskUnit, mapIndex.longValue());
          map.put(mapIndex, longAry);
        }
      }
      return longAry;
    }

    private OidLongArray readDiskEntry(long oid) {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      key.setData(Conversion.long2Bytes(oidIndex(oid)));
      try {
        OperationStatus status = oidDB.get(null, key, value, LockMode.DEFAULT);
        if (OperationStatus.SUCCESS.equals(status)) { return new OidLongArray(key.getData(), value.getData()); }
      } catch (DatabaseException e) {
        logger.error("Reading object ID " + oid + ":" + e);
      }
      return null;
    }

    private OidLongArray getAndModify(long oid, boolean doSet) {
      OidLongArray longAry = getOrLoadBitsArray(oid);
      int oidInArray = (int) (oid % bitsLength);
      synchronized (longAry) {
        if (doSet) {
          longAry.setBit(oidInArray);
        } else {
          longAry.clrBit(oidInArray);
        }
      }
      return (longAry);
    }

    public OidLongArray getAndSet(ObjectID id) {
      return (getAndModify(id.toLong(), true));
    }

    public OidLongArray getAndClr(ObjectID id) {
      return (getAndModify(id.toLong(), false));
    }

    // for testing
    void loadBitsArrayFromDisk() {
      clear();
      Cursor cursor = null;
      try {
        cursor = oidDB.openCursor(null, CursorConfig.READ_COMMITTED);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          OidLongArray bitsArray = new OidLongArray(key.getData(), value.getData());
          map.put(new Long(bitsArray.getKey()), bitsArray);
        }
        cursor.close();
        cursor = null;
      } catch (DatabaseException e) {
        throw new RuntimeException(e);
      } finally {
        try {
          if (cursor != null) cursor.close();
        } catch (DatabaseException e) {
          throw new RuntimeException(e);
        }
      }
    }

    // for testing
    boolean contains(ObjectID id) {
      long oid = id.toLong();
      Long mapIndex = oidIndex(oid);
      synchronized (map) {
        if (map.containsKey(mapIndex)) {
          OidLongArray longAry = (OidLongArray) map.get(mapIndex);
          return (longAry.isSet((int) oid % bitsLength));
        }
      }
      return (false);
    }
  }

  /*
   * for testing purpose. Load bitsArray from disk
   */
  void loadBitsArrayFromDisk() {
    oidBitsArrayMap.loadBitsArrayFromDisk();
  }

  /*
   * for testing purpose. Check if contains specified objectId
   */
  boolean contains(ObjectID objectId) {
    return oidBitsArrayMap.contains(objectId);
  }

  /*
   * for testing purpose only. Return all IDs with ObjectID
   */
  Collection bitsArrayMapToObjectID() {
    HashSet objectIDs = new HashSet();
    for (Iterator i = oidBitsArrayMap.map.keySet().iterator(); i.hasNext();) {
      long oid = ((Long) i.next()).longValue();
      OidLongArray bits = oidBitsArrayMap.getBitsArray(oid);
      for (int offset = 0; offset < bits.totalBits(); ++offset) {
        if (bits.isSet(offset)) {
          Assert.assertTrue("Same object ID represented by different bits in memory", objectIDs
              .add(new ObjectID(oid + offset)));
        }
      }
    }
    return (objectIDs);
  }

  /*
   * for testing purpose only.
   */
  void resetBitsArrayMap() {
    oidBitsArrayMap.clear();
  }

  /*
   * for testing purpose only.
   */
  void runCheckpoint() {
    processPreviousRunOidLog(new StoppedFlag(), null);
  }

}
