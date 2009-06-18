/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat.util;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProvider;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListener;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.SerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.util.SyncObjectIdSet;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PartitionDBData extends BaseUtility {

  private final Map<ObjectID, ObjectID> changeMap = new HashMap<ObjectID, ObjectID>();

  public PartitionDBData(File[] dir, Writer writer) throws Exception {
    super(writer, dir);
  }

  public void partitionData(int numberOfPartition) throws Exception {
    // ArrayList<TreeSet<ObjectID>> arrayList = new ArrayList<TreeSet<ObjectID>>();
    // for (int i = 0; i < numberOfPartition; i++) {
    // arrayList.add(new TreeSet<ObjectID>());
    // }
    SleepycatPersistor persistor = getPersistor(1);
    Set<ObjectID> roots = persistor.getManagedObjectPersistor().loadRoots();
    SyncObjectIdSet objectIDSet = persistor.getManagedObjectPersistor().getAllObjectIDs();
    objectIDSet.snapshot();

    // remove roots from others, they are handled differently
    objectIDSet.removeAll(roots);

    final ArrayList<ManagedObject> baseManagedObjects = new ArrayList<ManagedObject>();
    final ArrayList<ManagedObject> managedObjectRoots = new ArrayList<ManagedObject>();

    for (Iterator<ObjectID> rootIterator = roots.iterator(); rootIterator.hasNext();) {
      managedObjectRoots.add(persistor.getManagedObjectPersistor().loadObjectByID(rootIterator.next()));
    }

    for (Iterator<ObjectID> iter = objectIDSet.iterator(); iter.hasNext();) {
      baseManagedObjects.add(persistor.getManagedObjectPersistor().loadObjectByID(iter.next()));
    }

    mapChangeIDs(numberOfPartition, baseManagedObjects);

    // TreeSet<ObjectID> sortedOids = new TreeSet<ObjectID>();
    // sortedOids.addAll(objectIDSet);
    // log("total objects: " + objectIDSet.size());
    // log("objectIds before partition\n" + sortedOids);
    // int numOfObjectsInEachPartition = objectIDSet.size() / numberOfPartition;

    partitionData(numberOfPartition, baseManagedObjects, managedObjectRoots);

    // for (int i = 0; i < numberOfPartition; i++) {
    // log("size of partition " + (i + 1) + ": " + arrayList.get(i).size());
    // log("partition " + (i + 1) + "\n" + arrayList.get(i));
    // }

  }

  private void partitionData(int numberOfPartition, final ArrayList<ManagedObject> baseManagedObjects,
                             final ArrayList<ManagedObject> managedObjectRoots) throws Exception {
    Iterator<ManagedObject> iter = baseManagedObjects.iterator();
    SleepycatPersistor persistorPartition = null;

    int numOfObjectsInEachPartition = (baseManagedObjects.size() + managedObjectRoots.size()) / numberOfPartition;

    for (int j = 0; j < numberOfPartition; j++) {
      persistorPartition = createPartitionPersistor(j);
      // handle root, they will all go in the coordinator
      if (j == 0) {
        saveRootsToCoordinator(managedObjectRoots, persistorPartition);
      }

      for (int i = 0; i < numOfObjectsInEachPartition; i++) {
        ManagedObject mo = createUpdatedManagedObjectFrom(iter.next());
        saveManagedObject(i, persistorPartition, mo);
        // arrayList.get(j).add(mo.getID());
      }
    }

    while (iter.hasNext()) {
      ManagedObject mo = createUpdatedManagedObjectFrom(iter.next());
      saveManagedObject(numberOfPartition - 1, persistorPartition, mo);
      // arrayList.get(numberOfPartition - 1).add(mo.getID());
    }
  }

  private ManagedObject createUpdatedManagedObjectFrom(ManagedObject mo) {
    ObjectID oldId = mo.getID();
    ManagedObject newManagedObject = null;
    if (changeMap.containsKey(oldId)) {
      newManagedObject = new ManagedObjectImpl(changeMap.get(oldId));
    } else {
      newManagedObject = new ManagedObjectImpl(oldId);
    }

    // fill in here so that we can get the updated managed object
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    ObjectStringSerializer serializer = new ObjectStringSerializer();

    mo.toDNA(out, serializer);
    return newManagedObject;
  }

  private void mapChangeIDs(int numberOfPartition, ArrayList<ManagedObject> baseManagedObjects) {
    Iterator<ManagedObject> iter = baseManagedObjects.iterator();
    int numOfObjectsInEachPartition = baseManagedObjects.size() / numberOfPartition;
    for (int j = 0; j < numberOfPartition; j++) {

      for (int i = 0; i < numOfObjectsInEachPartition; i++) {
        ManagedObject mo = iter.next();
        mapIDsIfChanged(j, mo);
      }
    }

    while (iter.hasNext()) {
      ManagedObject mo = iter.next();
      mapIDsIfChanged(numberOfPartition - 1, mo);
    }
  }

  private void saveRootsToCoordinator(final ArrayList<ManagedObject> managedObjectRoots,
                                      SleepycatPersistor persistorPartition) {
    for (int k = 0; k < managedObjectRoots.size(); k++) {
      ManagedObject mo = createUpdatedManagedObjectFrom(managedObjectRoots.get(k));
      saveManagedObject(0, persistorPartition, mo);
    }
  }

  private SleepycatPersistor createPartitionPersistor(int j) throws Exception {
    DBEnvironment env = new DBEnvironment(true, new File(databaseDirs[0].getAbsolutePath() + "-partition" + j));
    SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
    final TestManagedObjectChangeListenerProvider managedObjectChangeListenerProvider = new TestManagedObjectChangeListenerProvider();
    SleepycatPersistor persistor = new SleepycatPersistor(getLogger(), env, serializationAdapterFactory);
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor);
    return persistor;
  }

  private void saveManagedObject(int partition, SleepycatPersistor persistor, ManagedObject mo) {
    mapIDsIfChanged(partition, mo);
    ManagedObjectPersistor managedObjectPersistor = persistor.getManagedObjectPersistor();
    PersistenceTransactionProvider persistenceTransactionProvider = persistor.getPersistenceTransactionProvider();
    PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
    mo.setIsDirty(true);
    managedObjectPersistor.saveObject(tx, mo);
    tx.commit();
  }

  private void mapIDsIfChanged(int partition, ManagedObject mo) {
    if (mo.getID().getGroupID() != partition) {
      ObjectID oldId = mo.getID();
      ObjectID newId = new ObjectID(oldId.getObjectID(), oldId.getGroupID());
      changeMap.put(oldId, newId);
    }
  }

  private static class TestManagedObjectChangeListenerProvider implements ManagedObjectChangeListenerProvider {

    public ManagedObjectChangeListener getListener() {
      return new NullManagedObjectChangeListener();

    }
  }
}
