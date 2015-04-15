/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.impl.GarbageCollectionID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;
import com.tc.util.UUID;
import com.tc.util.concurrent.LifeCycleState;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

final class MarkAndSweepGCAlgorithm {

  private final GCHook                         gcHook;
  private final long                           gcIteration;
  private final GarbageCollector               collector;
  private final GarbageCollectionInfoPublisher gcPublisher;
  private final LifeCycleState                 gcState;
  private final String                         uuid = UUID.getUUID().toString();

  public MarkAndSweepGCAlgorithm(GarbageCollector collector, GCHook gcHook, GarbageCollectionInfoPublisher gcPublisher,
                                 LifeCycleState gcState, long gcIteration) {
    this.collector = collector;
    this.gcHook = gcHook;
    this.gcPublisher = gcPublisher;
    this.gcState = gcState;
    this.gcIteration = gcIteration;
  }

  void doGC() {
    this.collector.waitToStartGC();

    GarbageCollectionID gcID = new GarbageCollectionID(gcIteration, uuid);
    GarbageCollectionInfo gcInfo = gcHook.createGCInfo(gcID);
    long startMillis = System.currentTimeMillis();
    gcInfo.setStartTime(startMillis);
    gcPublisher.fireGCStartEvent(gcInfo);

    // NOTE:It is important to set this reference collector before getting the roots ID and all object ids
    gcHook.startMonitoringReferenceChanges();

    final ObjectIDSet candidateIDs = gcHook.getGCCandidates();
    final Set rootIDs = gcHook.getRootObjectIDs(candidateIDs);

    gcInfo.setBeginObjectCount(candidateIDs.size());
    gcPublisher.fireGCMarkEvent(gcInfo);

    if (gcState.isStopRequested()) { return; }

    ObjectIDSet gcResults = collect(gcHook.getCollectCycleFilter(candidateIDs), rootIDs, candidateIDs, gcState);
    gcInfo.setPreRescueCount(gcResults.size());
    gcPublisher.fireGCMarkResultsEvent(gcInfo);

    if (gcState.isStopRequested()) { return; }

    long startRescue1 = System.currentTimeMillis();
    gcResults = rescue(gcResults);
    long rescue1Time = System.currentTimeMillis() - startRescue1;
    gcInfo.setRescue1Time(rescue1Time);
    gcInfo.setRescue1Count(gcResults.size());
    gcInfo.setMarkStageTime(System.currentTimeMillis() - startMillis);
    gcPublisher.fireGCRescue1CompleteEvent(gcInfo);

    if (gcResults.isEmpty()) {
      // No garbage, short circuit DGC cycle, don't pass objectMgr etc.
      gcHook.stopMonitoringReferenceChanges();
      collector.notifyGCComplete();
      shortCircuitGCComplete(gcInfo);
      return;
    }

    gcPublisher.fireGCPausingEvent(gcInfo);
    collector.requestGCPause();

    if (gcState.isStopRequested()) { return; }

    gcHook.waitUntilReadyToGC();

    if (gcState.isStopRequested()) { return; }

    long pauseStartMillis = System.currentTimeMillis();
    gcPublisher.fireGCPausedEvent(gcInfo);

    gcInfo.setCandidateGarbageCount(gcResults.size());
    gcPublisher.fireGCRescue2StartEvent(gcInfo);
    long startRescue2 = System.currentTimeMillis();
    ObjectIDSet toDelete = ObjectIDSet.unmodifiableObjectIDSet(rescue(new BitSetObjectIDSet(gcResults)));
    long rescue2Time = System.currentTimeMillis() - startRescue2;
    gcInfo.setRescue2Time(rescue2Time);

    if (gcState.isStopRequested()) { return; }

    gcHook.stopMonitoringReferenceChanges();

    long deleteStartMillis = System.currentTimeMillis();
    gcInfo.setPausedStageTime(deleteStartMillis - pauseStartMillis);
    gcInfo.setActualGarbageCount(toDelete.size());
    gcPublisher.fireGCMarkCompleteEvent(gcInfo);

    // Delete Garbage
    collector.deleteGarbage(new DGCResultContext(toDelete, gcInfo));

    long endMillis = System.currentTimeMillis();
    gcInfo.setTotalMarkCycleTime(endMillis - gcInfo.getStartTime());
    gcInfo.setEndObjectCount(gcHook.getLiveObjectCount());
    gcPublisher.fireGCCycleCompletedEvent(gcInfo, toDelete);
  }

