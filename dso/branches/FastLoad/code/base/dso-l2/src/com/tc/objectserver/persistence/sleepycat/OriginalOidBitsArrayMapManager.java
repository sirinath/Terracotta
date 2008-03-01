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
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet2;
import com.tc.util.SyncObjectIdSet;

import java.util.Set;

public class OriginalOidBitsArrayMapManager extends SleepycatPersistorBase implements OidBitsArrayMapManager {
  private static final TCLogger                logger = TCLogging
                                                          .getTestingLogger(OriginalOidBitsArrayMapManager.class);

  private final Database                       objectDB;
  private final PersistenceTransactionProvider ptp;
  private final CursorConfig                   dBCursorConfig;
  private final boolean                        paranoid;

  public OriginalOidBitsArrayMapManager(boolean paranoid, Database objectDB,
                                        PersistenceTransactionProvider ptp, CursorConfig dBCursorConfig) {
    this.objectDB = objectDB;
    this.paranoid = paranoid;
    this.ptp = ptp;
    this.dBCursorConfig = dBCursorConfig;
  }

  public Thread objectIDReaderThread(SyncObjectIdSet rv) {
    return new Thread(new ObjectIdReader(rv), "ObjectIdReaderThread");
  }

  public OperationStatus oidDeleteAll(PersistenceTransaction tx, Set<ObjectID> oidSet) {
    return OperationStatus.SUCCESS;
  }

  public OperationStatus oidPut(PersistenceTransaction tx, ObjectID objectID) {
    return OperationStatus.SUCCESS;
  }

  public OperationStatus oidPutAll(PersistenceTransaction tx, Set<ObjectID> oidSet) {
    return OperationStatus.SUCCESS;
  }

  /*
   * the old/slow reading object-Ids at server restart
   */
  private class ObjectIdReader implements Runnable {
    protected final SyncObjectIdSet set;

    public ObjectIdReader(SyncObjectIdSet set) {
      this.set = set;
    }

    public void run() {
      Assert.assertTrue("Shall be in persistent mode to refresh Object IDs at startup", paranoid);

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
