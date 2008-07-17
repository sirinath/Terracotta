/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.api.GCStats;
import com.tc.util.State;

import java.io.Serializable;

public class GCStatsImpl implements GCStats, Serializable {
  private static final long     serialVersionUID      = -4177683133067698672L;
  private static final TCLogger logger                = TCLogging.getLogger(GCStatsImpl.class);
  private static final long     NOT_INITIALIZED       = -1L;
  private static final String   YOUNG_GENERATION      = "Young";
  private static final String   FULL_GENERATION       = "Full";
  private final int             number;
  private long                  startTime             = NOT_INITIALIZED;
  private long                  elapsedTime           = NOT_INITIALIZED;
  private long                  beginObjectCount      = NOT_INITIALIZED;
  private long                  candidateGarbageCount = NOT_INITIALIZED;
  private long                  actualGarbageCount    = NOT_INITIALIZED;
  private long                  pausedStageTime       = NOT_INITIALIZED;
  private long                  deleteStageTime       = NOT_INITIALIZED;
  private State                 state;
  private boolean               young;

  public GCStatsImpl(int number, State aState, boolean aYoung) {
    this.number = number;
    this.state = aState;
    this.young = aYoung;
  }

  public synchronized boolean isYoung() {
    return young;
  }

  public int getIteration() {
    return this.number;
  }

  public synchronized long getStartTime() {
    return this.startTime;
  }

  public synchronized long getElapsedTime() {
    return this.elapsedTime;
  }

  public synchronized long getBeginObjectCount() {
    return this.beginObjectCount;
  }

  public synchronized long getCandidateGarbageCount() {
    return this.candidateGarbageCount;
  }

  public synchronized long getActualGarbageCount() {
    return this.actualGarbageCount;
  }

  public synchronized long getPausedStageTime() {
    return this.pausedStageTime;
  }

  public synchronized long getDeleteStageTime() {
    return this.deleteStageTime;
  }

  public synchronized String getStatus() {
    return state.getName();
  }

  public synchronized String getType() {
    return young ? YOUNG_GENERATION : FULL_GENERATION;
  }

  public synchronized void setState(State state) {
    this.state = state;
  }

  public synchronized void setActualGarbageCount(long count) {
    validate(count);
    this.actualGarbageCount = count;
  }

  public synchronized void setBeginObjectCount(long count) {
    validate(count);
    this.beginObjectCount = count;
  }

  public synchronized void setCandidateGarbageCount(long count) {
    validate(count);
    this.candidateGarbageCount = count;
  }

  public synchronized void setPausedStageTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC PausedStageTime to 0");
      time = 0;
    }
    this.pausedStageTime = time;
  }

  public synchronized void setDeleteStageTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC DeleteStageTime to 0");
      time = 0;
    }
    this.deleteStageTime = time;
  }

  public synchronized void setElapsedTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC ElapsedTime to 0");
      time = 0;
    }
    this.elapsedTime = time;
  }

  public synchronized void setStartTime(long time) {
    validate(time);
    this.startTime = time;
  }

  public synchronized void markYoungGen() {
    this.young = true;
  }

  public synchronized void markOldGen() {
    this.young = false;
  }

  private void validate(long value) {
    if (value < 0L) { throw new IllegalArgumentException("Value must be greater than or equal to zero"); }
  }


  protected void initialize(long aStartTime, long aElapsedTime, long aBeginObjectCount, long aCandidateGarbageCount,
                            long aActualGarbageCount, long aPausedStageTime, long aDeleteStageTime) {
    this.startTime = aStartTime;
    this.elapsedTime = aElapsedTime;
    this.beginObjectCount = aBeginObjectCount;
    this.candidateGarbageCount = aCandidateGarbageCount;
    this.actualGarbageCount = aActualGarbageCount;
    this.pausedStageTime = aPausedStageTime;
    this.deleteStageTime = aDeleteStageTime;
  }

  public String toString() {
    return "GCStats[ iteration: " + getIteration() + " type: " + getType() + " status: " + getStatus()
           + " ] : startTime = " + this.startTime + "ms; elapsedTime = " + this.elapsedTime + "ms; pausedStageTime = "
           + this.pausedStageTime + "ms; deleteStageTime = " + this.deleteStageTime + "ms; beginObjectCount = "
           + this.beginObjectCount + "; candidateGarbageCount = " + this.candidateGarbageCount
           + "; actualGarbageCount = " + this.actualGarbageCount;
  }

}
