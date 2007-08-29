/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.MapManagedObjectState;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet2;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.SyncObjectIdSetImpl;
import com.tc.util.sequence.MutableSequence;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class ManagedObjectPersistorImpl extends SleepycatPersistorBase implements ManagedObjectPersistor {

  private static final Comparator              MO_COMPARATOR      = new Comparator() {

                                                                    public int compare(Object o1, Object o2) {
                                                                      long oid1 = ((ManagedObject) o1).getID().toLong();
                                                                      long oid2 = ((ManagedObject) o2).getID().toLong();
                                                                      if (oid1 < oid2) {
                                                                        return -1;
                                                                      } else if (oid1 > oid2) {
                                                                        return 1;
                                                                      } else {
                                                                        return 0;
                                                                      }
                                                                    }

                                                                  };
  private static final Object                  MO_PERSISTOR_KEY   = ManagedObjectPersistorImpl.class.getName()
                                                                    + ".saveAllObjects";
  private static final Object                  MO_PERSISTOR_VALUE = "Complete";

  private final Database                       objectDB;
  private final Database                       oidDB;
  private final SerializationAdapterFactory    saf;
  private final CursorConfig                   oidDBCursorConfig;
  private final MutableSequence                objectIDSequence;
  private final Database                       rootDB;
  private final CursorConfig                   rootDBCursorConfig;
  private long                                 saveCount;
  private final TCLogger                       logger;
  private final PersistenceTransactionProvider ptp;
  private final ClassCatalog                   classCatalog;
  SerializationAdapter                         serializationAdapter;
  private final SleepycatCollectionsPersistor  collectionsPersistor;
  private final OidBitsArrayMap                oidBitsArrayMap;
  private volatile boolean                     isPopulating;
  private final int                            BytesPerLong       = 8;
  private final int                            BitsPerLong        = BytesPerLong * 8;
  private final String LongsPerDiskEntry = "l2.objectmanager.loadObjectID.longsPerDiskEntry";
  private final String LongsPerMemoryEntry = "l2.objectmanager.loadObjectID.longsPerMemoryEntry";


  public ManagedObjectPersistorImpl(TCLogger logger, ClassCatalog classCatalog,
                                    SerializationAdapterFactory serializationAdapterFactory, Database objectDB,
                                    Database oidDB, CursorConfig oidDBCursorConfig, MutableSequence objectIDSequence,
                                    Database rootDB, CursorConfig rootDBCursorConfig,
                                    PersistenceTransactionProvider ptp,
                                    SleepycatCollectionsPersistor collectionsPersistor) {
    this.logger = logger;
    this.classCatalog = classCatalog;
    this.saf = serializationAdapterFactory;
    this.objectDB = objectDB;
    this.oidDB = oidDB;
    this.oidDBCursorConfig = oidDBCursorConfig;
    this.objectIDSequence = objectIDSequence;
    this.rootDB = rootDB;
    this.rootDBCursorConfig = rootDBCursorConfig;
    this.ptp = ptp;
    this.collectionsPersistor = collectionsPersistor;
    
    isPopulating = false;
    
    oidBitsArrayMap = new OidBitsArrayMap(
            TCPropertiesImpl.getProperties().getInt(LongsPerMemoryEntry),
            TCPropertiesImpl.getProperties().getInt(LongsPerDiskEntry));
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
    Thread t = new Thread(new ObjectIdReader(rv), "ObjectIdReaderThread");
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
    if (state.getType() == ManagedObjectState.MAP_TYPE || state.getType() == ManagedObjectState.PARTIAL_MAP_TYPE) {
      MapManagedObjectState mapState = (MapManagedObjectState) state;
      Assert.assertNull(mapState.getMap());
      try {
        mapState.setMap(collectionsPersistor.loadMap(tx, mo.getID()));
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
    try {
      status = this.objectDB.put(pt2nt(tx), key, value);
      if (OperationStatus.SUCCESS.equals(status)) {
        status = oidPut(pt2nt(tx), managedObject.getID());
      }
      if (OperationStatus.SUCCESS.equals(status)) {
        basicSaveCollection(tx, managedObject);
        managedObject.setIsDirty(false);
        saveCount++;
        if (saveCount == 1 || saveCount % (100 * 1000) == 0) {
          logger.debug("saveCount: " + saveCount);
        }
      }
    } catch (DatabaseException de) {
      throw new TCDatabaseException(de);
    }
    return status;
  }

  private void basicSaveCollection(PersistenceTransaction tx, ManagedObject managedObject) throws IOException,
      TCDatabaseException {
    ManagedObjectState state = managedObject.getManagedObjectState();
    if (state.getType() == ManagedObjectState.MAP_TYPE || state.getType() == ManagedObjectState.PARTIAL_MAP_TYPE) {
      MapManagedObjectState mapState = (MapManagedObjectState) state;
      SleepycatPersistableMap map = (SleepycatPersistableMap) mapState.getMap();
      try {
        collectionsPersistor.saveMap(tx, map);
      } catch (DatabaseException e) {
        throw new TCDatabaseException(e);
      }
    }
  }

  public void saveAllObjects(PersistenceTransaction persistenceTransaction, Collection managedObjects) {
    long t0 = System.currentTimeMillis();
    if (managedObjects.isEmpty()) return;
    Object failureContext = null;

    // XXX:: We are sorting so that we maintain lock ordering when writting to sleepycat (check
    // SleepycatPersistableMap.basicClear()). This is done under the assumption that this method is not called
    // twice with the same transaction
    Object old = persistenceTransaction.setProperty(MO_PERSISTOR_KEY, MO_PERSISTOR_VALUE);
    Assert.assertNull(old);
    SortedSet sortedList = getSortedManagedObjectsSet(managedObjects);

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
      if (OperationStatus.SUCCESS.equals(status)) status = oidDelete(tx, id);
      if (!(OperationStatus.NOTFOUND.equals(status) || OperationStatus.SUCCESS.equals(status))) {
        // make the formatter happy
        throw new DBException("Unable to remove ManagedObject for object id: " + id + ", status: " + status);
      } else {
        collectionsPersistor.deleteCollection(tx, id);
      }
    } catch (DatabaseException t) {
      throw new DBException(t);
    }
  }

  public void deleteAllObjectsByID(PersistenceTransaction tx, Collection objectIDs) {
    for (Iterator i = objectIDs.iterator(); i.hasNext();) {
      deleteObjectByID(tx, (ObjectID) i.next());
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

  public void prettyPrint(PrettyPrinter out) {
    out.println(this.getClass().getName());
    out = out.duplicateAndIndent();
    out.println("db: " + objectDB);
  }

  class ObjectIdReader implements Runnable {
    private final SyncObjectIdSet set;

    public ObjectIdReader(SyncObjectIdSet set) {
      this.set = set;
    }

    public void run() {
      ObjectIDSet2 tmp = new ObjectIDSet2();
      PersistenceTransaction tx = null;
      Cursor cursor = null;
      Thread helperThread;
      try {
        BoundedLinkedQueue queue;
        queue = new BoundedLinkedQueue(1000);
        helperThread = new Thread(new ObjectIdCreator(queue, tmp), "ObjectIdCreatorThread");
        helperThread.start();
        isPopulating = true;
        tx = ptp.newTransaction();
        cursor = oidDB.openCursor(pt2nt(tx), oidDBCursorConfig);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
            queue.put(new OidLongArray(key, value));
        }
        // null data to end helper thread
        queue.put(new OidLongArray(null, null));
        helperThread.join();
        if (MeasurePerf) {
          System.out.println("XXX done");
        }
      } catch (Throwable t) {
        logger.error("Error Reading Object IDs", t);
      } finally {
        isPopulating = false;
        safeClose(cursor);
        safeCommit(tx);
        set.stopPopulating(tmp);
        tmp = null;
      }
    }

    private void safeCommit(PersistenceTransaction tx) {
      if (tx == null) return;
      try {
        tx.commit();
      } catch (Throwable t) {
        logger.error("Error Committing Transaction", t);
      }
    }

    private void safeClose(Cursor c) {
      if (c == null) return;

      try {
        c.close();
      } catch (Throwable e) {
        logger.error("Error closing cursor", e);
      }
    }
  }
    
  boolean MeasurePerf = false;
  
  class ObjectIdCreator implements Runnable {
    ObjectIDSet2 tmp;
    BoundedLinkedQueue queue;
    long start_time;
    int counter = 0;
   
    ObjectIdCreator(BoundedLinkedQueue queue, ObjectIDSet2 tmp ) {
      this.queue = queue;
      this.tmp = tmp;
    }
        
    public void run() {
      if (MeasurePerf) start_time = new Date().getTime();
      while (true) {
        try {
          OidLongArray entry = (OidLongArray) queue.take();
          if (entry.isEnded()) break;
          process(entry);
        } catch(InterruptedException ex) {
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
            if (MeasurePerf) {
              if ((++counter % 1000) == 0) {
                long elapse_time = new Date().getTime() - start_time;
                long avg_time = elapse_time / (counter / 1000);
                System.out.println("XXX reading " + counter + " OIDs " + "took " + 
                                   elapse_time + "ms avg(1000 objs):"
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

  OperationStatus oidPut(Transaction tx, ObjectID objectId) throws TCDatabaseException {
    // skip reduntant db write
    if (oidBitsArrayMap.contains(objectId)) return OperationStatus.SUCCESS;
    
    // while populating object IDs, go to disk to get original data before overwrite it.
    if (isPopulating) {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      key.setData(oidBitsArrayMap.onDiskIndex2Bytes(objectId));
      try {
        if (OperationStatus.SUCCESS.equals(this.oidDB.get(null, key, value, LockMode.DEFAULT))) {
          oidBitsArrayMap.applyOnDiskEntry(new OidLongArray(key, value));
        }
      } catch (DatabaseException e) {
        logger.warn("Reading object ID " + objectId + ":" + e);
      }
    }
 
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    synchronized (oidBitsArrayMap) {
      OidLongArray bits = oidBitsArrayMap.getAndSet(objectId);
      key.setData(bits.keyToBytes());
      value.setData(bits.arrayToBytes());
    }
    try {
      return this.oidDB.put(tx, key, value);
    } catch (DatabaseException de) {
      throw new TCDatabaseException(de);
    }
  }

  OperationStatus oidDelete(PersistenceTransaction tx, ObjectID objectId) throws DatabaseException {
    DatabaseEntry key = new DatabaseEntry();
    OidLongArray bits;
    synchronized (oidBitsArrayMap) {
      bits = oidBitsArrayMap.getAndClr(objectId);
      key.setData(bits.keyToBytes());
    }
    if (bits.isZero()) {
      return (this.oidDB.delete(pt2nt(tx), key));
    } else {
      DatabaseEntry value = new DatabaseEntry();
      value.setData(bits.arrayToBytes());
      return (this.oidDB.put(pt2nt(tx), key, value));
    }
  }

  private class OidBitsArrayMap {
    final private ConcurrentHashMap map;
    final int longsPerMemUnit;
    final int memBitsLength;
    final int longsPerDiskUnit;
    final int diskBitsLength;
    
    OidBitsArrayMap(int longsPerMemUnit, int longsPerDiskUnit) { 
      this.longsPerMemUnit = longsPerMemUnit;
      this.memBitsLength = longsPerMemUnit * BitsPerLong;
      this.longsPerDiskUnit = longsPerDiskUnit;
      this.diskBitsLength = longsPerDiskUnit * BitsPerLong;
      map = new ConcurrentHashMap();
      
      Assert.assertTrue("LongsPerMemUnit must be multiple of LongsPerDiskUnit", 
                        (longsPerMemUnit % longsPerDiskUnit) == 0);
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
  
    private OidLongArray getBitsArray(Long mapIndex) {
      OidLongArray longAry;
      synchronized(map) {
        if (map.containsKey(mapIndex)) {
          longAry = (OidLongArray) map.get(mapIndex);
        } else {
          longAry = new OidLongArray(longsPerMemUnit, mapIndex.longValue());
          map.put(mapIndex, longAry);
        }
      }
      return longAry;
    }
    
    private OidLongArray getArrayForDisk(OidLongArray inMemLongAry, long oid) {
      // first oid for on-disk array
      long keyOnDisk = oidOnDiskIndex(oid);
      OidLongArray onDiskAry = new OidLongArray(longsPerDiskUnit, keyOnDisk);
      int offset = (int)(keyOnDisk % memBitsLength) / BitsPerLong;
      inMemLongAry.copyOut(onDiskAry, offset);
      return onDiskAry;
    }

    private OidLongArray getAndModify(long oid, boolean doSet) {
      OidLongArray longAry = getBitsArray(oidInMemIndex(oid));
      int oidInArray = (int)(oid % memBitsLength);
      synchronized (longAry) {
        if (doSet) {
          longAry.setBit(oidInArray);
        } else {
          longAry.clrBit(oidInArray);
        }
        
        // purge out array if empty
        /* not thread safe
        if(!doSet) {
          if ((value == 0L) && longAry.isZero()) {
            map.remove(mapIndex);
          }
        }
        */
        return(getArrayForDisk(longAry, oid));
      }
    }
        
    public OidLongArray getAndSet(ObjectID id) {
      return (getAndModify(id.toLong(), true));
    }
    
    public OidLongArray getAndClr(ObjectID id) {
      return (getAndModify(id.toLong(), false));
    }
        
    public void applyOnDiskEntry(OidLongArray entry) {
      OidLongArray inMemArray = getBitsArray(oidInMemIndex(entry.getKey()));
      int offset = (int) (entry.getKey() % memBitsLength) / BitsPerLong;
      inMemArray.applyIn(entry, offset);
    }
        
    public boolean contains(ObjectID id) {
      long oid = id.toLong();
      Long mapIndex = oidInMemIndex(oid);
      synchronized(map) {
        if (map.containsKey(mapIndex)) {
          OidLongArray longAry = (OidLongArray) map.get(mapIndex);
          return (longAry.isSet((int)oid % memBitsLength));
        }
      }
      return (false); 
    }
  }
  
  private class OidLongArray {
    private long key;
    private long[] ary;
    
    OidLongArray(int size) {
      ary = new long[size];
    }
    
    OidLongArray(int size, long key) {
      this(size);
      this.key = key;
    }
    
    OidLongArray(DatabaseEntry key, DatabaseEntry value) {
      // ary null is used as an indicator for end of processing
      if (key == null || value == null) {
        ary = null;
        return;
      }
      
      this.key = Conversion.bytes2Long(key.getData());
      byte[] bytesData = value.getData();
      ary = new long[bytesData.length / BytesPerLong];
      for(int i = 0; i < ary.length; ++i) {
        ary[i] = Conversion.bytes2Long(bytesData, i * BytesPerLong);
      }
    }
    
    private long bit(int bitIndex) {
      Assert.assertTrue("Bit index out of range", bitIndex >= 0);
      Assert.assertTrue("Bit index out of range", bitIndex < BitsPerLong);
      return 1L << bitIndex;
    }      

    void setKey(long key) {
      this.key = key;
    }
    
    long getKey() {
      return (this.key);
    }
    
    long[] getArray() {
      return(this.ary);
    }
    
    byte[] keyToBytes() {
      return Conversion.long2Bytes(key);
    }
    
    byte[] arrayToBytes() {
      byte[] data = new byte[length() * BytesPerLong];
      for(int i = 0; i < length(); ++i) {
        Conversion.writeLong(ary[i], data, i * BytesPerLong);
      }
      return(data);
    }
    
    void copyOut(OidLongArray dest, int offset) {
      for(int i = 0; i < dest.length(); ++i) {
        dest.set(i, ary[offset + i]);
      }
    }
    
    void applyIn(OidLongArray src, int offset) {
      for(int i = 0; i < src.length(); ++i) {
        ary[i+offset] |= src.get(i);
      }
    }
   
    boolean isZero() {
      for(int i = 0; i < length(); ++i) {
        if (ary[i] != 0) return (false);
      }
      return (true);
    }
    
    // use null array as an indicator of end of record
    boolean isEnded() {
      return (ary == null);
    }
    
    long get(int index) {
      return ary[index];
    }

    void set(int index, long val) {
      ary[index] = val;
    }

    int length() {
      return ary.length;
    }
    
    long setBit(int bit) {
      int byteIndex = bit / BitsPerLong;
      int bitIndex = bit % BitsPerLong;
      ary[byteIndex] |= bit(bitIndex);
      return (ary[byteIndex]);
    }
    
    long clrBit(int bit) {
      int byteIndex = bit / BitsPerLong;
      int bitIndex = bit % BitsPerLong;
      ary[byteIndex] &= ~bit(bitIndex);
      return (ary[byteIndex]);
    }
    
    boolean isSet(int bit) {
      int byteIndex = bit / BitsPerLong;
      int bitIndex = bit % BitsPerLong;
      return((ary[byteIndex] & bit(bitIndex)) != 0);
    }


  }
  
}
