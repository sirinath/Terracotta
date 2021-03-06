/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.objectserver.core.impl.GarbageCollectionID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.Assert;

public class GCLogger {
  private final TCLogger logger;
  private final boolean  verboseGC;
  private final String   prefix;

  public GCLogger(TCLogger logger, boolean verboseGC) {
    this("DGC", logger, verboseGC);
  }

  public GCLogger(String prefix, TCLogger logger, boolean verboseGC) {
    Assert.assertNotNull(logger);
    this.logger = logger;
    this.verboseGC = verboseGC;
    this.prefix = prefix;
  }

  public void log_start(GarbageCollectionID id, boolean fullGC) {
    if (verboseGC()) logGC(id, (fullGC ? "Full GC" : "YoungGen GC") + " start ");
  }

  public void log_markStart(GarbageCollectionID id, long size) {
    if (verboseGC()) logGC(id, "pre-GC managed id count: " + size);
  }

  public void log_markResults(GarbageCollectionID id, long size) {
    if (verboseGC()) logGC(id, "pre-rescue GC results: " + size);
  }

  public void log_quiescing(GarbageCollectionID id) {
    if (verboseGC()) logGC(id, "quiescing...");
  }

  public void log_paused(GarbageCollectionID id) {
    if (verboseGC()) logGC(id, "paused.");
  }

  public void log_rescue_complete(GarbageCollectionID id, int pass, long count) {
    if (verboseGC()) logGC(id, "rescue pass " + pass + " completed. gc candidates = " + count + " objects...");
  }

  public void log_rescue_start(GarbageCollectionID id, int pass, long count) {
    if (verboseGC()) logGC(id, "rescue pass " + pass + " on " + count + " objects...");
  }

  public void log_markComplete(GarbageCollectionID id, long count) {
    if (verboseGC()) logGC(id, "deleting garbage: " + count + " objects");
  }

  public void log_deleteStart(GarbageCollectionID id, long toDeleteSize) {
    if (verboseGC()) logGC(id, "delete start : " + toDeleteSize + " objects");
  }

  public void log_cycleComplete(GarbageCollectionID id, GarbageCollectionInfo gcInfo) {
    if (verboseGC()) {
      logGC(id, "notifying gc complete...");
      logGC(id, "rescue 1 time   : " + gcInfo.getRescue1Time() + " ms.");
      logGC(id, "rescue 2 time   : " + gcInfo.getRescue2Time() + " ms.");
      logGC(id, "paused gc time  : " + gcInfo.getPausedStageTime() + " ms.");
      logGC(id, "delete in-memory garbage time  : " + gcInfo.getDeleteStageTime() + " ms.");
      logGC(id, "total mark cycle time   : " + gcInfo.getTotalMarkCycleTime() + " ms.");
      logGC(id, "" + (gcInfo.isFullGC() ? "Full GC" : "YoungGen GC") + " STOP ");
    } else {
      logGC(id, "complete : " + gcInfo);
    }
  }

  public void log_complete(GarbageCollectionID id, long deleteGarbageSize, long elapsed) {
    if (verboseGC()) {
      logGC(id, "delete completed : removed " + deleteGarbageSize + " objects in " + elapsed + " ms.");
    }
  }

  public void log_canceled(GarbageCollectionID id) {
    if (verboseGC()) {
      logGC(id, "canceled");
    }
  }

  public boolean verboseGC() {
    return verboseGC;
  }

  private void logGC(GarbageCollectionID id, String msg) {
    logger.info(prefix + "[ " + id.toLong() + " ] " + msg);
  }
}
