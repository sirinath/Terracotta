/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.locks.LockLevel;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;

/**
 * @author steve
 */
public class LockResponseHandler extends AbstractEventHandler {
  private static final TCLogger logger = TCLogging.getLogger(LockResponseHandler.class);
  private ClientLockManager     lockManager;
  private final SessionManager sessionManager;

  public LockResponseHandler(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  public void handleEvent(EventContext context) {
    final LockResponseMessage msg = (LockResponseMessage) context;
    final SessionID sessionID = msg.getLocalSessionID();
    if(!sessionManager.isCurrentSession(msg.getSourceNodeID() ,sessionID)) {
      logger.warn("Ignoring " + msg + " from a previous session:" + sessionID + ", " + sessionManager);
      return;
    }
    
    switch (msg.getResponseType()) {
      case AWARD:
        lockManager.awardLock(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getThreadID(), LockLevel.toLegacyInt(msg.getLockLevel()));
        return;
      case RECALL:
        lockManager.recall(msg.getLockID(), msg.getThreadID(), LockLevel.toLegacyInt(msg.getLockLevel()), 0);
        return;
      case RECALL_WITH_TIMEOUT:
        lockManager.recall(msg.getLockID(), msg.getThreadID(), LockLevel.toLegacyInt(msg.getLockLevel()), msg.getAwardLeaseTime());
        return;
      case REFUSE:
        lockManager.cannotAwardLock(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getThreadID(), LockLevel.toLegacyInt(msg.getLockLevel()));
        return;
      case WAIT_TIMEOUT:
        lockManager.waitTimedOut(msg.getLockID(), msg.getThreadID());
        return;
      case INFO:
        lockManager.queryLockCommit(msg.getThreadID(), msg.getGlobalLockInfo());
        return;
    }
    logger.error("Unknown lock response message: " + msg);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.lockManager = ccc.getLockManager();
  }

}
