/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.logging.TCLogger;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public final class OidBitsArrayMapManagerImpl extends SleepycatPersistorBase implements OidBitsArrayMapManager {
  private final static byte                    LOG_ACTION_ADD           = 1;
  private final static byte                    LOG_ACTION_DELETE        = 2;
  private final static byte[]                  LOG_DB_VALUE_ADD         = new byte[] { LOG_ACTION_ADD };
  private final static byte[]                  LOG_DB_VALUE_DELETE      = new byte[] { LOG_ACTION_DELETE };

  private final static String                  LOAD_OBJECTID_PROPERTIES = "l2.objectmanager.loadObjectID";
  private final static String                  LONGS_PER_DISK_ENTRY     = "longsPerDiskEntry";
  private final static String                  LONGS_PER_MEMORY_ENTRY   = "longsPerMemoryEntry";
  private final static String                  MEASURE_PERF             = "measure.performance";
  private final static String                  CHCKPOINT_CHANGES        = "checkpoint.changes";
  private final static String                  CHCKPOINT_TIMEPERIOD     = "checkpoint.timeperiod";
  private final int                            CHECKPOINT_CHANGES;
  private final int                            CHECKPOINT_PERIOD;
  private boolean                              isMeasurePerf;
  
  private final Database                       oidDB;
  private final Database                       oidLogDB;
  private final TCLogger                       logger;
  private final PersistenceTransactionProvider ptp;
  private final OidBitsArrayMap                oidBitsArrayMap;
  private final CursorConfig                   oidDBCursorConfig;
  private volatile boolean                     isPopulating;
  private final int                            BitsPerLong              = OidLongArray.BitsPerLong;
  private final boolean                        paranoid;
  private final AtomicInteger                  changesCount             = new AtomicInteger(0);
  private final Thread                         oidCheckPointThread;

  public OidBitsArrayMapManagerImpl(TCLogger logger, boolean paranoid, Database oidDB, Database oidLogDB,
                                    PersistenceTransactionProvider ptp, CursorConfig oidDBCursorConfig) {
    this.oidDB = oidDB;
    this.oidLogDB = oidLogDB;
    this.logger = logger;
    this.paranoid = paranoid;
    this.ptp = ptp;
    this.oidDBCursorConfig = oidDBCursorConfig;

    TCProperties loadObjProp = TCPropertiesImpl.getProperties().getPropertiesFor(LOAD_OBJECTID_PROPERTIES);
    CHECKPOINT_CHANGES = loadObjProp.getInt(CHCKPOINT_CHANGES);
    CHECKPOINT_PERIOD = loadObjProp.getInt(CHCKPOINT_TIMEPERIOD);
    isMeasurePerf = loadObjProp.getBoolean(MEASURE_PERF, false);
    isPopulating = false;
    if (!this.paranoid) {
      oidBitsArrayMap = null;
      oidCheckPointThread = null;
    } else {
      oidBitsArrayMap = new OidBitsArrayMap(loadObjProp.getInt(LONGS_PER_MEMORY_ENTRY), loadObjProp
          .getInt(LONGS_PER_DISK_ENTRY));
      oidCheckPointThread = new Thread(new OidUpdater(CHECKPOINT_PERIOD), "ObjectIDCheckPoint");
      oidCheckPointThread.setDaemon(true);
    }
  }

  private void incChangesCount() {
    if (changesCount.incrementAndGet() >= CHECKPOINT_CHANGES) {
      changesCount.set(0);
      synchronized (oidCheckPointThread) {
        oidCheckPointThread.notifyAll();
      }
    }
  }

  private void resetChangesCount() {
    changesCount.set(0);
  }

  private OperationStatus logObjectID(PersistenceTransaction tx, ObjectID objectID, boolean isAddNewObject)
      throws DatabaseException {
    incChangesCount();
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.long2Bytes(objectID.toLong()));
    DatabaseEntry value = new DatabaseEntry();
    byte[] action = isAddNewObject ? LOG_DB_VALUE_ADD : LOG_DB_VALUE_DELETE;
    value.setData(action);
    return this.oidLogDB.put(pt2nt(tx), key, value);
  }

  private OperationStatus logNewObjectID(PersistenceTransaction tx, ObjectID objectID) throws DatabaseException {
    return (logObjectID(tx, objectID, true));
  }

  private OperationStatus logDeleteObjectID(PersistenceTransaction tx, ObjectID objectID) throws DatabaseException {
    return (logObjectID(tx, objectID, false));
  }

  /*
   * Flush out oidLogDB to bitsArray on disk. isUpdateInMemory - true, if processing left over from previous run -
   * false, used by checkpoint to flush in-memory to disk.
   */
  private void oidFlushLogToBitsArray(boolean isUpdateInMemory) {
    SortedSet<Long> sortedOnDiskOidSet = new TreeSet<Long>();
    PersistenceTransaction tx = ptp.newTransaction();
    CursorConfig dbCursorConfig = new CursorConfig();
    dbCursorConfig.setReadUncommitted(true);
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    try {
      Cursor cursor = oidLogDB.openCursor(pt2nt(tx), dbCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        long oidValue = Conversion.bytes2Long(key.getData());

        if (isUpdateInMemory) {
          byte action = value.getData()[0];
          switch (action) {
            case LOG_ACTION_ADD:
              oidBitsArrayMap.getAndSet(new ObjectID(oidValue));
              break;
            case LOG_ACTION_DELETE:
              oidBitsArrayMap.getAndClr(new ObjectID(oidValue));
              break;
            default:
              throw new RuntimeException("Unknow oid log action " + action);
          }
        }

        sortedOnDiskOidSet.add(new Long(oidBitsArrayMap.oidOnDiskIndex(oidValue)));
        cursor.delete();
      }
      cursor.close();

      for (Long onDiskIndex : sortedOnDiskOidSet) {
        OidLongArray bits = oidBitsArrayMap.getArrayForDisk(onDiskIndex);
        key.setData(bits.keyToBytes());
        value.setData(bits.arrayToBytes());
        this.oidDB.put(pt2nt(tx), key, value);
      }

      tx.commit();
    } catch (DatabaseException e) {
      logger.error("Error ojectID updater " + e);
    }
  }

  /*
   * Periodically flush oid from oidLogDB to oidDB
   */
  private class OidUpdater implements Runnable {
    private final int        timeperiod;
    private volatile boolean quit = false;

    public OidUpdater(int timeperiod) {
      this.timeperiod = timeperiod;
    }

    public void stop() {
      quit = true;
    }

    public void run() {
      while (!quit) {
        synchronized (oidCheckPointThread) {
          try {
            oidCheckPointThread.wait(timeperiod);
          } catch (InterruptedException e) {
            //
          }
          resetChangesCount();
          if (quit) break;
          oidFlushLogToBitsArray(false);
        }
      }
    }
  }

  /*
   * fast way to load object-Ids at server restart by reading them from bits array
   */
  private class OidObjectIdReader implements Runnable {
    protected final SyncObjectIdSet set;

    public OidObjectIdReader(SyncObjectIdSet set) {
      this.set = set;
    }

    public void run() {
      Assert.assertTrue("Shall be in persistent mode to refresh Object IDs at startup", paranoid);

      ObjectIDSet2 tmp = new ObjectIDSet2();
      PersistenceTransaction tx = null;
      Cursor cursor = null;
      Thread helperThread = null;
      try {
        BoundedLinkedQueue queue;
        queue = new BoundedLinkedQueue(1000);
        helperThread = new Thread(new ObjectIdCreator(queue, tmp), "OidObjectIdCreatorThread");
        isPopulating = true;
        helperThread.start();
        tx = ptp.newTransaction();
        cursor = oidDB.openCursor(pt2nt(tx), oidDBCursorConfig);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          queue.put(dbToOidBitsArray(key, value));
        }
        // null data to end helper thread
        queue.put(new OidLongArray(null, null));
        helperThread.join();

        // process anything left in oidLogDB from previous run
        oidFlushLogToBitsArray(true);

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
      oidCheckPointThread.start();
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

  public class ObjectIdCreator implements Runnable {
    ObjectIDSet2       tmp;
    BoundedLinkedQueue queue;
    long               start_time;
    int                counter = 0;

    ObjectIdCreator(BoundedLinkedQueue queue, ObjectIDSet2 tmp) {
      this.queue = queue;
      this.tmp = tmp;
    }

    public void run() {
      if (isMeasurePerf) start_time = new Date().getTime();
      while (true) {
        try {
          OidLongArray entry = (OidLongArray) queue.take();
          if (entry.isEnded()) break;
          process(entry);
        } catch (InterruptedException ex) {
          logger.error("ObjectIdCreator interruptted!");
          break;
        }
      }
    }

    private void process(OidLongArray entry) {
      oidBitsArrayMap.applyOnDiskEntry(entry);
      long oid = entry.getKey();
      long[] ary = entry.getArray();
      for (int j = 0; j < oidBitsArrayMap.longsPerDiskUnit; ++j) {
        long bit = 1L;
        long bits = ary[j];
        for (int i = 0; i < BitsPerLong; ++i) {
          if ((bits & bit) != 0) {
            tmp.add(new ObjectID(oid));
            if (isMeasurePerf) {
              if ((++counter % 1000) == 0) {
                long elapse_time = new Date().getTime() - start_time;
                long avg_time = elapse_time / (counter / 1000);
                logger.info("MeaurePerf: reading " + counter + " OIDs " + "took " + elapse_time + "ms avg(1000 objs):"
                            + avg_time + " ms");
              }
            }
          }
          bit <<= 1;
          ++oid;
        }
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
        oidBitsArrayMap.applyOnDiskEntry(dbToOidBitsArray(key, value));
      }
    } catch (DatabaseException e) {
      logger.warn("Reading object ID " + objectId + ":" + e);
    }
  }

  /*
   * Use with great care!!! Shall do db commit before next call otherwise dead lock may result.
   */
  public OperationStatus oidPut(PersistenceTransaction tx, ManagedObject managedObject) throws DatabaseException {
    if (!paranoid) return OperationStatus.SUCCESS;

    // care only new object ID.
    if (managedObject.isNew()) return OperationStatus.SUCCESS;

    ObjectID objectId = managedObject.getID();
    synchronized (oidBitsArrayMap) {
      if (isPopulating) {
        syncOidBitsArrayDiskEntry(objectId);
      }
      oidBitsArrayMap.getAndSet(objectId);
      return (logNewObjectID(tx, objectId));
    }
  }

  public void oidKeepInSet(Set<ObjectID> set, ObjectID objectId) {
    set.add(objectId);
  }

  /*
   * Update in-memory array and prepare for writing all new object IDs out at end of loop.
   */
  private void processForPut(Set<ObjectID> rawSet, SortedSet<Long> sortedOidSet) {
    Iterator iter = rawSet.iterator();
    synchronized (oidBitsArrayMap) {
      while (iter.hasNext()) {
        ObjectID objectId = (ObjectID) iter.next();
        // care only new object ID.
        if (oidBitsArrayMap.contains(objectId)) continue;

        if (isPopulating) {
          syncOidBitsArrayDiskEntry(objectId);
        }

        oidBitsArrayMap.getAndSet(objectId);
        sortedOidSet.add(new Long(oidBitsArrayMap.oidOnDiskIndex(objectId.toLong())));
      }
    }
  }

  public OperationStatus oidPutAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    if (!paranoid) return OperationStatus.SUCCESS;

    OperationStatus status = OperationStatus.SUCCESS;
    SortedSet sortedOidSet = new TreeSet();
    processForPut(oidSet, sortedOidSet);
    if (!oidBitsArrayMap.oidMarkInUse(sortedOidSet)) { throw new TCDatabaseException("OidBitsArrayMap interrupted"); }
    for (Iterator i = oidSet.iterator(); i.hasNext();) {
      ObjectID objectID = (ObjectID) i.next();
      try {
        status = logNewObjectID(tx, objectID);
      } catch (DatabaseException de) {
        oidBitsArrayMap.oidUnmarkInUse(sortedOidSet);
        throw new TCDatabaseException(de);
      }
      if (!OperationStatus.SUCCESS.equals(status)) break;
    }
    oidBitsArrayMap.oidUnmarkInUse(sortedOidSet);
    return (status);
  }

  /*
   * Update in-memory array and prepare for deleting object IDs out at end of loop.
   */
  private void processForDelete(Set<ObjectID> rawSet, SortedSet<Long> sortedOidSet) {
    Iterator iter = rawSet.iterator();
    synchronized (oidBitsArrayMap) {
      while (iter.hasNext()) {
        ObjectID objectId = (ObjectID) iter.next();
        oidBitsArrayMap.getAndClr(objectId);
        sortedOidSet.add(new Long(oidBitsArrayMap.oidOnDiskIndex(objectId.toLong())));
      }
    }
  }

  public OperationStatus oidDeleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    if (!paranoid) return OperationStatus.SUCCESS;

    OperationStatus status = OperationStatus.SUCCESS;
    SortedSet sortedOidSet = new TreeSet();
    processForDelete(oidSet, sortedOidSet);
    if (!oidBitsArrayMap.oidMarkInUse(sortedOidSet)) { throw new TCDatabaseException("OidBitsArrayMap interrupted"); }
    for (Iterator i = sortedOidSet.iterator(); i.hasNext();) {
      DatabaseEntry key = new DatabaseEntry();
      Long keyOnDisk = (Long) i.next();
      OidLongArray bits = oidBitsArrayMap.getArrayForDisk(keyOnDisk.longValue());
      key.setData(bits.keyToBytes());
      try {
        if (bits.isZero()) {
          status = this.oidDB.delete(pt2nt(tx), key);
          if (!OperationStatus.SUCCESS.equals(status)) {
            oidBitsArrayMap.oidUnmarkInUse(sortedOidSet);
            throw new TCDatabaseException("Delete non-exist on-disk oid array " + bits.getKey());
          }
        } else {
          DatabaseEntry value = new DatabaseEntry();
          value.setData(bits.arrayToBytes());
          status = this.oidDB.put(pt2nt(tx), key, value);
          if (!OperationStatus.SUCCESS.equals(status)) {
            oidBitsArrayMap.oidUnmarkInUse(sortedOidSet);
            throw new TCDatabaseException("Failed to write");
          }
        }
      } catch (DatabaseException de) {
        oidBitsArrayMap.oidUnmarkInUse(sortedOidSet);
        throw new TCDatabaseException(de);
      }
    }
    oidBitsArrayMap.oidUnmarkInUse(sortedOidSet);
    return (status);
  }

  private OidLongArray dbToOidBitsArray(DatabaseEntry key, DatabaseEntry value) {
    return (new OidLongArray(key.getData(), value.getData()));
  }

  /*
   * for testing purpose. Check if contains specified objectId
   */
  public boolean inMemoryContains(ObjectID objectId) {
    return oidBitsArrayMap.contains(objectId);
  }

  /*
   * for testing purpose only. Return all IDs with ObjectID
   */
  public Collection bitsArrayMapToObjectID() {
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
  public void resetBitsArrayMap() {
    oidBitsArrayMap.reset();
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

    public void applyOnDiskEntry(OidLongArray entry) {
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
    public void reset() {
      map.clear();
    }
  }

  public Thread objectIDReaderThread(SyncObjectIdSet rv) {
    return new Thread(new OidObjectIdReader(rv), "OidObjectIdReaderThread");
  }

}
