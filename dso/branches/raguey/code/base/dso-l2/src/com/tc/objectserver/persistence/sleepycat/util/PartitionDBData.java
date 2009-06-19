/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat.util;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.ObjectDNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.managedobject.BackReferences;
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
import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class PartitionDBData extends BaseUtility {

  private final Map<ObjectID, ObjectID> changeMap = new HashMap<ObjectID, ObjectID>();

  public PartitionDBData(File[] dir, Writer writer) throws Exception {
    super(writer, dir);
  }

  public void partitionData(int numberOfPartition) throws Exception {
    SleepycatPersistor persistor = getPersistor(1);
    Map<String, ObjectID> roots = persistor.getManagedObjectPersistor().loadRootNamesToIDs();
    SyncObjectIdSet objectIDSet = persistor.getManagedObjectPersistor().getAllObjectIDs();
    objectIDSet.snapshot();

    // remove roots from others, they are handled differently
    objectIDSet.removeAll(roots.values());

    final ArrayList<ManagedObject> baseManagedObjects = new ArrayList<ManagedObject>();
    final Map<String, ManagedObject> managedObjectRoots = new HashMap<String, ManagedObject>();

    for (Iterator<Entry<String, ObjectID>> rootIterator = roots.entrySet().iterator(); rootIterator.hasNext();) {
      Entry<String, ObjectID> entry = rootIterator.next();
      managedObjectRoots.put(entry.getKey(), persistor.getManagedObjectPersistor().loadObjectByID(entry.getValue()));
    }

    for (Iterator<ObjectID> objecsIter = objectIDSet.iterator(); objecsIter.hasNext();) {
      ManagedObject mo = persistor.getManagedObjectPersistor().loadObjectByID(objecsIter.next());
      baseManagedObjects.add(mo);
    }

    mapChangeIDs(numberOfPartition, baseManagedObjects);
    partitionData(numberOfPartition, baseManagedObjects, managedObjectRoots);
  }

  private void partitionData(int numberOfPartition, final ArrayList<ManagedObject> baseManagedObjects,
                             final Map<String, ManagedObject> managedObjectRoots) throws Exception {
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
      }
    }

    while (iter.hasNext()) {
      ManagedObject mo = createUpdatedManagedObjectFrom(iter.next());
      saveManagedObject(numberOfPartition - 1, persistorPartition, mo);
    }
  }

  private ManagedObject createUpdatedManagedObjectFrom(ManagedObject mo) {

    System.out.println("original mo: " + mo);
    System.out.println("original object ref: " + mo.getObjectReferences());
    ObjectID oldId = mo.getID();
    ManagedObject newManagedObject = null;
    if (changeMap.containsKey(oldId)) {
      newManagedObject = new ManagedObjectImpl(changeMap.get(oldId));
    } else {
      newManagedObject = new ManagedObjectImpl(oldId);
    }

    // get the new references from the old ones
    Set<ObjectID> newRef = getNewObjectReferences(mo);

    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    mo.toDNA(out, serializer);
    TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    ObjectDNAImpl dna = new ObjectDNAImpl(serializer, false);
    try {
      dna.deserializeFrom(in);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    System.out.println(dna);

    BackReferences backReferences = new BackReferences();
    System.out.println("new ref size: " + newRef.size());
    for (Iterator<ObjectID> iter = newRef.iterator(); iter.hasNext();) {
      backReferences.addBackReference(iter.next(), newManagedObject.getID());
    }

    DNA change = new VersionizedDNAWrapper(dna, 0, true);
    newManagedObject.apply(change, new TransactionID(1), backReferences, new ObjectInstanceMonitorImpl(), true);
    System.out.println("new mo: " + newManagedObject);
    System.out.println("new object ref: " + newManagedObject.getObjectReferences());
    System.out.println("----------------------------------------------");
    return newManagedObject;
  }

  private Set<ObjectID> getNewObjectReferences(ManagedObject mo) {
    Set<ObjectID> oldRef = mo.getObjectReferences();
    Set<ObjectID> newRef = new ObjectIDSet();
    for (Iterator<ObjectID> iter = oldRef.iterator(); iter.hasNext();) {
      ObjectID oid = iter.next();
      if (changeMap.containsKey(oid)) {
        newRef.add(changeMap.get(oid));
      } else {
        newRef.add(oid);
      }
    }
    return newRef;
  }

  private void mapChangeIDs(int numberOfPartition, ArrayList<ManagedObject> baseManagedObjects) {
    Iterator<ManagedObject> baseManagedObjectIter = baseManagedObjects.iterator();
    int numOfObjectsInEachPartition = baseManagedObjects.size() / numberOfPartition;
    for (int j = 0; j < numberOfPartition; j++) {
      for (int i = 0; i < numOfObjectsInEachPartition; i++) {
        ManagedObject mo = baseManagedObjectIter.next();
        mapIDsIfChanged(j, mo);
      }
    }

    while (baseManagedObjectIter.hasNext()) {
      ManagedObject mo = baseManagedObjectIter.next();
      mapIDsIfChanged(numberOfPartition - 1, mo);
    }
  }

  private void mapIDsIfChanged(int partition, ManagedObject mo) {
    if (mo.getID().getGroupID() != partition) {
      ObjectID oldId = mo.getID();
      ObjectID newId = new ObjectID(oldId.getObjectID(), oldId.getGroupID());
      changeMap.put(oldId, newId);
    }
  }

  private void saveRootsToCoordinator(final Map<String, ManagedObject> managedObjectRoots, SleepycatPersistor persistor)
      throws Exception {
    for (Iterator<Entry<String, ManagedObject>> iter = managedObjectRoots.entrySet().iterator(); iter.hasNext();) {
      Entry<String, ManagedObject> entry = iter.next();
      String rootName = entry.getKey();
      ManagedObject root = createUpdatedManagedObjectFrom(entry.getValue());

      ManagedObjectPersistor managedObjectPersistor = persistor.getManagedObjectPersistor();
      PersistenceTransactionProvider persistenceTransactionProvider = persistor.getPersistenceTransactionProvider();
      PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
      root.setIsDirty(true);
      persistor.getManagedObjectPersistor().addRoot(tx, rootName, root.getID());
      managedObjectPersistor.saveObject(tx, entry.getValue());
      tx.commit();
    }
  }

  private void saveManagedObject(int partition, SleepycatPersistor persistor, ManagedObject mo) {
    ManagedObjectPersistor managedObjectPersistor = persistor.getManagedObjectPersistor();
    PersistenceTransactionProvider persistenceTransactionProvider = persistor.getPersistenceTransactionProvider();
    PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
    mo.setIsDirty(true);
    managedObjectPersistor.saveObject(tx, mo);
    tx.commit();
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

  private static class TestManagedObjectChangeListenerProvider implements ManagedObjectChangeListenerProvider {

    public ManagedObjectChangeListener getListener() {
      return new NullManagedObjectChangeListener();

    }
  }
}
