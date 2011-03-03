/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.logging.DumpHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.TextDecoratorTCLogger;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.cache.CacheStats;
import com.tc.object.cache.Evictable;
import com.tc.object.cache.EvictionPolicy;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.impl.GarbageCollectorThread;
import com.tc.objectserver.dgc.impl.NullGarbageCollector;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.tx.NullTransactionalObjectManager;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Counter;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.StoppableThread;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class StripedObjectManagerImpl implements ObjectManager, ManagedObjectChangeListener, ObjectManagerMBean,
    Evictable, DumpHandler, PrettyPrintable {

  private final TCLogger             logger       = TCLogging.getLogger(ObjectManager.class);
  private final ObjectManagerImpl    objectManagers[];
  private final int                  segmentShift;
  private final int                  segmentMask;
  private GarbageCollector           collector    = new NullGarbageCollector();
  private TransactionalObjectManager txnObjectMgr = new NullTransactionalObjectManager();

  private final ThreadGroup          gcThreadGroup;
  private final ObjectManagerConfig  config;
  private int                        stripedC     = 0;

  public StripedObjectManagerImpl(ObjectManagerConfig config, ThreadGroup gcThreadGroup,
                                  ClientStateManager stateManager, ManagedObjectStore objectStore,
                                  EvictionPolicy cache, PersistenceTransactionProvider persistenceTransactionProvider,
                                  Sink faultSink, Sink flushSink) {
    this.gcThreadGroup = gcThreadGroup;
    this.config = config;
    int stripedCount = 16;
    this.stripedC = stripedCount;

    int sshift = 0;
    int ssize = 1;
    while (ssize < stripedCount) {
      ++sshift;
      ssize <<= 1;
    }
    segmentShift = 32 - sshift;
    segmentMask = ssize - 1;

    objectManagers = new ObjectManagerImpl[ssize];
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i] = new ObjectManagerImpl(new TextDecoratorTCLogger(logger, "OM[" + i + "]"), config,
                                                gcThreadGroup, stateManager, objectStore, cache,
                                                persistenceTransactionProvider, faultSink, flushSink);
    }
  }

  /**
   * Returns the segment that should be used for key with given hash
   * 
   * @param hash the hash code for the key
   * @return the segment
   */
  final ObjectManagerImpl objectManagerFor(ObjectID oid) {
    return objectManagerFor(oid.asString());
  }

  private ObjectManagerImpl objectManagerFor(String oid) {
    int hash = hash(oid.hashCode());
    return objectManagers[hash2Index(hash)];
  }

  private static int hash(int h) {
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  private int hash2Index(int hash) {
    int index = (hash >>> segmentShift) & segmentMask;
    Assert.assertTrue("index value = " + index + " stripedC = " + stripedC, index < stripedC);
    return index;
  }

  private Map partitionManagedObjects(Collection managedObjects) {
    final Map objectID2ManagedObjects = new HashMap();
    for (Iterator iter = managedObjects.iterator(); iter.hasNext();) {
      ManagedObject mo = (ManagedObject) iter.next();
      ObjectID id = mo.getID();
      int hash = hash(id.asString().hashCode());
      int index = hash2Index(hash);
      List list = (List) objectID2ManagedObjects.get(index);
      if (list == null) {
        list = new ArrayList();
        objectID2ManagedObjects.put(index, list);
      }
      list.add(mo);
    }
    return objectID2ManagedObjects;
  }

  private Map partitionObjectIDs(Set objectsIDs) {
    final Map objectID2ObjectIDs = new HashMap();
    for (Iterator iter = objectsIDs.iterator(); iter.hasNext();) {
      ObjectID id = (ObjectID) iter.next();
      int hash = hash(id.asString().hashCode());
      int index = hash2Index(hash);
      Set set = (Set) objectID2ObjectIDs.get(index);
      if (set == null) {
        set = new ObjectIDSet();
        objectID2ObjectIDs.put(index, set);
      }
      set.add(id);
    }
    return objectID2ObjectIDs;
  }

  public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
    objectManagerFor(oid).addFaultedObject(oid, mo, removeOnRelease);
  }

  public void createNewObjects(Set<ObjectID> ids) {
    for (final ObjectID oid : ids) {
      ManagedObject mo = new ManagedObjectImpl(oid);
      objectManagerFor(oid).createObject(mo);
    }
  }

  public void createRoot(String name, ObjectID id) {
    objectManagerFor(id).createRoot(name, id);
  }

  public synchronized void flushAndEvict(List objects2Flush) {

    final Map objectID2ManagedObjects = partitionManagedObjects(objects2Flush);
    for (Iterator iter = objectID2ManagedObjects.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      int index = (Integer) entry.getKey();
      List list = (List) entry.getValue();
      objectManagers[index].flushAndEvict(list);
    }
  }

  public synchronized ObjectIDSet getAllObjectIDs() {
    ObjectIDSet allIDs = new ObjectIDSet();
    for (int i = 0; i < objectManagers.length; i++) {
      allIDs.addAll(objectManagers[i].getAllObjectIDs());
    }
    return allIDs;
  }

  public synchronized int getCheckedOutCount() {
    int checkedOutCount = 0;
    for (int i = 0; i < objectManagers.length; i++) {
      checkedOutCount += objectManagers[i].getCheckedOutCount();
    }
    return checkedOutCount;
  }

  public GarbageCollector getGarbageCollector() {
    return this.collector;
  }

  public ManagedObject getObjectByIDOrNull(ObjectID id) {
    return objectManagerFor(id).getObjectByIDOrNull(id);
  }

  public ManagedObject getObjectFromCacheByIDOrNull(ObjectID id) {
    return objectManagerFor(id).getObjectFromCacheByIDOrNull(id);
  }

  public synchronized ObjectIDSet getObjectIDsInCache() {
    ObjectIDSet allObjectIDsInCache = new ObjectIDSet();
    for (int i = 0; i < objectManagers.length; i++) {
      allObjectIDsInCache.addAll(objectManagers[i].getObjectIDsInCache());
    }
    return allObjectIDsInCache;
  }

  public synchronized Set getRootIDs() {
    ObjectIDSet rootIDs = new ObjectIDSet();
    for (int i = 0; i < objectManagers.length; i++) {
      rootIDs.addAll(objectManagers[i].getRootIDs());
    }
    return rootIDs;
  }

  public synchronized Map getRootNamesToIDsMap() {
    Map allRootNamesToIDsMap = new HashMap();
    for (int i = 0; i < objectManagers.length; i++) {
      allRootNamesToIDsMap.putAll(objectManagers[i].getRootNamesToIDsMap());
    }
    return allRootNamesToIDsMap;
  }

  public synchronized Iterator getRoots() {
    Set roots = new HashSet();
    for (int i = 0; i < objectManagers.length; i++) {
      roots.addAll(objectManagers[i].getRootsSet());
    }
    return roots.iterator();
  }

  public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext, int maxCount) {

    Map oidsMap = partitionObjectIDs(responseContext.getLookupIDs());
    Counter stripedCount = new Counter(oidsMap.keySet().size());
    ObjectManagerLookupResults resultsContext = new ObjectManagerLookupResultsImpl(
                                                                                   new HashMap<ObjectID, ManagedObject>());

    if (oidsMap.size() < 1) { return false; }

    boolean returnVal = true;

    for (Iterator iter = oidsMap.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      int index = (Integer) entry.getKey();
      Set oidsSet = (Set) entry.getValue();
      StripedObjectManagerResultsContext stripedObjectManagerResultsContext = new StripedObjectManagerResultsContext(
                                                                                                                     stripedCount,
                                                                                                                     resultsContext,
                                                                                                                     new ObjectIDSet(
                                                                                                                                     oidsSet),
                                                                                                                     responseContext);
      boolean rtr = objectManagers[index].lookupObjectsAndSubObjectsFor(nodeID, stripedObjectManagerResultsContext,
                                                                        maxCount);
      if (!rtr) {
        returnVal = false;
      }
    }
    return returnVal;
  }

  public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {

    Map oidsMap = partitionObjectIDs(context.getLookupIDs());
    Counter stripedCount = new Counter(oidsMap.keySet().size());
    ObjectManagerLookupResults resultsContext = new ObjectManagerLookupResultsImpl(
                                                                                   new HashMap<ObjectID, ManagedObject>());

    if (oidsMap.size() < 1) { return false; }

    boolean returnVal = true;

    for (Iterator iter = oidsMap.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      int index = (Integer) entry.getKey();
      Set oidsSet = (Set) entry.getValue();
      StripedObjectManagerResultsContext stripedObjectManagerResultsContext = new StripedObjectManagerResultsContext(
                                                                                                                     stripedCount,
                                                                                                                     resultsContext,
                                                                                                                     new ObjectIDSet(
                                                                                                                                     oidsSet),
                                                                                                                     context);

      boolean rtr = objectManagers[index].lookupObjectsFor(nodeID, stripedObjectManagerResultsContext);
      if (!rtr) {
        returnVal = false;
      }

    }
    return returnVal;
  }

  public synchronized ObjectID lookupRootID(String name) {
    ObjectID objectID = ObjectID.NULL_ID;
    for (int i = 0; i < objectManagers.length; i++) {
      objectID = objectManagers[i].lookupRootID(name);
      if (!(ObjectID.NULL_ID.equals(objectID))) { return objectID; }
    }
    return ObjectID.NULL_ID;
  }

  public synchronized void notifyGCComplete(GCResultContext resultContext) {
    Set objectIDs = resultContext.getGCedObjectIDs();

    Map oidsMap = partitionObjectIDs(objectIDs);
    for (Iterator iter = oidsMap.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      int index = (Integer) entry.getKey();
      Set oidsSet = (Set) entry.getValue();
      GCResultContext cnxt = new GCResultContext(resultContext.getGCIterationCount(), new TreeSet(oidsSet),
                                                 resultContext.getGCInfo(), resultContext.getGCPublisher());
      objectManagers[index].notifyGCComplete(cnxt);
    }

  }

  public void preFetchObjectsAndCreate(Set<ObjectID> oids, Set<ObjectID> newOids) {
    Map combinedMap = new HashMap();
    Map oidsMap = partitionObjectIDs(oids);
    Map newOidsMap = partitionObjectIDs(newOids);
    combinedMap.putAll(oidsMap);
    combinedMap.putAll(newOidsMap);
    for (Iterator iter = combinedMap.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      int index = (Integer) entry.getKey();
      Set oidsSet = (Set) oidsMap.get(index);
      Set newOidsSet = (Set) newOidsMap.get(index);
      if (oidsSet == null) {
        oidsSet = new HashSet();
      }
      if (newOidsSet == null) {
        newOidsSet = new HashSet();
      }
      Assert.assertNotNull(oidsSet);
      Assert.assertNotNull(newOidsSet);
      objectManagers[index].preFetchObjectsAndCreate(oidsSet, newOidsSet);
    }
  }

  public void release(PersistenceTransaction tx, ManagedObject object) {
    ObjectID id = object.getID();
    objectManagerFor(id).release(tx, object);
  }

  public synchronized void releaseAll(PersistenceTransaction tx, Collection<ManagedObject> collection) {
    final Map objectID2ManagedObjects = partitionManagedObjects(collection);
    for (Iterator iter = objectID2ManagedObjects.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      int index = (Integer) entry.getKey();
      List list = (List) entry.getValue();
      objectManagers[index].releaseAll(tx, list);
    }
  }

  public synchronized void releaseAllReadOnly(Collection<ManagedObject> objects) {
    final Map objectID2ManagedObjects = partitionManagedObjects(objects);
    for (Iterator iter = objectID2ManagedObjects.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      int index = (Integer) entry.getKey();
      List list = (List) entry.getValue();
      objectManagers[index].releaseAllReadOnly(list);
    }
  }

  public void releaseReadOnly(ManagedObject object) {
    ObjectID id = object.getID();
    objectManagerFor(id).releaseReadOnly(object);
  }

  public void setGarbageCollector(final GarbageCollector newCollector) {
    if (this.collector != null) {
      this.collector.stop();
    }
    this.collector = newCollector;

    if (!config.doGC() || config.gcThreadSleepTime() < 0) return;

    StoppableThread st = new GarbageCollectorThread(this.gcThreadGroup, "DGC", newCollector, this.config);
    st.setDaemon(true);
    newCollector.setState(st);
  }

  public synchronized void setStatsListener(ObjectManagerStatsListener listener) {
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].setStatsListener(listener);
    }
  }

  public synchronized void start() {
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].start();
    }
  }

  public synchronized void stop() {
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].stop();
    }
  }

  int getPendingSize() {
    int pendingSize = 0;
    for (int i = 0; i < objectManagers.length; i++) {
      pendingSize += objectManagers[i].getPendingSize();
    }
    return pendingSize;
  }

  private void checkAndNotifyGC() {
    if (getCheckedOutCount() == 0) {
      logger.info("Notifying GC : pending = " + getPendingSize() + " checkedOutCount = " + getCheckedOutCount());
      collector.notifyReadyToGC();
    }
  }

  public synchronized void waitUntilReadyToGC() {
    checkAndNotifyGC();
    txnObjectMgr.recallAllCheckedoutObject();
    int count = 0;
    while (!collector.isPaused()) {
      if (count++ > 2) {
        logger.warn("Still waiting for object to be checked back in. collector state is not paused. checkout count = "
                    + getCheckedOutCount());
      }
      try {
        this.wait(10000);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  public ManagedObject getObjectByID(ObjectID id) {
    return objectManagerFor(id).getObjectByID(id);
  }

  private static final class StripedObjectManagerResultsContext implements ObjectManagerResultsContext {

    private final Counter                     stripedCount;

    private final ObjectIDSet                 lookupIDs;

    private final ObjectManagerResultsContext context;

    private final ObjectManagerLookupResults  resultsContext;

    public StripedObjectManagerResultsContext(Counter stripedCount, ObjectManagerLookupResults resultsContext,
                                              ObjectIDSet lookupIDs, ObjectManagerResultsContext context) {
      this.stripedCount = stripedCount;
      this.lookupIDs = lookupIDs;
      this.context = context;
      this.resultsContext = resultsContext;
    }

    public ObjectIDSet getLookupIDs() {
      return lookupIDs;
    }

    public ObjectIDSet getNewObjectIDs() {
      return context.getNewObjectIDs();
    }

    public void missingObject(ObjectID oid) {
      context.missingObject(oid);
    }

    public void setResults(ObjectManagerLookupResults results) {
      synchronized (stripedCount) {
        stripedCount.decrement();

        resultsContext.getObjects().putAll(results.getObjects());
        resultsContext.getLookupPendingObjectIDs().addAll(results.getLookupPendingObjectIDs());
      }
      if (stripedCount.get() < 1) {
        context.setResults(resultsContext);
      }
    }

    public boolean updateStats() {
      return context.updateStats();
    }

  }

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    collector.changed(changedObject, oldReference, newReference);
  }

  public int getLiveObjectCount() {
    int liveObject = 0;
    for (int i = 0; i < objectManagers.length; i++) {
      liveObject += objectManagers[i].getLiveObjectCount();
    }
    return liveObject;
  }

  public Iterator getRootNames() {
    Set rootNames = new HashSet();
    for (int i = 0; i < objectManagers.length; i++) {
      rootNames.addAll(objectManagers[i].getRootNamesSet());
    }
    return rootNames.iterator();
  }

  public void setTransactionalObjectManager(TransactionalObjectManager txnObjectManager) {
    this.txnObjectMgr = txnObjectManager;
  }

  public ManagedObjectFacade lookupFacade(ObjectID id, int limit) throws NoSuchObjectException {
    return objectManagerFor(id).lookupFacade(id, limit);
  }

  public void evictCache(CacheStats stat) {
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].evictCache(stat);
    }
  }

  public String dump() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].dump();
    }
    return sb.toString();
  }

  public void dump(Writer writer) {
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].dump(writer);
    }
  }

  public void dumpToLogger() {
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].dumpToLogger();
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    for (int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].prettyPrint(out);
    }
    return out;
  }

}