  private void shortCircuitGCComplete(GarbageCollectionInfo gcInfo) {
    gcInfo.setCandidateGarbageCount(0);
    gcInfo.setRescue1Time(0);
    gcInfo.setRescue2Time(0);
    gcInfo.setPausedStageTime(0);
    gcInfo.setActualGarbageCount(0);
    gcInfo.setEndObjectCount(gcHook.getLiveObjectCount());
    long elapsedTime = System.currentTimeMillis() - gcInfo.getStartTime();
    gcInfo.setTotalMarkCycleTime(elapsedTime);
    gcInfo.setElapsedTime(elapsedTime);
    gcPublisher.fireGCCycleCompletedEvent(gcInfo, new BitSetObjectIDSet());
    gcPublisher.fireGCCompletedEvent(gcInfo);
  }

  public ObjectIDSet collect(Filter filter, Collection rootIds, ObjectIDSet managedObjectIds,
                             LifeCycleState lifeCycleState) {
    long start = System.currentTimeMillis();
    logstart_collect(rootIds, managedObjectIds);

    for (Iterator i = rootIds.iterator(); i.hasNext() && !managedObjectIds.isEmpty();) {
      ObjectID rootId = (ObjectID) i.next();
      managedObjectIds.remove(rootId);
      if (lifeCycleState.isStopRequested()) return TCCollections.EMPTY_OBJECT_ID_SET;
      collectRoot(filter, rootId, managedObjectIds, lifeCycleState);
    }

    profile_collect(start);

    return managedObjectIds;
  }

  private void collectRoot(Filter filter, ObjectID rootId, Set managedObjectIds, LifeCycleState lifeCycleState) {
    Set<ObjectID> toBeVisited = new BitSetObjectIDSet();
    toBeVisited.add(rootId);

    while (!toBeVisited.isEmpty() && !managedObjectIds.isEmpty()) {

      for (Iterator i = new BitSetObjectIDSet(toBeVisited).iterator(); i.hasNext() && !managedObjectIds.isEmpty();) {
        ObjectID id = (ObjectID) i.next();
        if (lifeCycleState.isStopRequested()) return;
        Set<ObjectID> references = gcHook.getObjectReferencesFrom(id);
        toBeVisited.remove(id);

        for (final ObjectID mid : references) {
          if (mid == null) {
            // see CDV-765
            MarkAndSweepGarbageCollector.logger.error("null value returned from getObjectReferences() on " + id);
            continue;
          }
          if (mid.isNull() || !managedObjectIds.contains(mid)) continue;
          if (filter.shouldVisit(mid)) toBeVisited.add(mid);
          managedObjectIds.remove(mid);
        }
      }
    }
  }

  private ObjectIDSet rescue(final ObjectIDSet gcResults) {
    Set<ObjectID> rescueIds = gcHook.getRescueIDs();
    rescueIds.retainAll(gcResults);

    Filter rescueFilter = new SelectiveFilter(gcResults);
    return collect(rescueFilter, rescueIds, gcResults, gcState);
  }

  private void logstart_collect(Collection rootIds, Set managedObjectIds) {
    if (MarkAndSweepGarbageCollector.logger.isDebugEnabled()) MarkAndSweepGarbageCollector.logger
        .debug("collect(): rootIds=" + rootIds.size() + ", managedObjectIds=" + managedObjectIds.size());
  }

  private void profile_collect(long start) {
    if (MarkAndSweepGarbageCollector.logger.isDebugEnabled()) MarkAndSweepGarbageCollector.logger
        .debug("collect: " + (System.currentTimeMillis() - start) + " ms.");
  }

}