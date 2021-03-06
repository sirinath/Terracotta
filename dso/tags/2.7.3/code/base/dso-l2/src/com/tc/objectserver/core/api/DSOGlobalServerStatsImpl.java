/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.objectserver.impl.ObjectManagerStatsImpl;
import com.tc.stats.counter.sampled.SampledCounter;

public class DSOGlobalServerStatsImpl implements DSOGlobalServerStats {

  private final SampledCounter         faultCounter;
  private final SampledCounter         flushCounter;
  private final SampledCounter         txnCounter;
  private final ObjectManagerStatsImpl objMgrStats;

  private final SampledCounter         broadcastCounter;
  private final SampledCounter         changesCounter;
  private final SampledCounter         l2FaultFromDiskCounter;
  private final SampledCounter         time2FaultFromDisk;
  private final SampledCounter         time2Add2ObjMgr;

  public DSOGlobalServerStatsImpl(SampledCounter flushCounter, SampledCounter faultCounter, SampledCounter txnCounter,
                                  ObjectManagerStatsImpl objMgrStats, SampledCounter broadcastCounter,
                                  SampledCounter changesCounter, SampledCounter l2FaultFromDiskCounter,
                                  SampledCounter time2FaultFromDisk, SampledCounter time2Add2ObjMgr) {
    this.flushCounter = flushCounter;
    this.faultCounter = faultCounter;
    this.txnCounter = txnCounter;
    this.objMgrStats = objMgrStats;
    this.broadcastCounter = broadcastCounter;
    this.changesCounter = changesCounter;
    this.l2FaultFromDiskCounter = l2FaultFromDiskCounter;
    this.time2FaultFromDisk = time2FaultFromDisk;
    this.time2Add2ObjMgr = time2Add2ObjMgr;
  }

  public SampledCounter getObjectFlushCounter() {
    return this.flushCounter;
  }

  public SampledCounter getObjectFaultCounter() {
    return this.faultCounter;
  }

  public ObjectManagerStats getObjectManagerStats() {
    return this.objMgrStats;
  }

  public SampledCounter getTransactionCounter() {
    return this.txnCounter;
  }

  public SampledCounter getBroadcastCounter() {
    return broadcastCounter;
  }

  public SampledCounter getChangesCounter() {
    return changesCounter;
  }

  public SampledCounter getL2FaultFromDiskCounter() {
    return l2FaultFromDiskCounter;
  }

  public SampledCounter getTime2FaultFromDisk() {
    return time2FaultFromDisk;
  }

  public SampledCounter getTime2Add2ObjectMgr() {
    return time2Add2ObjMgr;
  }
}
