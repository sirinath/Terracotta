/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
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
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet2;
import com.tc.util.SyncObjectIdSet;

import java.util.Set;

public class PlainObjectIDManagerImpl extends SleepycatPersistorBase implements ObjectIDManager {
  private static final TCLogger                logger = TCLogging.getTestingLogger(PlainObjectIDManagerImpl.class);

  private final Database                       objectDB;
  private final PersistenceTransactionProvider ptp;
  private final CursorConfig                   dBCursorConfig;
  private final boolean                        isMeasurePerf;

  public PlainObjectIDManagerImpl(Database objectDB, PersistenceTransactionProvider ptp,
                                  CursorConfig dBCursorConfig) {
    this.objectDB = objectDB;
    this.ptp = ptp;
    this.dBCursorConfig = dBCursorConfig;
    
    TCProperties loadObjProp = TCPropertiesImpl.getProperties().getPropertiesFor(FastObjectIDManagerImpl.LOAD_OBJECTID_PROPERTIES);
    isMeasurePerf = loadObjProp.getBoolean(FastObjectIDManagerImpl.MEASURE_PERF, false);
  }

  public Runnable getObjectIDReader(SyncObjectIdSet rv) {
    return new ObjectIdReader(rv);
  }

  public OperationStatus deleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) {
    return OperationStatus.SUCCESS;
  }

  public OperationStatus put(PersistenceTransaction tx, ObjectID objectID) {
    return OperationStatus.SUCCESS;
  }

  public void prePutAll(Set<ObjectID> oidSet, ObjectID objectID) {
    return;
  }

  public OperationStatus putAll(PersistenceTransaction tx, Set<ObjectID> oidSet) {
    return OperationStatus.SUCCESS;
  }

  /*
   * the old/slow reading object-Ids at server restart
   */
  private class ObjectIdReader implements Runnable {
    protected final SyncObjectIdSet set;
    long startTime;

    public ObjectIdReader(SyncObjectIdSet set) {
      this.set = set;
    }

    public void run() {
      if (isMeasurePerf) startTime = System.currentTimeMillis();
      int counter = 0;
      ObjectIDSet2 tmp = new ObjectIDSet2();
      PersistenceTransaction tx = null;
      Cursor cursor = null;
      try {
        tx = ptp.newTransaction();
        cursor = objectDB.openCursor(pt2nt(tx), dBCursorConfig);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          tmp.add(new ObjectID(Conversion.bytes2Long(key.getData())));
          if (isMeasurePerf && ((++counter % 1000) == 0)) {
            long elapse_time = System.currentTimeMillis() - startTime;
            long avg_time = elapse_time / (counter / 1000);
            logger.info("MeasurePerf: reading " + counter + " OIDs took " + elapse_time + "ms avg(1000 objs):"
                        + avg_time + " ms");
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

}
