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

public final class FastObjectIDManagerImpl extends SleepycatPersistorBase implements ObjectIDManager {
  private static final TCLogger                logger                = TCLogging
                                                                         .getTestingLogger(FastObjectIDManagerImpl.class);
  private final static int                     SEQUENCE_BATCH_SIZE   = 50000;
  private final static int                     MINIMUM_WAIT_TIME     = 1000;
  private final byte                           PERSIST_COLL          = (byte) 1;
  private final byte                           NOT_PERSIST_COLL      = (byte) 0;
  private final byte                           ADD_OBJECT_ID         = (byte) 0;
  private final byte                           DEL_OBJECT_ID         = (byte) 1;
  // property
  private final int                            checkpointMaxLimit;
  private final int                            checkpointMaxSleep;
  private final int                            longsPerDiskEntry;
  private final int                            longsPerStateEntry;
  private final boolean                        isMeasurePerf;

  private final Database                       oidDB;
  private final Database                       oidStateDB;
  private final Database                       oidLogDB;
  private final PersistenceTransactionProvider ptp;
  private final CursorConfig                   oidDBCursorConfig;
  private final CursorConfig                   oidStateDBCursorConfig;
  private final CheckpointRunner               checkpointThread;
  private final Object                         checkpointSyncObj     = new Object();
  private final Object                         objectIDUpdateSyncObj = new Object();
  private final MutableSequence                sequence;
  private long                                 nextSequence;
  private long                                 endSequence;
  private final long                           firstSequenceThisRun;
  private final OidBitsArrayMap                persistableMap;
  private volatile boolean                     isObjectIDLoadingDone = false;

