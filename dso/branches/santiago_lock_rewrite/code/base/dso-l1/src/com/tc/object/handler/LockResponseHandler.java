/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.impl.GlobalLockInfo;
import com.tc.object.lockmanager.impl.GlobalLockStateInfo;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author steve
 */
public class LockResponseHandler extends AbstractEventHandler {
  private static final TCLogger logger = TCLogging.getLogger(LockResponseHandler.class);
  private ClientLockManager     lockManager;
  private final SessionManager  sessionManager;

  public LockResponseHandler(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  public void handleEvent(EventContext context) {
    final LockResponseMessage msg = (LockResponseMessage) context;
    final SessionID sessionID = msg.getLocalSessionID();
    if (!sessionManager.isCurrentSession(msg.getSourceNodeID(), sessionID)) {
      logger.warn("Ignoring " + msg + " from a previous session:" + sessionID + ", " + sessionManager);
      return;
    }

    switch (msg.getResponseType()) {
      case AWARD:
        lockManager.awardLock(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getThreadID(),
                              getLevelFromMessage(msg));
        return;
      case RECALL:
        lockManager.recall(msg.getLockID(), msg.getThreadID(), getLevelFromMessage(msg), 0);
        return;
      case RECALL_WITH_TIMEOUT:
        lockManager.recall(msg.getLockID(), msg.getThreadID(), getLevelFromMessage(msg), msg.getAwardLeaseTime());
        return;
      case REFUSE:
        lockManager.cannotAwardLock(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getThreadID(),
                                    getLevelFromMessage(msg));
        return;
      case WAIT_TIMEOUT:
        lockManager.waitTimedOut(msg.getLockID(), msg.getThreadID());
        return;
      case INFO:
        lockManager.queryLockCommit(msg.getThreadID(), getGliFromMessage(msg));
        return;
    }
    logger.error("Unknown lock response message: " + msg);
  }

  private int getLevelFromMessage(final LockResponseMessage msg) {
    int level;
    level = ServerLockLevel.toLegacyInt(msg.getLockLevel());
    if (msg.getThreadID().equals(ThreadID.VM_ID)) {
      level = com.tc.object.lockmanager.api.LockLevel.makeGreedy(level);
    }
    return level;
  }

  private GlobalLockInfo getGliFromMessage(LockResponseMessage msg) {
    Collection<GlobalLockStateInfo> greedyHolders = new ArrayList<GlobalLockStateInfo>();
    Collection<GlobalLockStateInfo> holders = new ArrayList<GlobalLockStateInfo>();
    Collection<GlobalLockStateInfo> waiters = new ArrayList<GlobalLockStateInfo>();
    int queueLength = 0;
    
    for (ClientServerExchangeLockContext cselc : msg.getContexts()) {
      switch (cselc.getState().getType()) {
        case GREEDY_HOLDER:
          greedyHolders.add(new GlobalLockStateInfo(cselc.getLockID(), cselc.getNodeID(), cselc.getThreadID(), -1L, ServerLockLevel.toLegacyInt(cselc.getState().getLockLevel())));
          break;
        case HOLDER:
          holders.add(new GlobalLockStateInfo(cselc.getLockID(), cselc.getNodeID(), cselc.getThreadID(), -1L, ServerLockLevel.toLegacyInt(cselc.getState().getLockLevel())));
          break;
        case WAITER:
          waiters.add(new GlobalLockStateInfo(cselc.getLockID(), cselc.getNodeID(), cselc.getThreadID(), -1L, ServerLockLevel.toLegacyInt(cselc.getState().getLockLevel())));
          break;
        case PENDING:
        case TRY_PENDING:
          queueLength++;
      }
    }
    
    return new GlobalLockInfo(msg.getLockID(), getLevelFromMessage(msg), queueLength, greedyHolders, holders, waiters);
  }
  
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.lockManager = ccc.getLockManager();
  }

}
