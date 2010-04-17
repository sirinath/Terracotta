/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.OrderedGroupIDs;
import com.tc.object.ClientIDProvider;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.util.runtime.ThreadIDManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClientLockManagerGroupImpl implements ClientLockManager {
  private final Map<GroupID, ClientLockManager> lockManagers;
  private final LockDistributionStrategy        distribution;

  public ClientLockManagerGroupImpl(final TCLogger logger, final ClientIDProvider clientIdProvider,
                                    final OrderedGroupIDs groups, final LockDistributionStrategy lockDistribution,
                                    final SessionManager sessionManager, final ThreadIDManager threadManager,
                                    final LockRequestMessageFactory messageFactory,
                                    final ClientGlobalTransactionManager globalTxManager,
                                    final ClientLockManagerConfig config, final ClientLockStatManager statManager) {
    this.distribution = lockDistribution;
    this.lockManagers = new HashMap<GroupID, ClientLockManager>();

    for (final GroupID g : groups.getGroupIDs()) {
      this.lockManagers.put(g, new ClientLockManagerImpl(logger, sessionManager,
                                                         new RemoteLockManagerImpl(clientIdProvider, g, messageFactory,
                                                                                   globalTxManager, statManager),
                                                         threadManager, config, statManager));
    }
  }

  private ClientLockManager getClientLockManagerFor(final LockID lock) {
    return this.lockManagers.get(this.distribution.getGroupIDFor(lock));
  }

  private ClientLockManager getClientLockManagerFor(final GroupID group) {
    return this.lockManagers.get(group);
  }

  public void lock(final LockID lock, final LockLevel level) {
    getClientLockManagerFor(lock).lock(lock, level);
  }

  public boolean tryLock(final LockID lock, final LockLevel level) {
    return getClientLockManagerFor(lock).tryLock(lock, level);
  }

  public boolean tryLock(final LockID lock, final LockLevel level, final long timeout) throws InterruptedException {
    return getClientLockManagerFor(lock).tryLock(lock, level, timeout);
  }

  public void lockInterruptibly(final LockID lock, final LockLevel level) throws InterruptedException {
    getClientLockManagerFor(lock).lockInterruptibly(lock, level);
  }

  public void unlock(final LockID lock, final LockLevel level) {
    getClientLockManagerFor(lock).unlock(lock, level);
  }

  public Notify notify(final LockID lock, final Object waitObject) {
    return getClientLockManagerFor(lock).notify(lock, null);
  }

  public Notify notifyAll(final LockID lock, final Object waitObject) {
    return getClientLockManagerFor(lock).notifyAll(lock, null);
  }

  public void wait(final LockID lock, final Object waitObject) throws InterruptedException {
    getClientLockManagerFor(lock).wait(lock, waitObject);
  }

  public void wait(final LockID lock, final Object waitObject, final long timeout) throws InterruptedException {
    getClientLockManagerFor(lock).wait(lock, waitObject, timeout);
  }

  public boolean isLocked(final LockID lock, final LockLevel level) {
    return getClientLockManagerFor(lock).isLocked(lock, level);
  }

  public boolean isLockedByCurrentThread(final LockID lock, final LockLevel level) {
    return getClientLockManagerFor(lock).isLockedByCurrentThread(lock, level);
  }

  public boolean isLockedByCurrentThread(final LockLevel level) {
    for (final ClientLockManager clm : this.lockManagers.values()) {
      if (clm.isLockedByCurrentThread(level)) { return true; }
    }
    return false;
  }

  public int localHoldCount(final LockID lock, final LockLevel level) {
    return getClientLockManagerFor(lock).localHoldCount(lock, level);
  }

  public int globalHoldCount(final LockID lock, final LockLevel level) {
    return getClientLockManagerFor(lock).globalHoldCount(lock, level);
  }

  public int globalPendingCount(final LockID lock) {
    return getClientLockManagerFor(lock).globalPendingCount(lock);
  }

  public int globalWaitingCount(final LockID lock) {
    return getClientLockManagerFor(lock).globalWaitingCount(lock);
  }

  public void notified(final LockID lock, final ThreadID thread) {
    getClientLockManagerFor(lock).notified(lock, thread);
  }

  public void recall(final LockID lock, final ServerLockLevel level, final int lease) {
    getClientLockManagerFor(lock).recall(lock, level, lease);
  }

  public void award(final NodeID node, final SessionID session, final LockID lock, final ThreadID thread,
                    final ServerLockLevel level) {
    getClientLockManagerFor(lock).award(node, session, lock, thread, level);
  }

  public void refuse(final NodeID node, final SessionID session, final LockID lock, final ThreadID thread,
                     final ServerLockLevel level) {
    getClientLockManagerFor(lock).refuse(node, session, lock, thread, level);
  }

  public void info(final LockID lock, final ThreadID requestor,
                   final Collection<ClientServerExchangeLockContext> contexts) {
    getClientLockManagerFor(lock).info(lock, requestor, contexts);
  }

  public void pinLock(final LockID lock) {
    getClientLockManagerFor(lock).pinLock(lock);
  }

  public void unpinLock(final LockID lock) {
    getClientLockManagerFor(lock).unpinLock(lock);
  }

  public LockID generateLockIdentifier(final String str) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  public LockID generateLockIdentifier(final Object obj) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  public LockID generateLockIdentifier(final Object obj, final String field) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    getClientLockManagerFor((GroupID) remoteNode).initializeHandshake(thisNode, remoteNode, handshakeMessage);
  }

  public void pause(final NodeID remoteNode, final int disconnected) {
    if (remoteNode.equals(GroupID.ALL_GROUPS)) {
      for (final ClientLockManager clm : this.lockManagers.values()) {
        clm.pause(remoteNode, disconnected);
      }
    } else {
      final ClientLockManager clm = getClientLockManagerFor((GroupID) remoteNode);
      if (clm != null) {
        clm.pause(remoteNode, disconnected);
      }
    }
  }

  public void unpause(final NodeID remoteNode, final int disconnected) {
    if (remoteNode.equals(GroupID.ALL_GROUPS)) {
      for (final ClientLockManager clm : this.lockManagers.values()) {
        clm.unpause(remoteNode, disconnected);
      }
    } else {
      final ClientLockManager clm = getClientLockManagerFor((GroupID) remoteNode);
      if (clm != null) {
        clm.unpause(remoteNode, disconnected);
      }
    }
  }

  public void shutdown() {
    for (final ClientLockManager clm : this.lockManagers.values()) {
      clm.shutdown();
    }
  }

  public void dumpToLogger() {
    for (final ClientLockManager clm : this.lockManagers.values()) {
      clm.dumpToLogger();
    }
  }

  public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
    final Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
    for (final ClientLockManager clm : this.lockManagers.values()) {
      contexts.addAll(clm.getAllLockContexts());
    }
    return contexts;
  }
}