  public FastObjectIDManagerImpl(DBEnvironment env, PersistenceTransactionProvider ptp, MutableSequence sequence)
      throws TCDatabaseException {
    this.oidDB = env.getOidDatabase();
    this.oidStateDB = env.getOidStateDatabase();
    this.oidLogDB = env.getOidLogDatabase();
    this.ptp = ptp;
    this.oidDBCursorConfig = new CursorConfig();
    this.oidDBCursorConfig.setReadCommitted(true);
    this.oidStateDBCursorConfig = new CursorConfig();
    this.oidStateDBCursorConfig.setReadCommitted(true);

    checkpointMaxLimit = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXLIMIT);
    checkpointMaxSleep = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXSLEEP);
    longsPerDiskEntry = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_LONGS_PERDISKENTRY);
    longsPerStateEntry = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_STATE_LONGS_PERDISKENTRY);
    isMeasurePerf = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_MEASURE_PERF, false);

    this.sequence = sequence;
    nextSequence = this.sequence.nextBatch(SEQUENCE_BATCH_SIZE);
    endSequence = nextSequence + SEQUENCE_BATCH_SIZE;
    firstSequenceThisRun = nextSequence;

    persistableMap = new OidBitsArrayMapImpl(longsPerStateEntry, this.oidStateDB);

    // start checkpoint thread
    checkpointThread = new CheckpointRunner(checkpointMaxSleep);
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

  boolean isReadObjectIDDone() {
    return isObjectIDLoadingDone;
  }

  void setReadObjectIDDone(boolean done) {
    isObjectIDLoadingDone = done;
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
  
  private boolean isLogOfPreviousRun(byte[] logKey) {
    return (firstSequenceThisRun > Conversion.bytes2Long(logKey));
  }

  /*
   * for Mamanged Object Persistent State
   */
  public boolean isPersistMapped(ObjectID id) {
    return persistableMap.contains(id);
  }

  public void setPersistent(ObjectID id) {
    persistableMap.getAndSet(id);
  }

  public void flushPersistentEntryToDisk(PersistenceTransaction tx, ObjectID id) throws DatabaseException {
    persistableMap.updateToDiskEntry(pt2nt(tx), persistableMap.oidIndex(id));
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
    return (status);
  }

  /*
   * Flush out oidLogDB to bitsArray on disk.
   */
  private boolean oidFlushLogToBitsArray(StoppedFlag stoppedFlag, int maxProcessLimit, boolean previousRunLogOnly) {
    boolean isAllFlushed = true;
    synchronized (objectIDUpdateSyncObj) {
      if (stoppedFlag.isStopped()) return isAllFlushed;
      OidBitsArrayMap oidBitsArrayMap = new OidBitsArrayMapImpl(longsPerDiskEntry, this.oidDB);
      SortedSet<Long> sortedOnDiskIndexSet = new TreeSet<Long>();
      SortedSet<Long> sortedStateMapIndexSet = new TreeSet<Long>();
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
              return isAllFlushed;
            }
            
            if (previousRunLogOnly && !isLogOfPreviousRun(key.getData())) {
              break;
            }

            boolean isAddOper = isAddOper(key.getData());
            byte[] oids = value.getData();
            int offset = 0;
            while (offset < oids.length) {
              ObjectID objectID = new ObjectID(Conversion.bytes2Long(oids, offset));
              if (isAddOper) {
                oidBitsArrayMap.getAndSet(objectID);
                // check persistableCollectionType
                if (oids[offset + OidLongArray.BYTES_PER_LONG] == PERSIST_COLL) {
                  // only load entries to be updated
                  persistableMap.getAndSet(objectID);
                  sortedStateMapIndexSet.add(new Long(persistableMap.oidIndex(objectID)));
                }
              } else {
                oidBitsArrayMap.getAndClr(objectID);
                if (oids[offset + OidLongArray.BYTES_PER_LONG] == PERSIST_COLL) {
                  persistableMap.getAndClr(objectID);
                  sortedStateMapIndexSet.add(new Long(persistableMap.oidIndex(objectID)));
                }
              }

              sortedOnDiskIndexSet.add(new Long(oidBitsArrayMap.oidIndex(objectID)));
              offset += OidLongArray.BYTES_PER_LONG + 1;
              ++changes;
            }
            cursor.delete();

            if (maxProcessLimit > 0 && changes >= maxProcessLimit) {
              cursor.close();
              cursor = null;
              isAllFlushed = false;
              break;
            }
          }
        } catch (DatabaseException e) {
          throw e;
        } finally {
          if (cursor != null) cursor.close();
          cursor = null;
        }

        for (Long onDiskIndex : sortedOnDiskIndexSet) {
          if (stoppedFlag.isStopped()) {
            abortOnError(tx);
            return isAllFlushed;
          }
          oidBitsArrayMap.updateToDiskEntry(pt2nt(tx), onDiskIndex);
        }

        for (Long stateIndex : sortedStateMapIndexSet) {
          if (stoppedFlag.isStopped()) {
            abortOnError(tx);
            return isAllFlushed;
          }
          persistableMap.updateToDiskEntry(pt2nt(tx), stateIndex);
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
    return isAllFlushed;
  }

  private void processPreviousRunOidLog() {
    oidFlushLogToBitsArray(new StoppedFlag(), Integer.MAX_VALUE, true);
  }

  private boolean processCheckpoint(StoppedFlag stoppedFlag, int maxProcessLimit) {
    return oidFlushLogToBitsArray(stoppedFlag, maxProcessLimit, false);
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
    private final int         maxSleep;
    private final StoppedFlag stoppedFlag = new StoppedFlag();

    public CheckpointRunner(int maxSleep) {
      super("ObjectID-Checkpoint");
      this.maxSleep = maxSleep;
    }

    public void quit() {
      stoppedFlag.setStopped(true);
    }

    public void run() {
      int currentwait = maxSleep;
      int maxProcessLimit = checkpointMaxLimit;
      while (!stoppedFlag.isStopped()) {
        // Wait for enough changes or specified time-period
        synchronized (checkpointSyncObj) {
          try {
            checkpointSyncObj.wait(currentwait);
          } catch (InterruptedException e) {
            //
          }
        }
        if (stoppedFlag.isStopped()) break;
        // run only after done with ObjectID reading
        if (!isReadObjectIDDone()) continue;
        boolean isAllFlushed = processCheckpoint(stoppedFlag, maxProcessLimit);

        if (isAllFlushed) {
          // All flushed, wait longer for next time
          currentwait += currentwait;
          if (currentwait > maxSleep) {
            currentwait = maxSleep;
            maxProcessLimit = checkpointMaxLimit;
          }
        } else {
          // reduce wait time to catch up
          currentwait = currentwait / 2;
          // at least wait 1 second
          if (currentwait < MINIMUM_WAIT_TIME) {
            currentwait = MINIMUM_WAIT_TIME;
            maxProcessLimit = -1; // unlimited
          }
        }
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
          OidLongArray bitsArray = new OidLongArray(key.getData(), value.getData());
          makeObjectIDFromBitsArray(bitsArray, tmp);
        }
        cursor.close();
        cursor = null;

        cursor = oidStateDB.openCursor(pt2nt(tx), oidStateDBCursorConfig);
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          OidLongArray bitsArray = new OidLongArray(key.getData(), value.getData());
          persistableMap.setMapEntry(bitsArray.getKey(), bitsArray);
        }

        cursor.close();
        cursor = null;
        safeCommit(tx);
        if (isMeasurePerf) {
          logger.info("MeasurePerf: done");
        }
      } catch (Throwable t) {
        logger.error("Error Reading Object IDs", t);
      } finally {
        safeClose(cursor);
        set.stopPopulating(tmp);
        setReadObjectIDDone(true);
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
      setPersistent(mo.getID());
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
      oids[offset + OidLongArray.BYTES_PER_LONG] = isPersistMapped(objectID) ? PERSIST_COLL : NOT_PERSIST_COLL;
      offset += OidLongArray.BYTES_PER_LONG + 1;
    }
    try {
      status = logObjectID(tx, oids, isAdd);
    } catch (DatabaseException de) {
      throw new TCDatabaseException(de);
    }
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
  OidBitsArrayMapImpl loadBitsArrayFromDisk() {
    OidBitsArrayMapImpl oidMap = new OidBitsArrayMapImpl(longsPerDiskEntry, this.oidDB);
    oidMap.loadAllFromDisk();
    return (oidMap);
  }

  /*
   * for testing purpose only. Return all IDs with ObjectID
   */
  Collection bitsArrayMapToObjectID() {
    OidBitsArrayMapImpl oidMap = loadBitsArrayFromDisk();
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
