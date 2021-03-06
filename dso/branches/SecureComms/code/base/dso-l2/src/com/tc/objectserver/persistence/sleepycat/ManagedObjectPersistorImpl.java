/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.util.NullStatsRecorder;
import com.tc.statistics.util.StatsPrinter;
import com.tc.statistics.util.StatsRecorder;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.NullSyncObjectIdSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.SyncObjectIdSetImpl;
import com.tc.util.sequence.MutableSequence;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class ManagedObjectPersistorImpl extends SleepycatPersistorBase implements ManagedObjectPersistor,
    PrettyPrintable {

  private static final Comparator              MO_COMPARATOR         = new Comparator() {
                                                                       public int compare(Object o1, Object o2) {
                                                                         long oid1 = ((ManagedObject) o1).getID()
                                                                             .toLong();
                                                                         long oid2 = ((ManagedObject) o2).getID()
                                                                             .toLong();
                                                                         if (oid1 < oid2) {
                                                                           return -1;
                                                                         } else if (oid1 > oid2) {
                                                                           return 1;
                                                                         } else {
                                                                           return 0;
                                                                         }
                                                                       }
                                                                     };

  private static final Object                  MO_PERSISTOR_KEY      = ManagedObjectPersistorImpl.class.getName()
                                                                       + ".saveAllObjects";
  private static final Object                  MO_PERSISTOR_VALUE    = "Complete";

  private static final boolean                 STATS_LOGGING_ENABLED = TCPropertiesImpl
                                                                         .getProperties()
                                                                         .getBoolean(
                                                                                     TCPropertiesConsts.L2_OBJECTMANAGER_PERSISTOR_LOGGING_ENABLED);
  private static final boolean                 MEASURE_PERF          = TCPropertiesImpl
                                                                         .getProperties()
                                                                         .getBoolean(
                                                                                     TCPropertiesConsts.L2_OBJECTMANAGER_PERSISTOR_MEASURE_PERF,
                                                                                     false);

  private final Database                       objectDB;
  private final SerializationAdapterFactory    saf;
  private final MutableSequence                objectIDSequence;
  private final Database                       rootDB;
  private final CursorConfig                   rootDBCursorConfig;
  private long                                 saveCount;
  private final TCLogger                       logger;
  private final PersistenceTransactionProvider ptp;
  private final ClassCatalog                   classCatalog;
  private SerializationAdapter                 serializationAdapter;
  private final SleepycatCollectionsPersistor  collectionsPersistor;
  private final ObjectIDManager                objectIDManager;
  private final SyncObjectIdSet                extantObjectIDs;
  private final SyncObjectIdSet                extantMapTypeOidSet;
  private final StatsRecorder                  commitStats;
  private final StatsRecorder                  perfMeasureStats;

  public ManagedObjectPersistorImpl(TCLogger logger, ClassCatalog classCatalog,
                                    SerializationAdapterFactory serializationAdapterFactory, DBEnvironment env,
                                    MutableSequence objectIDSequence, Database rootDB, CursorConfig rootDBCursorConfig,
                                    PersistenceTransactionProvider ptp,
                                    SleepycatCollectionsPersistor collectionsPersistor, boolean paranoid)
      throws TCDatabaseException {
    this.logger = logger;
    this.classCatalog = classCatalog;
    this.saf = serializationAdapterFactory;
    this.objectDB = env.getObjectDatabase();
    this.objectIDSequence = objectIDSequence;
    this.rootDB = rootDB;
    this.rootDBCursorConfig = rootDBCursorConfig;
    this.ptp = ptp;
    this.collectionsPersistor = collectionsPersistor;

    boolean oidFastLoad = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_FASTLOAD);
    if (!paranoid) {
      this.objectIDManager = new NullObjectIDManager();
    } else if (oidFastLoad) {
      // read objectIDs from compressed DB
      MutableSequence sequence = new SleepycatSequence(this.ptp, logger,
                                                       SleepycatSequenceKeys.OID_STORE_LOG_SEQUENCE_DB_NAME, 1000, env
                                                           .getGlobalSequenceDatabase());
      this.objectIDManager = new FastObjectIDManagerImpl(env, ptp, sequence, this);
    } else {
      // read objectIDs from object DB
      this.objectIDManager = new PlainObjectIDManagerImpl(objectDB, ptp);
    }

    this.extantObjectIDs = getAllObjectIDs();
    this.extantMapTypeOidSet = getAllMapsObjectIDs();

    if (STATS_LOGGING_ENABLED) {
      commitStats = new StatsPrinter("MO Commit Stats Printer", 5000, new MessageFormat("Commits in the Last {0} ms"),
                                     new MessageFormat(
                                     // hate this stupid formatter, can't figure how to prefix with space
                                     // " count = {0,number,000000}   bytes = {1,number,0000000}   new = {2,number, 0000}"
                                                       " count = {0}   bytes = {1}   new = {2}"), true);
    } else {
      commitStats = new NullStatsRecorder();
    }
    if (MEASURE_PERF) {
      perfMeasureStats = new StatsPrinter(
                                          "MO Delete Stats Printer",
                                          60000,
                                          new MessageFormat("Deletes in the Last {0} ms"),
                                          new MessageFormat(
                                          // " count = {0,number,#}   collections mo state = {1,number,#}   time taken = {2,number, #}"
                                                            " total count = {0}   collections mo state = {1}   time taken = {2} nanos"),
                                          false);
    } else {
      perfMeasureStats = new NullStatsRecorder();
    }
  }

  public int getObjectCount() {
    return extantObjectIDs.size();
  }

  public boolean addNewObject(ObjectID id) {
    return extantObjectIDs.add(id);
  }

  public boolean containsObject(ObjectID id) {
    return extantObjectIDs.contains(id);
  }

  public void removeAllObjectsByID(SortedSet<ObjectID> ids) {
    this.extantObjectIDs.removeAll(ids);
  }

  public ObjectIDSet snapshotObjects() {
    return this.extantObjectIDs.snapshot();
  }

  public boolean containsMapType(ObjectID id) {
    return extantMapTypeOidSet.contains(id);
  }

  public boolean addMapTypeObject(ObjectID id) {
    return extantMapTypeOidSet.add(id);
  }

  public void removeAllMapTypeObject(Collection ids) {
    extantMapTypeOidSet.removeAll(ids);
  }

  public long nextObjectIDBatch(int batchSize) {
    return objectIDSequence.nextBatch(batchSize);
  }

  public void setNextAvailableObjectID(long startID) {
    objectIDSequence.setNext(startID);
  }

  public void addRoot(PersistenceTransaction tx, String name, ObjectID id) {
    validateID(id);
    OperationStatus status = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      setStringData(key, name);
      setObjectIDData(value, id);

      status = this.rootDB.put(pt2nt(tx), key, value);
    } catch (Throwable t) {
      throw new DBException(t);
    }
    if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException("Unable to write root id: " + name + "=" + id
                                                                         + "; status: " + status); }
  }

  public ObjectID loadRootID(String name) {
    if (name == null) throw new AssertionError("Attempt to retrieve a null root name");
    OperationStatus status = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      setStringData(key, name);
      PersistenceTransaction tx = ptp.newTransaction();
      status = this.rootDB.get(pt2nt(tx), key, value, LockMode.DEFAULT);
      tx.commit();
      if (OperationStatus.SUCCESS.equals(status)) {
        ObjectID rv = getObjectIDData(value);
        return rv;
      }
    } catch (Throwable t) {
      throw new DBException(t);
    }
    if (OperationStatus.NOTFOUND.equals(status)) return ObjectID.NULL_ID;
    else throw new DBException("Error retrieving root: " + name + "; status: " + status);
  }

  public Set loadRoots() {
    Set rv = new HashSet();
    Cursor cursor = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      PersistenceTransaction tx = ptp.newTransaction();
      cursor = rootDB.openCursor(pt2nt(tx), rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.add(getObjectIDData(value));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public SyncObjectIdSet getAllObjectIDs() {
    SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    Thread t = new Thread(objectIDManager.getObjectIDReader(rv), "ObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    return rv;
  }

  public SyncObjectIdSet getAllMapsObjectIDs() {
    SyncObjectIdSet rv = new SyncObjectIdSetImpl();

    Runnable reader = objectIDManager.getMapsObjectIDReader(rv);
    if (reader == null) return new NullSyncObjectIdSet();

    rv.startPopulating();
    Thread t = new Thread(reader, "MapsObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    return rv;
  }

  public Set loadRootNames() {
    Set rv = new HashSet();
    Cursor cursor = null;
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = rootDB.openCursor(pt2nt(tx), rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.add(getStringData(key));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public Map loadRootNamesToIDs() {
    Map rv = new HashMap();
    Cursor cursor = null;
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = rootDB.openCursor(pt2nt(tx), rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.put(getStringData(key), getObjectIDData(value));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public ManagedObject loadObjectByID(ObjectID id) {
    validateID(id);
    OperationStatus status = null;
    PersistenceTransaction tx = ptp.newTransaction();
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      setObjectIDData(key, id);
      status = this.objectDB.get(pt2nt(tx), key, value, LockMode.DEFAULT);
      if (OperationStatus.SUCCESS.equals(status)) {
        ManagedObject mo = getManagedObjectData(value);
        loadCollection(tx, mo);
        tx.commit();
        return mo;
      }
    } catch (Throwable e) {
      abortOnError(tx);
      throw new DBException(e);
    }
    if (OperationStatus.NOTFOUND.equals(status)) return null;
    else throw new DBException("Error retrieving object id: " + id + "; status: " + status);
  }

  private void loadCollection(PersistenceTransaction tx, ManagedObject mo) throws IOException, ClassNotFoundException,
      TCDatabaseException {
    ManagedObjectState state = mo.getManagedObjectState();
    if (PersistentCollectionsUtil.isPersistableCollectionType(state.getType())) {
      try {
        collectionsPersistor.loadCollectionsToManagedState(tx, mo.getID(), state);
      } catch (DatabaseException e) {
        throw new TCDatabaseException(e);
      }
    }
  }

  public void saveObject(PersistenceTransaction persistenceTransaction, ManagedObject managedObject) {
    Assert.assertNotNull(managedObject);
    validateID(managedObject.getID());
    OperationStatus status = null;
    try {
      status = basicSaveObject(persistenceTransaction, managedObject);
      if (OperationStatus.SUCCESS.equals(status) && managedObject.isNew()) {
        status = objectIDManager.put(persistenceTransaction, managedObject);
      }
    } catch (DBException e) {
      throw e;
    } catch (Throwable t) {
      throw new DBException("Trying to save object: " + managedObject, t);
    }

    if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException("Unable to write ManagedObject: "
                                                                         + managedObject + "; status: " + status); }

  }

  private OperationStatus basicSaveObject(PersistenceTransaction tx, ManagedObject managedObject)
      throws TCDatabaseException, IOException {
    if (!managedObject.isDirty()) return OperationStatus.SUCCESS;
    OperationStatus status;
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    setObjectIDData(key, managedObject.getID());
    setManagedObjectData(value, managedObject);
    int length = value.getSize();
    length += key.getSize();
    try {
      status = this.objectDB.put(pt2nt(tx), key, value);
      if (OperationStatus.SUCCESS.equals(status)) {
        length += basicSaveCollection(tx, managedObject);
        managedObject.setIsDirty(false);
        saveCount++;
        if (saveCount == 1 || saveCount % (100 * 1000) == 0) {
          logger.debug("saveCount: " + saveCount);
        }
      }
      if (STATS_LOGGING_ENABLED) {
        updateStats(managedObject, length);
      }
    } catch (DatabaseException de) {
      throw new TCDatabaseException(de);
    }
    return status;
  }

  private void updateStats(ManagedObject managedObject, int length) {
    String className = managedObject.getManagedObjectState().getClassName();
    record(className, length, managedObject.isNew());
  }

  private void record(String className, int length, boolean isNew) {
    commitStats.updateStats(className, 1, length, (isNew ? 1 : 0)); // count, bytes written, new
  }

  // logger.info("Deletes count:" + deleteCounter + " delete state count:" + deletePersistentStateCounter
  // + " usedTime(ns): " + deleteTime);

  private int basicSaveCollection(PersistenceTransaction tx, ManagedObject managedObject) throws IOException,
      TCDatabaseException {
    ManagedObjectState state = managedObject.getManagedObjectState();
    if (PersistentCollectionsUtil.isPersistableCollectionType(state.getType())) {
      try {
        return collectionsPersistor.saveCollections(tx, state);
      } catch (DatabaseException e) {
        throw new TCDatabaseException(e);
      }
    }
    return 0;
  }

  public void saveAllObjects(PersistenceTransaction persistenceTransaction, Collection managedObjects) {
    long t0 = System.currentTimeMillis();
    if (managedObjects.isEmpty()) return;
    Object failureContext = null;

    // XXX:: We are sorting so that we maintain lock ordering when writing to sleepycat (check
    // SleepycatPersistableMap.basicClear()). This is done under the assumption that this method is not called
    // twice with the same transaction
    Object old = persistenceTransaction.setProperty(MO_PERSISTOR_KEY, MO_PERSISTOR_VALUE);
    Assert.assertNull(old);
    SortedSet sortedList = getSortedManagedObjectsSet(managedObjects);
    SortedSet oidSet = new TreeSet();

    try {
      for (Iterator i = sortedList.iterator(); i.hasNext();) {
        final ManagedObject managedObject = (ManagedObject) i.next();

        final OperationStatus status = basicSaveObject(persistenceTransaction, managedObject);

        if (!OperationStatus.SUCCESS.equals(status)) {
          failureContext = new Object() {
            public String toString() {
              return "Unable to save ManagedObject: " + managedObject + "; status: " + status;
            }
          };
          break;
        }

        // record new object-IDs to be written to persistent store later.
        if (managedObject.isNew()) {
          objectIDManager.prePutAll(oidSet, managedObject);
        }
      }
      if (!OperationStatus.SUCCESS.equals(objectIDManager.putAll(persistenceTransaction, oidSet))) {
        //
        throw new DBException("Failed to save Object-IDs");
      }
    } catch (Throwable t) {
      throw new DBException(t);
    }

    if (failureContext != null) throw new DBException(failureContext.toString());

    long delta = System.currentTimeMillis() - t0;
    saveAllElapsed += delta;
    saveAllCount++;
    saveAllObjectCount += managedObjects.size();
    if (saveAllCount % (100 * 1000) == 0) {
      double avg = ((double) saveAllObjectCount / (double) saveAllElapsed) * 1000;
      logger.debug("save time: " + delta + ", " + managedObjects.size() + " objects; avg: " + avg + "/sec");
    }
  }

  private SortedSet getSortedManagedObjectsSet(Collection managedObjects) {
    TreeSet sorted = new TreeSet(MO_COMPARATOR);
    sorted.addAll(managedObjects);
    Assert.assertEquals(managedObjects.size(), sorted.size());
    return sorted;
  }

  private long saveAllCount       = 0;
  private long saveAllObjectCount = 0;
  private long saveAllElapsed     = 0;

  private void deleteObjectByID(PersistenceTransaction tx, ObjectID id) {
    validateID(id);
    try {
      DatabaseEntry key = new DatabaseEntry();
      setObjectIDData(key, id);
      OperationStatus status = this.objectDB.delete(pt2nt(tx), key);
      if (!(OperationStatus.NOTFOUND.equals(status) || OperationStatus.SUCCESS.equals(status))) {
        // make the formatter happy
        throw new DBException("Unable to remove ManagedObject for object id: " + id + ", status: " + status);
      } else {
        long startTime = 0;
        boolean isMapType = false;
        if (MEASURE_PERF) {
          startTime = System.nanoTime();
        }
        if (containsMapType(id)) {
          isMapType = true;
          // may return false if ManagedObject persistent state empty
          collectionsPersistor.deleteCollection(tx, id);
        }
        if (MEASURE_PERF) {
          perfMeasureStats.updateStats("Managed Objects deleted ", 1, (isMapType ? 1 : 0),
                                       (System.nanoTime() - startTime));
        }
      }
    } catch (DatabaseException t) {
      throw new DBException(t);
    }
  }

  /*
   * This method takes a SortedSet of Object ID to delete for two reasons. 1) to maintain lock ordering - check
   * saveAllObjects 2) for performance reason
   */
  public void deleteAllObjectsByID(PersistenceTransaction tx, SortedSet<ObjectID> sortedOids) {
    for (Iterator i = sortedOids.iterator(); i.hasNext();) {
      ObjectID objectId = (ObjectID) i.next();
      deleteObjectByID(tx, objectId);
    }

    try {
      objectIDManager.deleteAll(tx, sortedOids);
      removeAllMapTypeObject(sortedOids);
    } catch (TCDatabaseException de) {
      throw new TCRuntimeException(de);
    }
  }

  /**
   * This is only package protected for tests.
   */
  SerializationAdapter getSerializationAdapter() throws IOException {
    // XXX: This lazy initialization comes from how the sleepycat stuff is glued together in the server.
    if (serializationAdapter == null) serializationAdapter = saf.newAdapter(this.classCatalog);
    return serializationAdapter;
  }

  /*********************************************************************************************************************
   * Private stuff
   */

  private void validateID(ObjectID id) {
    Assert.assertNotNull(id);
    Assert.eval(!ObjectID.NULL_ID.equals(id));
  }

  private void setObjectIDData(DatabaseEntry entry, ObjectID objectID) {
    entry.setData(Conversion.long2Bytes(objectID.toLong()));
  }

  private void setStringData(DatabaseEntry entry, String string) throws IOException {
    getSerializationAdapter().serializeString(entry, string);
  }

  private void setManagedObjectData(DatabaseEntry entry, ManagedObject mo) throws IOException {
    getSerializationAdapter().serializeManagedObject(entry, mo);
  }

  private ObjectID getObjectIDData(DatabaseEntry entry) {
    return new ObjectID(Conversion.bytes2Long(entry.getData()));
  }

  private String getStringData(DatabaseEntry entry) throws IOException, ClassNotFoundException {
    return getSerializationAdapter().deserializeString(entry);
  }

  private ManagedObject getManagedObjectData(DatabaseEntry entry) throws IOException, ClassNotFoundException {
    return getSerializationAdapter().deserializeManagedObject(entry);
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(this.getClass().getName());
    out = out.duplicateAndIndent();
    out.println("db: " + objectDB);
    out.indent().print("extantObjectIDs: ").visit(extantObjectIDs).println();
    out.indent().print("extantMapTypeOidSet: ").visit(extantMapTypeOidSet).println();
    return out;
  }

  // for testing purpose only
  ObjectIDManager getOibjectIDManager() {
    return objectIDManager;
  }
}
