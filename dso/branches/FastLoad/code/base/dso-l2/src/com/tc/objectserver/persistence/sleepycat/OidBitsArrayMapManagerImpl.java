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
import com.tc.objectserver.core.api.ManagedObject;
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
  private final static String                  LONGS_PER_MEMORY_ENTRY   = "longsPerMemoryEntry";
  private final static String                  MEASURE_PERF             = "measure.performance";                                // hidden
  private final static String                  CHCKPOINT_CHANGES        = "checkpoint.changes";
  private final static String                  CHCKPOINT_TIMEPERIOD     = "checkpoint.timeperiod";
  private final int                            CHECKPOINT_CHANGES;
  private final int                            CHECKPOINT_PERIOD;
  private boolean                              isMeasurePerf;

  private final Database                       oidDB;
  private final Database                       oidLogDB;
  private final PersistenceTransactionProvider ptp;
  private final OidBitsArrayMap                oidBitsArrayMap;
  private final CursorConfig                   oidDBCursorConfig;
  private volatile boolean                     isPopulating             = false;
  private final int                            BitsPerLong              = OidLongArray.BitsPerLong;
  private final boolean                        paranoid;
  private final AtomicInteger                  changesCount             = new AtomicInteger(0);

  public OidBitsArrayMapManagerImpl(boolean paranoid, Database oidDB, Database oidLogDB,
                                    PersistenceTransactionProvider ptp, CursorConfig oidDBCursorConfig) {
    this.oidDB = oidDB;
    this.oidLogDB = oidLogDB;
    this.paranoid = paranoid;
    this.ptp = ptp;
    this.oidDBCursorConfig = oidDBCursorConfig;

    TCProperties loadObjProp = TCPropertiesImpl.getProperties().getPropertiesFor(LOAD_OBJECTID_PROPERTIES);
    CHECKPOINT_CHANGES = loadObjProp.getInt(CHCKPOINT_CHANGES);
    CHECKPOINT_PERIOD = loadObjProp.getInt(CHCKPOINT_TIMEPERIOD);
    isMeasurePerf = loadObjProp.getBoolean(MEASURE_PERF, false);
    if (!this.paranoid) {
      oidBitsArrayMap = null;
    } else {
      oidBitsArrayMap = new OidBitsArrayMap(loadObjProp.getInt(LONGS_PER_MEMORY_ENTRY), loadObjProp
          .getInt(LONGS_PER_DISK_ENTRY));
    }
  }

  /*
   * A thread to read in ObjectIDs from compressed DB at server restart
   */
  public Thread objectIDReaderThread(SyncObjectIdSet rv) {
    return new Thread(new OidObjectIdReader(rv), "OidObjectIdReaderThread");
  }
  
  private void startCheckpointThread() {
    Thread t = new Thread(new CheckpointRunner(CHECKPOINT_PERIOD), "ObjectIDCheckPoint");
    t.setDaemon(true);
    t.start();
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
   * Flush out oidLogDB to bitsArray on disk. isUpdateArrayInMemory -- true, if processing left over from previous run --
   * false, used by checkpoint to flush in-memory to disk.
   */
  private void oidFlushLogToBitsArray(boolean isUpdateArrayInMemory, boolean isStopped, ObjectIDSet2 tmp) {
    SortedSet<Long> sortedOnDiskOidSet = new TreeSet<Long>();
    PersistenceTransaction tx = ptp.newTransaction();
    CursorConfig dbCursorConfig = new CursorConfig();
    dbCursorConfig.setReadUncommitted(true);
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    int changes = 0;
    try {
      Assert.assertNotNull(pt2nt(tx));
      Cursor cursor = oidLogDB.openCursor(pt2nt(tx), dbCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {

        if (isStopped) {
          abortOnError(tx);
          return;
        }

        long oidValue = Conversion.bytes2Long(key.getData());

        if (isUpdateArrayInMemory) {
          ObjectID objectID = new ObjectID(oidValue);
          byte action = value.getData()[0];
          switch (action) {
            case LOG_ACTION_ADD:
              oidBitsArrayMap.getAndSet(objectID);
              tmp.add(objectID);
              break;
            case LOG_ACTION_DELETE:
              oidBitsArrayMap.getAndClr(objectID);
              tmp.remove(objectID);
              break;
            default:
              throw new RuntimeException("Unknown object log action " + action);
          }
        }

        sortedOnDiskOidSet.add(new Long(oidBitsArrayMap.oidOnDiskIndex(oidValue)));
        cursor.delete();
        ++changes;
      }

      cursor.close();
      resetChangesCount();

      for (Long onDiskIndex : sortedOnDiskOidSet) {
        if (isStopped) {
          abortOnError(tx);
          return;
        }
        OidLongArray bits = oidBitsArrayMap.getArrayForDisk(onDiskIndex);
        key.setData(bits.keyToBytes());
        value.setData(bits.arrayToBytes());
        this.oidDB.put(pt2nt(tx), key, value);
      }

      tx.commit();
      logger.debug("Checkpoint updated " + changes + " objectIDs");
    } catch (DatabaseException e) {
      logger.error("Error ojectID updater " + e);
      abortOnError(tx);
    }
  }

  /*
   * Update bitsArray in memory and disk, add/delete objectIDs on tmp.
   */
  private void processPreviousRunOidLog(boolean isStopped, ObjectIDSet2 tmp) {
    oidFlushLogToBitsArray(true, isStopped, tmp);
  }

  /*
   * Update bitsArray on disk only. In-memory is already up to date.
   */
  private void processCheckpoint(boolean isStopped) {
    oidFlushLogToBitsArray(false, isStopped, null);
  }

  /*
   * Periodically flush oid from oidLogDB to oidDB
   */
  private class CheckpointRunner implements Runnable {
    private final int        timeperiod;
    private volatile boolean isQuit = false;

    public CheckpointRunner(int timeperiod) {
      this.timeperiod = timeperiod;
    }

    public void stop() {
      isQuit = true;
    }

    public void run() {
      while (!isQuit) {
        // Wait for enough changes or specified time-period
        synchronized (changesCount) {
          try {
            changesCount.wait(timeperiod);
          } catch (InterruptedException e) {
            //
          }
        }
        if (isQuit) break;
        processCheckpoint(isQuit);
      }
    }
  }

  /*
   * fast way to load object-Ids at server restart by reading them from bits array
   */
  private class OidObjectIdReader implements Runnable {
    private long                    startTime;
    private int                     counter   = 0;
    private volatile boolean        isStopped = false;
    protected final SyncObjectIdSet set;

    public OidObjectIdReader(SyncObjectIdSet set) {
      this.set = set;
    }

    public void stop() {
      isStopped = true;
    }

    public void run() {
      Assert.assertTrue("Shall be in persistent mode to refresh Object IDs at startup", paranoid);
      if (isMeasurePerf) startTime = System.currentTimeMillis();

      ObjectIDSet2 tmp = new ObjectIDSet2();
      PersistenceTransaction tx = null;
      Cursor cursor = null;
      try {
        isPopulating = true;
        cursor = oidDB.openCursor(pt2nt(tx), oidDBCursorConfig);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          makeObjectIDFromDiskArrayEntry(dbToOidBitsArray(key, value), tmp);
        }

        // process anything left in oidLogDB from previous run
        processPreviousRunOidLog(isStopped, tmp);

        if (isMeasurePerf) {
          logger.info("MeasurePerf: done");
        }
      } catch (Throwable t) {
        logger.error("Error Reading Object IDs", t);
      } finally {
        safeClose(cursor);
        safeCommit(tx);
        isPopulating = false;
        set.stopPopulating(tmp);
        tmp = null;
      }

      startCheckpointThread();
    }

    private void makeObjectIDFromDiskArrayEntry(OidLongArray entry, ObjectIDSet2 tmp) {
      oidBitsArrayMap.applyDiskEntry(entry);
      long oid = entry.getKey();
      long[] ary = entry.getArray();
      for (int j = 0; j < oidBitsArrayMap.longsPerDiskUnit; ++j) {
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

  /*
   * Sync up in-memory bits array from persistor (disk) if exist
   */
  private void syncOidBitsArrayDiskEntry(ObjectID objectId) {
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    key.setData(oidBitsArrayMap.onDiskIndex2Bytes(objectId));
    try {
      if (OperationStatus.SUCCESS.equals(this.oidDB.get(null, key, value, LockMode.DEFAULT))) {
        oidBitsArrayMap.applyDiskEntry(dbToOidBitsArray(key, value));
      }
    } catch (DatabaseException e) {
      logger.error("Reading object ID " + objectId + ":" + e);
    }
  }

  /*
   * Use with great care!!! Shall do db commit before next call otherwise dead lock may result.
   */
  public OperationStatus oidPut(PersistenceTransaction tx, ManagedObject managedObject) throws DatabaseException {
    if (!paranoid) return OperationStatus.SUCCESS;

    // care only new object ID.
    if (!managedObject.isNew()) return OperationStatus.SUCCESS;

    ObjectID objectId = managedObject.getID();
    synchronized (oidBitsArrayMap) {
      if (isPopulating) syncOidBitsArrayDiskEntry(objectId);
      oidBitsArrayMap.getAndSet(objectId);
      return (logAddObjectID(tx, objectId));
    }
  }

  public OperationStatus oidPutAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    if (!paranoid) return OperationStatus.SUCCESS;

    OperationStatus status = OperationStatus.SUCCESS;
    for (ObjectID objectID : oidSet) {
      if (isPopulating) syncOidBitsArrayDiskEntry(objectID);
      oidBitsArrayMap.getAndSet(objectID);
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
      oidBitsArrayMap.getAndClr(objectID);
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

  private OidLongArray dbToOidBitsArray(DatabaseEntry key, DatabaseEntry value) {
    return (new OidLongArray(key.getData(), value.getData()));
  }

  public class OidBitsArrayMap {
    final HashMap       map;
    final int           longsPerMemUnit;
    final int           memBitsLength;
    final int           longsPerDiskUnit;
    final int           diskBitsLength;
    final HashSet<Long> inUseSet;

    OidBitsArrayMap(int longsPerMemUnit, int longsPerDiskUnit) {
      this.longsPerMemUnit = longsPerMemUnit;
      this.memBitsLength = longsPerMemUnit * BitsPerLong;
      this.longsPerDiskUnit = longsPerDiskUnit;
      this.diskBitsLength = longsPerDiskUnit * BitsPerLong;
      map = new HashMap();
      inUseSet = new HashSet<Long>();

      Assert.assertTrue("LongsPerMemUnit must be multiple of LongsPerDiskUnit",
                        (longsPerMemUnit % longsPerDiskUnit) == 0);
    }

    public boolean oidMarkInUse(Set<Long> set) {
      synchronized (this) {
        while (useSetContainsAny(set)) {
          try {
            wait();
          } catch (InterruptedException ex) {
            return false;
          }
        }
        inUseSet.addAll(set);
      }
      return true;
    }

    public void oidUnmarkInUse(Set<Long> set) {
      synchronized (this) {
        inUseSet.removeAll(set);
        notifyAll();
      }
    }

    public boolean useSetContainsAny(Set set) {
      Iterator iter = set.iterator();
      while (iter.hasNext()) {
        if (inUseSet.contains(iter.next())) return true;
      }
      return false;
    }

    public long oidOnDiskIndex(long oid) {
      return (oid / diskBitsLength * diskBitsLength);
    }

    public byte[] onDiskIndex2Bytes(ObjectID id) {
      return Conversion.long2Bytes(oidOnDiskIndex(id.toLong()));
    }

    public Long oidInMemIndex(long oid) {
      return new Long(oid / memBitsLength * memBitsLength);
    }

    private OidLongArray getBitsArray(long oid) {
      Long mapIndex = oidInMemIndex(oid);
      OidLongArray longAry;
      synchronized (map) {
        if (map.containsKey(mapIndex)) {
          longAry = (OidLongArray) map.get(mapIndex);
        } else {
          longAry = new OidLongArray(longsPerMemUnit, mapIndex.longValue());
          map.put(mapIndex, longAry);
        }
      }
      return longAry;
    }

    public OidLongArray getArrayForDisk(long keyOnDisk) {
      OidLongArray longAry = getBitsArray(keyOnDisk);
      return (getArrayForDisk(longAry, keyOnDisk));
    }

    private OidLongArray getArrayForDisk(OidLongArray inMemLongAry, long oid) {
      long keyOnDisk = oidOnDiskIndex(oid);
      OidLongArray onDiskAry = new OidLongArray(longsPerDiskUnit, keyOnDisk);
      int offset = (int) (keyOnDisk % memBitsLength) / BitsPerLong;
      inMemLongAry.copyOut(onDiskAry, offset);
      return onDiskAry;
    }

    private OidLongArray getAndModify(long oid, boolean doSet) {
      OidLongArray longAry = getBitsArray(oid);
      int oidInArray = (int) (oid % memBitsLength);
      synchronized (longAry) {
        if (doSet) {
          longAry.setBit(oidInArray);
        } else {
          longAry.clrBit(oidInArray);
        }

        // purge out array if empty
        /*
         * not thread safe if(!doSet) { if ((value == 0L) && longAry.isZero()) { map.remove(mapIndex); } }
         */
        return (getArrayForDisk(longAry, oid));
      }
    }

    public OidLongArray getAndSet(ObjectID id) {
      return (getAndModify(id.toLong(), true));
    }

    public OidLongArray getAndClr(ObjectID id) {
      return (getAndModify(id.toLong(), false));
    }

    public void applyDiskEntry(OidLongArray entry) {
      OidLongArray inMemArray = getBitsArray(entry.getKey());
      int offset = (int) (entry.getKey() % memBitsLength) / BitsPerLong;
      inMemArray.applyIn(entry, offset);
    }

    public boolean contains(ObjectID id) {
      long oid = id.toLong();
      Long mapIndex = oidInMemIndex(oid);
      synchronized (map) {
        if (map.containsKey(mapIndex)) {
          OidLongArray longAry = (OidLongArray) map.get(mapIndex);
          return (longAry.isSet((int) oid % memBitsLength));
        }
      }
      return (false);
    }

    /*
     * for testing purpose only.
     */
    void reset() {
      map.clear();
    }
  }

  /*
   * for testing purpose. Check if contains specified objectId
   */
  boolean inMemoryContains(ObjectID objectId) {
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
    oidBitsArrayMap.reset();
  }
  
  /*
   * for testing purpose only.
   */
  void runCheckpoint() {
    processCheckpoint(false);
  }

}
