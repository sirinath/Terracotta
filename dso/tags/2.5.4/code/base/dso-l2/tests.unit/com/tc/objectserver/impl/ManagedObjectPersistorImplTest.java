/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.sleepycat.je.CursorConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.ManagedObjectPersistorImpl;
import com.tc.objectserver.persistence.sleepycat.OidBitsArrayMapManagerImpl;
import com.tc.objectserver.persistence.sleepycat.SleepycatCollectionFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatCollectionsPersistor;
import com.tc.objectserver.persistence.sleepycat.SleepycatSerializationAdapterFactory;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.SyncObjectIdSet;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class ManagedObjectPersistorImplTest extends TCTestCase {
  private static final TCLogger        logger = TCLogging.getTestingLogger(ManagedObjectPersistorImplTest.class);
  private ManagedObjectPersistorImpl   managedObjectPersistor;
  private PersistentManagedObjectStore objectStore;
  private PersistenceTransactionProvider       persistenceTransactionProvider;
  private DBEnvironment                        env;
  private OidBitsArrayMapManagerImpl   oidManager;
  private final String                 OID_FAST_LOAD = "l2.objectmanager.loadObjectID.fastLoad";


  public ManagedObjectPersistorImplTest() {
    //
  }

  protected void setUp() throws Exception {
    super.setUp();
    // test only with Oid fastLoad enabled
    TCPropertiesImpl.setProperty(OID_FAST_LOAD, "true");
    assertTrue( TCPropertiesImpl.getProperties().getBoolean(OID_FAST_LOAD));
    boolean paranoid = true;
    env = newDBEnvironment(paranoid);
    env.open();
    CursorConfig dbCursorConfig = new CursorConfig();
    persistenceTransactionProvider = new TestPersistenceTransactionProvider();
    CursorConfig rootDBCursorConfig = new CursorConfig();
    SleepycatCollectionFactory sleepycatCollectionFactory = new SleepycatCollectionFactory();
    SleepycatCollectionsPersistor sleepycatCollectionsPersistor = new SleepycatCollectionsPersistor(logger, env
        .getMapsDatabase(), sleepycatCollectionFactory);
    managedObjectPersistor = new ManagedObjectPersistorImpl(logger, env.getClassCatalogWrapper().getClassCatalog(),
                                                            new SleepycatSerializationAdapterFactory(), env
                                                                .getObjectDatabase(), env.getOidDatabase(),
                                                            dbCursorConfig, new TestMutableSequence(), env
                                                                .getRootDatabase(), rootDBCursorConfig,
                                                            persistenceTransactionProvider,
                                                            sleepycatCollectionsPersistor, env.isParanoidMode());
    objectStore = new PersistentManagedObjectStore(managedObjectPersistor);
    oidManager = managedObjectPersistor.getOidManager();
  }

  protected void tearDown() throws Exception {
    env.close();
    super.tearDown();
  }

  private DBEnvironment newDBEnvironment(boolean paranoid) throws Exception {
    File dbHome;
    int count = 0;
    do {
      dbHome = new File(this.getTempDirectory(), getClass().getName() + "db" + (++count));
    } while (dbHome.exists());
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new DBEnvironment(paranoid, dbHome);
  }

  private Collection createRandomObjects(int num) {
    Random r = new Random();
    HashSet objects = new HashSet(num);
    HashSet ids = new HashSet(num);
    for (int i = 0; i < num; i++) {
      long id = (long) r.nextInt(num * 10) + 1;
      if (ids.add(new Long(id))) {
        ManagedObject mo = new TestManagedObject(new ObjectID(id), new ObjectID[] {});
        objects.add(mo);
      }
    }
    logger.info("Test with " + objects.size() + " objects");
    return (objects);
  }

  private void verify(Collection objects) {
    // verify a in-memory bit crosspond to an object ID
    HashSet originalIds = new HashSet();
    for (Iterator i = objects.iterator(); i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      originalIds.add(mo.getID());
    }
    Collection inMemoryIds = oidManager.bitsArrayMapToObjectID();
    assertTrue("Wrong bits in memory were set", originalIds.containsAll(inMemoryIds));

    // verify on disk object IDs
    // clear in memory arrays then read in from persistor
    oidManager.resetBitsArrayMap();
    SyncObjectIdSet idSet = managedObjectPersistor.getAllObjectIDs();
    idSet.snapshot(); // blocked while reading from disk
    Collection diskIds = oidManager.bitsArrayMapToObjectID();
    assertTrue("Wrong object IDs on disk", diskIds.equals(inMemoryIds));

  }

  public void testOidBitsArraySave() throws Exception {
    // wait for background retrieving persistent data
    objectStore.getAllObjectIDs();
    oidManager.resetBitsArrayMap();

    // publish data
    Collection objects = createRandomObjects(15050);
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.saveAllObjects(ptx, objects);
    ptx.commit();

    // verify object IDs is in memory
    for (Iterator i = objects.iterator(); i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      assertTrue("Object:" + mo.getID() + " missed in memory! ", oidManager.inMemoryContains(mo.getID()));
    }

    verify(objects);
  }

  public void testOidBitsArrayDeleteHalf() throws Exception {
    // wait for background retrieving persistent data
    objectStore.getAllObjectIDs();
    oidManager.resetBitsArrayMap();

    // publish data
    Collection objects = createRandomObjects(15050);
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.saveAllObjects(ptx, objects);
    ptx.commit();

    int total = objects.size();
    HashSet toDelete = new HashSet();
    int count = 0;
    for (Iterator i = objects.iterator(); (count < total / 2) && i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      toDelete.add(mo.getID());
      i.remove();
    }
    ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.deleteAllObjectsByID(ptx, toDelete);
    ptx.commit();

    verify(objects);
  }

  public void testOidBitsArrayDeleteAll() throws Exception {
    // wait for background retrieving persistent data
    objectStore.getAllObjectIDs();
    oidManager.resetBitsArrayMap();

    // publish data
    Collection objects = createRandomObjects(15050);
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.saveAllObjects(ptx, objects);
    ptx.commit();

    HashSet objectIds = new HashSet();
    for (Iterator i = objects.iterator(); i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      objectIds.add(mo.getID());
    }
    ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.deleteAllObjectsByID(ptx, objectIds);
    ptx.commit();

    objects.clear();
    verify(objects);
  }

}
