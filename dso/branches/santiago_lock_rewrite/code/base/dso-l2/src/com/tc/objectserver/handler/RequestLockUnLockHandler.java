/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.lockmanager.api.LockManager;

/**
 * Makes the request for a lock on behalf of a client
 * 
 * @author steve
 */
public class RequestLockUnLockHandler extends AbstractEventHandler {
  public static final Sink NULL_SINK = new NullSink();

  private LockManager      lockManager;
  private Sink             lockResponseSink;

  public void handleEvent(EventContext context) {
    LockRequestMessage lrm = (LockRequestMessage) context;

    LockID lid = lrm.getLockID();
    NodeID cid = lrm.getSourceNodeID();
    ThreadID tid = lrm.getThreadID();

    switch (lrm.getRequestType()) {
      case LOCK:
        lockManager.requestLock(lid, cid, tid, ServerLockLevel.toLegacyInt(lrm.getLockLevel()), "", lockResponseSink);
        return;
      case TRY_LOCK:
        long waitMillis = lrm.getTimeout();
        TimerSpec ts = (waitMillis < 0) ? new TimerSpec() : new TimerSpec(waitMillis);
        lockManager.tryRequestLock(lid, cid, tid, ServerLockLevel.toLegacyInt(lrm.getLockLevel()), "", ts,
                                   lockResponseSink);
        return;
      case UNLOCK:
        lockManager.unlock(lid, cid, tid);
        return;
      case WAIT:
        waitMillis = lrm.getTimeout();
        ts = (waitMillis < 0) ? new TimerSpec() : new TimerSpec(waitMillis);
        lockManager.wait(lid, cid, tid, ts, lockResponseSink);
        return;
      case RECALL_COMMIT:
        lockManager.recallCommit(lid, cid, lrm.getLockContexts(), lrm.getWaitContexts(), lrm.getPendingLockContexts(),
                                 lrm.getPendingTryLockContexts(), lockResponseSink);
        return;
      case QUERY:
        lockManager.queryLock(lid, cid, tid, lockResponseSink);
        return;
      case INTERRUPT_WAIT:
        lockManager.interrupt(lid, cid, tid);
        return;
    }
    throw new AssertionError("Unknown lock request message: " + lrm);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.lockManager = oscc.getLockManager();
    this.lockResponseSink = oscc.getStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE).getSink();
  }
}
