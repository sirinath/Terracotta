/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.OrderedGroupIDs;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.util.runtime.ThreadIDManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClientLockManagerGroupImpl implements ClientLockManager {
  private final Map<GroupID, ClientLockManager> lockManagers;
  private final LockDistributionStrategy        distribution;

  public ClientLockManagerGroupImpl(TCLogger logger, OrderedGroupIDs groups, LockDistributionStrategy lockDistribution,
                                    SessionManager sessionManager, ThreadIDManager threadManager, ClientTransactionManager transactionManager,
                                    LockRequestMessageFactory messageFactory, ClientGlobalTransactionManager globalTxManager) {
    distribution = lockDistribution;
    lockManagers = new HashMap<GroupID, ClientLockManager>();

    for (GroupID g : groups.getGroupIDs()) {
      lockManagers.put(g, new ClientLockManagerImpl(logger, sessionManager, new RemoteLockManagerImpl(g, messageFactory, globalTxManager), threadManager, transactionManager));
    }
  }
  
  private ClientLockManager getClientLockManagerFor(LockID lock) {
    return lockManagers.get(distribution.getGroupIDFor(lock));
  }
  
  private ClientLockManager getClientLockManagerFor(GroupID group) {
    return lockManagers.get(group);
  }
  
  public void lock(LockID lock, LockLevel level) {
    getClientLockManagerFor(lock).lock(lock, level);
  }

  public boolean tryLock(LockID lock, LockLevel level) {
    return getClientLockManagerFor(lock).tryLock(lock, level);
  }
  
  public boolean tryLock(LockID lock, LockLevel level, long timeout) throws InterruptedException {
    return getClientLockManagerFor(lock).tryLock(lock, level, timeout);
  }

  public void lockInterruptibly(LockID lock, LockLevel level) throws InterruptedException {
    getClientLockManagerFor(lock).lockInterruptibly(lock, level);
  }

  public void unlock(LockID lock, LockLevel level) {
    getClientLockManagerFor(lock).unlock(lock, level);
  }

  public void notify(LockID lock) {
    getClientLockManagerFor(lock).notify(lock);
  }
  
  public void notifyAll(LockID lock) {
    getClientLockManagerFor(lock).notifyAll(lock);
  }
  
  public void wait(LockID lock) throws InterruptedException {
    getClientLockManagerFor(lock).wait(lock);
  }
  
  public void wait(LockID lock, long timeout) throws InterruptedException {
    getClientLockManagerFor(lock).wait(lock, timeout);
  }

  public void wait(LockID lock, WaitListener listener) throws InterruptedException {
    getClientLockManagerFor(lock).wait(lock, listener);
  }

  public void wait(LockID lock, WaitListener listener, long timeout) throws InterruptedException {
    getClientLockManagerFor(lock).wait(lock, listener, timeout);
  }

  public boolean isLocked(LockID lock, LockLevel level) {
    return getClientLockManagerFor(lock).isLocked(lock, level);
  }
  
  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    return getClientLockManagerFor(lock).isLockedByCurrentThread(lock, level);
  }

  public int localHoldCount(LockID lock, LockLevel level) {
    return getClientLockManagerFor(lock).localHoldCount(lock, level);
  }
  
  public int globalHoldCount(LockID lock, LockLevel level) {
    return getClientLockManagerFor(lock).globalHoldCount(lock, level);
  }
  
  public int globalPendingCount(LockID lock) {
    return getClientLockManagerFor(lock).globalPendingCount(lock);
  }
  
  public int globalWaitingCount(LockID lock) {
    return getClientLockManagerFor(lock).globalWaitingCount(lock);
  }

  public void notified(LockID lock, ThreadID thread) {
    getClientLockManagerFor(lock).notified(lock, thread);
  }
  
  public void recall(LockID lock, ServerLockLevel level, int lease) {
    getClientLockManagerFor(lock).recall(lock, level, lease);
  }

  public void award(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    getClientLockManagerFor(lock).award(node, session, lock, thread, level);
  }
  
  public void refuse(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    getClientLockManagerFor(lock).refuse(node, session, lock, thread, level);
  }
  
  public void info(ThreadID requestor, Collection<ClientServerExchangeLockContext> contexts) {
    throw new AssertionError();
  }
  
  public LockID generateLockIdentifier(String str) {
    throw new AssertionError();
  }
  public LockID generateLockIdentifier(Object obj) {
    throw new AssertionError();
  }
  public LockID generateLockIdentifier(Object obj, String field) {
    throw new AssertionError();
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    getClientLockManagerFor((GroupID) remoteNode).initializeHandshake(thisNode, remoteNode, handshakeMessage);
  }

  public void pause(NodeID remoteNode, int disconnected) {
    if (remoteNode.equals(GroupID.ALL_GROUPS)) {
      for (ClientLockManager clm : lockManagers.values()) {
        clm.pause(remoteNode, disconnected);
      }
    } else {
      ClientLockManager clm = getClientLockManagerFor((GroupID) remoteNode);
      if (clm != null) {
        clm.pause(remoteNode, disconnected);
      }
    }
  }

  public void unpause(NodeID remoteNode, int disconnected) {
    if (remoteNode.equals(GroupID.ALL_GROUPS)) {
      for (ClientLockManager clm : lockManagers.values()) {
        clm.unpause(remoteNode, disconnected);
      }
    } else {
      ClientLockManager clm = getClientLockManagerFor((GroupID) remoteNode);
      if (clm != null) {
        clm.unpause(remoteNode, disconnected);
      }
    }
  }  

  public void shutdown() {
    for (ClientLockManager clm : lockManagers.values()) {
      clm.shutdown();
    }
  }

  public String dump() {
    StringBuilder sb = new StringBuilder();
    for (ClientLockManager clm : lockManagers.values()) {
      sb.append(clm.dump());
    }
    return sb.toString();
  }

  public void dumpToLogger() {
    for (ClientLockManager clm : lockManagers.values()) {
      clm.dumpToLogger();
    }
  }

  public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
    Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
    for (ClientLockManager clm : lockManagers.values()) {
      contexts.addAll(clm.getAllLockContexts());
    }
    return contexts;
  }
}
