/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet;
import com.tc.util.OidLongArray;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.sequence.MutableSequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public final class FastObjectIDManagerImpl extends SleepycatPersistorBase implements ObjectIDManager {
  private static final TCLogger                logger                = TCLogging
                                                                         .getTestingLogger(FastObjectIDManagerImpl.class);
  private final static int                     SEQUENCE_BATCH_SIZE   = 50000;
  private final byte                           PERSIST_COLL          = (byte) 1;
  private final byte                           NOT_PERSIST_COLL      = (byte) 0;
  private final byte                           ADD_OBJECT_ID         = (byte) 0;
  private final byte                           DEL_OBJECT_ID         = (byte) 1;
  private final int                            AUXDB_KEY             = 1;
  // property
  private final int                            checkpointChanges;
  private final int                            checkpointMaxLimit;
  private final int                            checkpointPeriod;
  private final int                            longsPerDiskEntry;
  private final boolean                        isMeasurePerf;

  private final Database                       oidDB;
  private final Database                       oidLogDB;
  private final PersistenceTransactionProvider ptp;
  private final CursorConfig                   oidDBCursorConfig;
  private final AtomicInteger                  changesCount          = new AtomicInteger(0);
  private final CheckpointRunner               checkpointThread;
  private final Object                         checkpointSyncObj     = new Object();
  private final Object                         objectIDUpdateSyncObj = new Object();
  private final MutableSequence                sequence;
  private final ObjectIDPersistentMapInfo      objectIDPersistentMapInfo;
  private long                                 nextSequence;
  private long                                 endSequence;

  public FastObjectIDManagerImpl(Database oidDB, Database oidLogDB, PersistenceTransactionProvider ptp,
                                 CursorConfig oidDBCursorConfig, MutableSequence sequence,
                                 ObjectIDPersistentMapInfo objectIDPersistentMapInfo) {
    this.oidDB = oidDB;
    this.oidLogDB = oidLogDB;
    this.ptp = ptp;
    this.oidDBCursorConfig = oidDBCursorConfig;

    checkpointChanges = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_CHANGES);
    checkpointMaxLimit = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXLIMIT);
    checkpointPeriod = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_TIMEPERIOD);
    longsPerDiskEntry = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_LONGS_PERDISKENTRY);
    isMeasurePerf = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_MEASURE_PERF, false);

    this.sequence = sequence;
    nextSequence = this.sequence.nextBatch(SEQUENCE_BATCH_SIZE);
    endSequence = nextSequence + SEQUENCE_BATCH_SIZE;
    this.objectIDPersistentMapInfo = objectIDPersistentMapInfo;

    // start checkpoint thread
    checkpointThread = new CheckpointRunner(checkpointPeriod);
    checkpointThread.setDaemon(true);
    checkpointThread.start();
  }

  /*
   * A thread to read in ObjectIDs from compressed DB at server restart
   */
  public Runnable getObjectIDReader(SyncObjectIdSet rv) {
    return new OidObjectIdReader(rv);
  }

  public void stopCheckpointRunner() {
    checkpointThread.quit();
  }

  /*
   * changesCount: the amount of changes to trigger checkpoint.
   */
  private void incChangesCount(int n) {
    if (changesCount.addAndGet(n) > checkpointChanges) {
      synchronized (checkpointSyncObj) {
        checkpointSyncObj.notifyAll();
      }
      logger.debug("Checkpoint waked up by " + changesCount.get() + " changes");
      resetChangesCount();
    }
  }

  private void incChangesCount() {
    incChangesCount(1);
  }

  private void resetChangesCount() {
    changesCount.set(0);
  }

  private synchronized long nextSeqID() {
    if (nextSequence == endSequence) {
      nextSequence = this.sequence.nextBatch(SEQUENCE_BATCH_SIZE);
      endSequence = nextSequence + SEQUENCE_BATCH_SIZE;
    }
    return (nextSequence++);
  }

  /*
   * Log key to make log records ordered in time sequenece
   */
  private byte[] makeLogKey(boolean isAdd) {
    byte[] rv = new byte[OidLongArray.BYTES_PER_LONG + 1];
    Conversion.writeLong(nextSeqID(), rv, 0);
    rv[OidLongArray.BYTES_PER_LONG] = isAdd ? ADD_OBJECT_ID : DEL_OBJECT_ID;
    return rv;
  }

  /*
   * Log ObjectID+isPersistableCollectionType in db value
   */
  private byte[] makeLogValue(ManagedObject mo) {
    byte[] rv = new byte[OidLongArray.BYTES_PER_LONG + 1];
    Conversion.writeLong(mo.getID().toLong(), rv, 0);
    rv[OidLongArray.BYTES_PER_LONG] = isPersistableCollection(mo) ? PERSIST_COLL : NOT_PERSIST_COLL;
    return rv;
  }

  private boolean isPersistableCollection(ManagedObject mo) {
    return PersistentCollectionsUtil.isPersistableCollectionType(mo.getManagedObjectState().getType());
  }

  private boolean isAddOper(byte[] logKey) {
    return (logKey[OidLongArray.BYTES_PER_LONG] == ADD_OBJECT_ID);
  }

  /*
   * Log the change of an ObjectID, added or deleted. Later, flush to BitsArray OidDB by checkpoint thread.
   */
  private OperationStatus logObjectID(PersistenceTransaction tx, byte[] oids, boolean isAdd) throws DatabaseException {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(makeLogKey(isAdd));
    DatabaseEntry value = new DatabaseEntry();
    value.setData(oids);
    OperationStatus rtn = this.oidLogDB.putNoOverwrite(pt2nt(tx), key, value);
    return (rtn);
  }

  private OperationStatus logAddObjectID(PersistenceTransaction tx, ManagedObject mo) throws DatabaseException {
    OperationStatus status = logObjectID(tx, makeLogValue(mo), true);
    if (OperationStatus.SUCCESS.equals(status)) incChangesCount();
    return (status);
  }

  /*
   * Flush out oidLogDB to bitsArray on disk.
   */
  private void oidFlushLogToBitsArray(StoppedFlag stoppedFlag, boolean isNoLimit) {
    synchronized (objectIDUpdateSyncObj) {
      if (stoppedFlag.isStopped()) return;
      OidBitsArrayMap oidBitsArrayMap = new OidBitsArrayMap(longsPerDiskEntry, this.oidDB);
      OidBitsArrayMap persistableMap = new OidBitsArrayMap(longsPerDiskEntry, this.oidDB, AUXDB_KEY);
      SortedSet<Long> sortedOnDiskIndexSet = new TreeSet<Long>();
      PersistenceTransaction tx = null;
      try {
        tx = ptp.newTransaction();
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        int changes = 0;
        Cursor cursor = oidLogDB.openCursor(pt2nt(tx), CursorConfig.READ_COMMITTED);
        try {
          while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {

            if (stoppedFlag.isStopped()) {
              cursor.close();
              cursor = null;
              abortOnError(tx);
              return;
            }

            boolean isAddOper = isAddOper(key.getData());
            byte[] oids = value.getData();
            int offset = 0;
            while (offset < oids.length) {
              long oidValue = Conversion.bytes2Long(oids, offset);
              ObjectID objectID = new ObjectID(oidValue);
              if (isAddOper) {
                oidBitsArrayMap.getAndSet(objectID);
                // check persistableCollectionType
                if (oids[offset + OidLongArray.BYTES_PER_LONG] == PERSIST_COLL) {
                  // only load entries to be updated
                  persistableMap.getAndSet(objectID);
                }
              } else {
                oidBitsArrayMap.getAndClr(objectID);
                if (persistableMap.contains(objectID)) {
                  persistableMap.getAndClr(objectID);
                }
              }
              sortedOnDiskIndexSet.add(new Long(oidBitsArrayMap.oidIndex(oidValue)));
              offset += OidLongArray.BYTES_PER_LONG + 1;
              ++changes;
            }
            cursor.delete();

            if (!isNoLimit && (changes >= checkpointMaxLimit)) {
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
          oidBitsArrayMap.writeDiskEntry(pt2nt(tx), bits);

          OidLongArray auxBits = persistableMap.getBitsArray(onDiskIndex);
          if (auxBits != null) {
            persistableMap.writeDiskEntry(pt2nt(tx), auxBits);
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

  private void processPreviousRunOidLog() {
    oidFlushLogToBitsArray(new StoppedFlag(), true);
  }

  private void processCheckpoint(StoppedFlag stoppedFlag) {
    oidFlushLogToBitsArray(stoppedFlag, false);
  }

  private static class StoppedFlag {
    private volatile boolean isStopped = false;

    public boolean isStopped() {
      return isStopped;
    }

    public void setStopped(boolean stopped) {
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
        synchronized (checkpointSyncObj) {
          try {
            checkpointSyncObj.wait(timeperiod);
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

    private boolean isPersistableCollectionInfo(long index) {
      int bitsLength = longsPerDiskEntry * OidLongArray.BITS_PER_LONG;
      return ((index % bitsLength) == AUXDB_KEY);
    }

    public void run() {
      if (isMeasurePerf) startTime = System.currentTimeMillis();

      // process left over from previous run
      processPreviousRunOidLog();

      ObjectIDSet tmp = new ObjectIDSet();
      PersistenceTransaction tx = null;
      Cursor cursor = null;
      try {
        cursor = oidDB.openCursor(pt2nt(tx), oidDBCursorConfig);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          long index = Conversion.bytes2Long(key.getData());
          OidLongArray bitsArray = new OidLongArray(key.getData(), value.getData());
          if (!isPersistableCollectionInfo(index)) {
            makeObjectIDFromBitsArray(bitsArray, tmp);
          } else {
            // persistentCollectionInfo records
            setPersistableFromBitsArray(bitsArray);
          }
        }

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
    }

    private void makeObjectIDFromBitsArray(OidLongArray entry, ObjectIDSet tmp) {
      long oid = entry.getKey();
      long[] ary = entry.getArray();
      for (int j = 0; j < ary.length; ++j) {
        long bit = 1L;
        long bits = ary[j];
        for (int i = 0; i < OidLongArray.BITS_PER_LONG; ++i) {
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

    private void setPersistableFromBitsArray(OidLongArray entry) {
      long oid = entry.getKey();
      long[] ary = entry.getArray();
      for (int j = 0; j < ary.length; ++j) {
        long bit = 1L;
        long bits = ary[j];
        for (int i = 0; i < OidLongArray.BITS_PER_LONG; ++i) {
          if ((bits & bit) != 0) {
            // set bit
            objectIDPersistentMapInfo.setPersistent(new ObjectID(oid));
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

  public OperationStatus put(PersistenceTransaction tx, ManagedObject mo) throws DatabaseException {
    return (logAddObjectID(tx, mo));
  }

  public void prePutAll(Set<ObjectID> oidSet, ManagedObject mo) {
    if (isPersistableCollection(mo)) {
      objectIDPersistentMapInfo.setPersistent(mo.getID());
    }
    oidSet.add(mo.getID());
  }

  private OperationStatus doAll(PersistenceTransaction tx, Set<ObjectID> oidSet, boolean isAdd)
      throws TCDatabaseException {
    OperationStatus status = OperationStatus.SUCCESS;
    int size = oidSet.size();
    if (size == 0) return (status);

    byte[] oids = new byte[size * (OidLongArray.BYTES_PER_LONG + 1)];
    int offset = 0;
    for (ObjectID objectID : oidSet) {
      Conversion.writeLong(objectID.toLong(), oids, offset);
      oids[offset + OidLongArray.BYTES_PER_LONG] = objectIDPersistentMapInfo.isPersistMapped(objectID) ? PERSIST_COLL
          : NOT_PERSIST_COLL;
      offset += OidLongArray.BYTES_PER_LONG + 1;
    }
    try {
      status = logObjectID(tx, oids, isAdd);
    } catch (DatabaseException de) {
      throw new TCDatabaseException(de);
    }
    incChangesCount(size);
    return (status);
  }

  public OperationStatus putAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    return (doAll(tx, oidSet, true));
  }

  public OperationStatus deleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) throws TCDatabaseException {
    return (doAll(tx, oidSet, false));
  }

  /*
   * for testing purpose. Load bitsArray from disk
   */
  OidBitsArrayMap loadBitsArrayFromDisk() {
    OidBitsArrayMap oidMap = new OidBitsArrayMap(longsPerDiskEntry, this.oidDB);
    oidMap.loadAllFromDisk();
    return (oidMap);
  }

  /*
   * for testing purpose only. Return all IDs with ObjectID
   */
  Collection bitsArrayMapToObjectID() {
    OidBitsArrayMap oidMap = loadBitsArrayFromDisk();
    HashSet objectIDs = new HashSet();
    for (Iterator i = oidMap.getMapKeySet().iterator(); i.hasNext();) {
      long oid = ((Long) i.next()).longValue();
      OidLongArray bits = oidMap.getBitsArray(oid);
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
  void runCheckpoint() {
    processPreviousRunOidLog();
  }

}
