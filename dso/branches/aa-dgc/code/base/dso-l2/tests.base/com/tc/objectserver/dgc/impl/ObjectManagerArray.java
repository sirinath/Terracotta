/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.NullCache;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.impl.InMemoryManagedObjectStore;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.impl.ObjectManagerImpl;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class ObjectManagerArray {

  private final int                 arrayCount;
  private final ObjectManagerImpl[] objectManagers;
  private final ClientStateManager  clientStateManager;

  public ObjectManagerArray(int arrayCount) {
    this.arrayCount = arrayCount;
    this.objectManagers = new ObjectManagerImpl[arrayCount];
    this.clientStateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));
    initObjectManagers();
  }

  public void initObjectManagers() {
    for (int i = 0; i < arrayCount; i++) {
      objectManagers[i] = createObjectManager();
    }
  }
  
  public ObjectManagerImpl[] getObjectManagers() {
    return objectManagers;
  }

  public void createObjects(Set<ObjectID> ids) {
    for (Iterator<ObjectID> iter = ids.iterator(); iter.hasNext();) {
      ObjectID id = iter.next();
      TestManagedObject mo = new TestManagedObject(id, new ObjectID[] {});
      Random rand = new Random();
      int index = rand.nextInt(arrayCount);
      objectManagers[index].createObject(mo);
      objectManagers[index].getObjectStore().addNewObject(mo);   
    }
  }

  private ObjectManagerImpl createObjectManager() {
    ObjectManagerConfig config = new TestObjectManagerConfig();
    ThreadGroup tg = new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(ObjectManagerImpl.class)));
    ManagedObjectStore store = new InMemoryManagedObjectStore(new HashMap());
    EvictionPolicy cache = new NullCache();
    PersistenceTransactionProvider persistenceTransactionalProvider = new TestPersistenceTransactionProvider();
    TestSink faultSink = new TestSink();
    TestSink flushSink = new TestSink();
    ObjectManagerImpl objectManager = new ObjectManagerImpl(config, tg, clientStateManager, store, cache,
                                                            persistenceTransactionalProvider, faultSink, flushSink);
    return objectManager;
  }

  private static class TestObjectManagerConfig extends ObjectManagerConfig {

    public long    myGCThreadSleepTime = 100;
    public boolean paranoid;

    public TestObjectManagerConfig() {
      super(10000, true, true, true, false, 60000);
    }

    TestObjectManagerConfig(long gcThreadSleepTime, boolean doGC) {
      super(gcThreadSleepTime, doGC, true, true, false, 60000);
      throw new RuntimeException("Don't use me.");
    }

    @Override
    public long gcThreadSleepTime() {
      return myGCThreadSleepTime;
    }

    @Override
    public boolean paranoid() {
      return paranoid;
    }
  }

}
