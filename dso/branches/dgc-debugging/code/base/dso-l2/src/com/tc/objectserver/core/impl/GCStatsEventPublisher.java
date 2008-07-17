/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.GCStatsEventListener;
import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.impl.GCStatsImpl;
import com.tc.stats.LossyStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class GCStatsEventPublisher extends AbstractGarbageCollectorEventListener {

  private Map              gcStatsMap            = new HashMap();

  private List             gcStatsEventListeners = new CopyOnWriteArrayList();

  private final LossyStack gcHistory             = new LossyStack(1500);

  public void addListener(GCStatsEventListener listener) {
    gcStatsEventListeners.add(listener);
  }
  
  public GCStats[] getGarbageCollectorStats() {
    return (GCStats[]) gcHistory.toArray(new GCStats[gcHistory.depth()]);
  }

  public void garbageCollectorStart(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    if (info.isYoungGen()) {
      gcStats.markYoungGen();
    } else {
      gcStats.markFullGen();
    }
    gcStats.setStartTime(info.getStartTime());
    fireGCStatsEvent(gcStats);
  }

  public void garbageCollectorMark(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setBeginObjectCount(info.getBeginObjectCount());
    gcStats.setMarkState();
    fireGCStatsEvent(gcStats);
  }

  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setMarkStageTime(info.getMarkStageTime());
    gcStats.setPauseState();
    fireGCStatsEvent(gcStats);
    
  }

  public void garbageCollectorDeleting(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setCandidateGarbageCount(info.getCandidateGarbageCount());
    gcStats.setActualGarbageCount(info.getActualGarbageCount());
    gcStats.setPausedStageTime(info.getPauseStageTime());
    gcStats.setMarkCompleteState();
    fireGCStatsEvent(gcStats);
    
  }

  public void garbageCollectorDelete(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setDeleteState();
    fireGCStatsEvent(gcStats);
  }

  
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setDeleteStageTime(info.getDeleteStageTime());
    gcStats.setElapsedTime(info.getElapsedTime());
    push(gcStats);
    gcStatsMap.remove(info.getIteration());
  }
  
  private GCStatsImpl getGCStats(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = null;
    int iteration = info.getIteration();
    if((gcStats = (GCStatsImpl)gcStatsMap.get(iteration)) == null) {
      gcStats = new GCStatsImpl(iteration);
      gcStatsMap.put(iteration, gcStats);
    } 
    return gcStats;
  }
  
  private void push(Object obj) {
    gcHistory.push(obj);
  }

  public void fireGCStatsEvent(GCStats gcStats) {
    for (Iterator iter = gcStatsEventListeners.iterator(); iter.hasNext();) {
      GCStatsEventListener listener = (GCStatsEventListener) iter.next();
      listener.update(gcStats);
    }
  }

}
