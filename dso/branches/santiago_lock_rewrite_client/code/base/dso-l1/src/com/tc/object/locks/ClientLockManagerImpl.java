/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.TCLogger;
import com.tc.net.NodeID;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.util.Util;
import com.tc.util.runtime.ThreadIDManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientLockManagerImpl implements ClientLockManager {
  private static final WaitListener NULL_LISTENER = new WaitListener() {
    public void handleWaitEvent() {
      //
    }
  };

  private final ConcurrentMap<LockID, ClientLock> locks = new ConcurrentHashMap<LockID, ClientLock>();
  private final ReentrantReadWriteLock            stateGuard = new ReentrantReadWriteLock();
  private final Condition                         runningCondition = stateGuard.writeLock().newCondition();
  private State                                   state = State.RUNNING;
  
  private final RemoteLockManager                 remoteManager;
  private final ThreadIDManager                   threadManager;
  private final SessionManager                    sessionManager;
  private final ClientTransactionManager          transactionManager;
  private final TCLogger                          logger;

  private final ConcurrentMap<ThreadID, Object>   inFlightLockQueries = new ConcurrentHashMap<ThreadID, Object>();
  
  public ClientLockManagerImpl(TCLogger logger, SessionManager sessionManager, RemoteLockManager remoteManager, ThreadIDManager threadManager, ClientTransactionManager txnManager) {
    this.logger = logger;
    this.remoteManager = remoteManager;
    this.threadManager = threadManager;
    this.sessionManager = sessionManager;
    this.transactionManager = txnManager;
  }
  
  private void waitUntilRunning() {
    stateGuard.readLock().lock();
    try {
      if (state == State.RUNNING)
        return;
    } finally {
      stateGuard.readLock().unlock();
    }
    
    boolean interrupted = false;
    stateGuard.writeLock().lock();
    try {
      while (state != State.RUNNING) {
        try {
          runningCondition.await();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      stateGuard.writeLock().unlock();
    }
    
    Util.selfInterruptIfNeeded(interrupted);
  }
  
  private boolean paused() {
    stateGuard.readLock().lock();
    try {
      return state == State.PAUSED;
    } finally {
      stateGuard.readLock().unlock();
    }
  }
  
  private ClientLock getOrCreateState(LockID lock) {
    ClientLock lockState = locks.get(lock);
    if (lockState == null) {
      lockState = new SynchronizedClientLock(lock, remoteManager);
      ClientLock racer = locks.putIfAbsent(lock, lockState);
      if (racer != null) {
        return racer;
      }
    }

    return lockState;
  }
  
  private ClientLock getState(LockID lock) {
    return locks.get(lock);
  }
  
  public void award(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    if (paused() || !sessionManager.isCurrentSession(node, session)) {
      logger.warn("Ignoring lock award from a dead server :" + session + ", " + sessionManager + " : "
                       + lock + " " + thread + " " + level + " state = " + state);
      return;
    }
    
    if (ThreadID.VM_ID.equals(thread)) {
      while (true) {
        ClientLock lockState = getOrCreateState(lock);
        try {
          lockState.award(thread, level);
          return;
        } catch (GarbageLockException e) {
          // ignore
        }
      }
    } else {
      ClientLock lockState = getState(lock);
      if (lockState == null) {
        remoteManager.unlock(lock, thread, level);
      } else {
        try {
          lockState.award(thread, level);
        } catch (GarbageLockException e) {
          // ignore
        }
      }
    }
  }

  public void notified(LockID lock, ThreadID thread) {
    if (paused()) {
      logger.warn("Ignoring notified call from dead server : " + lock + ", " + thread);
      return;
    }

    final ClientLock lockState = getState(lock);
    if (lockState == null) {
      throw new AssertionError(lock);
    } else {
      try {
        lockState.notified(thread);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void recall(LockID lock, ServerLockLevel level, int lease) {
    if (paused()) {
      logger.warn("Ignoring recall request from dead server : " + lock + ", interestedLevel : " + level);
      return;
    }
    

    ClientLock lockState = getState(lock);
    if (lockState != null) {
      lockState.recall(remoteManager, level, lease);
    }
  }

  public void refuse(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    if (paused() || !sessionManager.isCurrentSession(node, session)) {
      logger.warn("Ignoring lock refuse from a dead server :" + session + ", " + sessionManager + " : "
                       + lock + " " + thread + " " + level + " state = " + state);
      return;
    }
    
    ClientLock lockState = getState(lock);
    if (lockState != null) {
      try {
        lockState.refuse(thread, level);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void info(ThreadID requestor, Collection<ClientServerExchangeLockContext> contexts) {
    Object old = inFlightLockQueries.put(requestor, contexts);
    synchronized (old) {
      old.notifyAll();
    }
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

  public int globalHoldCount(LockID lock, LockLevel level) {
    waitUntilRunning();
    
    int holdCount = 0;
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        holdCount += lockState.holdCount(level);
      } catch (GarbageLockException e) {
        continue;
      }
      break;
    }
    
    for (ClientServerExchangeLockContext cselc : queryLock(lock)) {
      if (ManagerUtil.getClientID().equals(cselc.getNodeID())) {
        continue;
      }
      
      switch (cselc.getState()) {
        case GREEDY_HOLDER_READ:
        case HOLDER_READ:
          if (level == LockLevel.READ) holdCount++;
          break;
        case GREEDY_HOLDER_WRITE:
          holdCount++;
          break;
        case HOLDER_WRITE:
          if ((level == LockLevel.WRITE) || (level == LockLevel.SYNCHRONOUS_WRITE)) holdCount++;
          break;
        default:
          break;
      }
    }

    return holdCount;
  }

  public int globalPendingCount(LockID lock) {
    waitUntilRunning();
    
    int pendingCount = 0;
    while (true) {
      ClientLock lockState = getState(lock);
      if (lockState != null) {
        try {
          pendingCount += lockState.pendingCount();
        } catch (GarbageLockException e) {
          continue;
        }
      }
      break;
    }

    for (ClientServerExchangeLockContext cselc : queryLock(lock)) {
      switch (cselc.getState()) {
        default: continue;
        case PENDING_READ:
        case PENDING_WRITE:
          pendingCount++;
      }
    }
    
    return pendingCount;
  }

  public int globalWaitingCount(LockID lock) {
    waitUntilRunning();
    
    int waiterCount = 0;
    for (ClientServerExchangeLockContext cselc : queryLock(lock)) {
      switch (cselc.getState()) {
        default: continue;
        case WAITER:
          waiterCount++;
      }
    }

    if (waiterCount > 0) {
      return waiterCount;
    }
    
    while (true) {
      ClientLock lockState = getState(lock);
      if (lockState != null) {
        try {
          return lockState.waitingCount();
        } catch (GarbageLockException e) {
          continue;
        }
      }
      break;
    }

    return 0;
  }

  public boolean isLocked(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getState(lock);
      if (lockState != null) {
        try {
          if (lockState.isLocked(level)) {
            return true;
          }
        } catch (GarbageLockException e) {
          continue;
        }
      }
      break;
    }
  
    for (ClientServerExchangeLockContext cselc : queryLock(lock)) {
      if (ManagerUtil.getClientID().equals(cselc.getNodeID())) {
        continue;
      }
      
      switch (cselc.getState()) {
        default: continue;
        
        case GREEDY_HOLDER_READ:
        case HOLDER_READ:
          if (level == LockLevel.READ) {
            return true;
          }
          break;
        case GREEDY_HOLDER_WRITE:
        case HOLDER_WRITE:
          if ((level == LockLevel.WRITE) || (level == LockLevel.SYNCHRONOUS_WRITE)) {
            return true;
          }
          break;
      }
    }
    return false;
  }

  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getState(lock);
      if (lockState != null) {
        try {
          return lockState.isLockedBy(threadManager.getThreadID(), level);
        } catch (GarbageLockException e) {
          continue;
        }
      }
      break;
    }
    return false;
  }

  public int localHoldCount(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        return lockState.holdCount(level);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void lock(LockID lock, LockLevel level) {
    if (transactionManager.isTransactionLoggingDisabled() || transactionManager.isObjectCreationInProgress()) { return; }

    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        lockState.lock(remoteManager, threadManager.getThreadID(), level);
        break;
      } catch (GarbageLockException e) {
        // ignore
      }
    }
    transactionManager.begin(lock, level);
  }

  public void lockInterruptibly(LockID lock, LockLevel level) throws InterruptedException {
    if (transactionManager.isTransactionLoggingDisabled() || transactionManager.isObjectCreationInProgress()) { return; }

    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        lockState.lockInterruptibly(remoteManager, threadManager.getThreadID(), level);
        break;
      } catch (GarbageLockException e) {
        // ignore
      }
    }
    transactionManager.begin(lock, level);
  }

  public void notify(LockID lock) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        ThreadID thread = threadManager.getThreadID();
        if (lockState.notify(remoteManager, thread)) {
          transactionManager.notify(lock, thread, false);
        }
        return;
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void notifyAll(LockID lock) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        ThreadID thread = threadManager.getThreadID();
        if (lockState.notifyAll(remoteManager, thread)) {
          transactionManager.notify(lock, thread, true);
        }
        return;
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public boolean tryLock(LockID lock, LockLevel level) {
    if (transactionManager.isTransactionLoggingDisabled() || transactionManager.isObjectCreationInProgress()) { return true; }
    
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        if (lockState.tryLock(remoteManager, threadManager.getThreadID(), level)) {
          transactionManager.begin(lock, level);
          return true;
        } else {
          return false;
        }
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public boolean tryLock(LockID lock, LockLevel level, long timeout) throws InterruptedException {
    if (transactionManager.isTransactionLoggingDisabled() || transactionManager.isObjectCreationInProgress()) { return true; }

    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        if (lockState.tryLock(remoteManager, threadManager.getThreadID(), level, timeout)) {
          transactionManager.begin(lock, level);
          return true;
        } else {
          return false;
        }
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void unlock(LockID lock, LockLevel level) {
    if (transactionManager.isTransactionLoggingDisabled() || transactionManager.isObjectCreationInProgress()) { return; }

    waitUntilRunning();
    try {
      transactionManager.commit(lock);
    } finally {
      while (true) {
        ClientLock lockState = getOrCreateState(lock);
        try {
          lockState.unlock(remoteManager, threadManager.getThreadID(), level);
          break;
        } catch (GarbageLockException e) {
          // ignore
        }
      }
    }
  }

  public void wait(LockID lock) throws InterruptedException {
    wait(lock, NULL_LISTENER);
  }

  public void wait(LockID lock, long timeout) throws InterruptedException {
    wait(lock, NULL_LISTENER, timeout);
  }

  public void wait(LockID lock, WaitListener listener) throws InterruptedException {
    waitUntilRunning();

    transactionManager.commit(lock);
    try {
      while (true) {
        ClientLock lockState = getOrCreateState(lock);
        try {
          lockState.wait(remoteManager, listener, threadManager.getThreadID());
          break;
        } catch (GarbageLockException e) {
          // ignore
        }
      }
    } finally {
      // XXX this is questionable
      transactionManager.begin(lock, LockLevel.WRITE);
    }
  }

  public void wait(LockID lock, WaitListener listener, long timeout) throws InterruptedException {
    waitUntilRunning();

    transactionManager.commit(lock);
    try {
      while (true) {
        ClientLock lockState = getOrCreateState(lock);
        try {
          lockState.wait(remoteManager, listener, threadManager.getThreadID(), timeout);
          break;
        } catch (GarbageLockException e) {
          // ignore
        }
      }
    } finally {
      transactionManager.begin(lock, LockLevel.WRITE);
    }
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    State newState;
    stateGuard.writeLock().lock();
    try {
      newState = state.initialize();
      state = newState;
    } finally {
      stateGuard.writeLock().unlock();
    }

    if (newState == State.STARTING) {
      for (ClientLock cls : locks.values()) {
        for (ClientServerExchangeLockContext c: cls.getStateSnapshot()) {
          handshakeMessage.addLockContext(c);
        }
      }
    }
  }

  public void pause(NodeID remoteNode, int disconnected) {
    stateGuard.writeLock().lock();
    try {
      state = state.pause();
    } finally {
      stateGuard.writeLock().unlock();
    }
  }

  public void shutdown() {
    stateGuard.writeLock().lock();
    try {
      state = state.shutdown();
    } finally {
      stateGuard.writeLock().unlock();
    }
  }

  public void unpause(NodeID remoteNode, int disconnected) {
    stateGuard.writeLock().lock();
    try {
      state = state.unpause();
      if (state == State.RUNNING) {
        resubmitInFlightLockQueries();
        runningCondition.signalAll();
      }
    } finally {
      stateGuard.writeLock().unlock();
    }
  }
  
  static enum State {
    RUNNING {
      State unpause() {
        throw new AssertionError();
      }
      
      State pause() {
        return PAUSED;
      }

      State initialize() {
        throw new AssertionError();
      }
      
      State shutdown() {
        return SHUTDOWN;
      }
    },
    
    STARTING {
      State unpause() {
        return RUNNING;
      }
      
      State pause() {
        return PAUSED;
      }
      
      State initialize() {
        throw new AssertionError();
      }
      
      State shutdown() {
        return SHUTDOWN;
      }
    },
    
    PAUSED {
      State unpause() {
        throw new AssertionError();
      }
      
      State pause() {
        throw new AssertionError();
      }
      
      State initialize() {
        return STARTING;
      }
      
      State shutdown() {
        return SHUTDOWN;
      }
    },
    
    SHUTDOWN {
      State pause() {
        return SHUTDOWN;
      }
      
      State unpause() {
        return SHUTDOWN;
      }
      
      State initialize() {
        return SHUTDOWN;
      }
      
      State shutdown() {
        return SHUTDOWN;
      }      
    };

    abstract State pause();
    
    abstract State unpause();
    
    abstract State initialize();
    
    abstract State shutdown();
  }

  public String dump() {
    return "";
  }

  public void dumpToLogger() {
    //
  }
  
  public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
    Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
    for (ClientLock lock : locks.values()) {
      contexts.addAll(lock.getStateSnapshot());
    }
    return contexts;
  }
  
  private Collection<ClientServerExchangeLockContext> queryLock(LockID lock) {
    ThreadID current = threadManager.getThreadID();

    inFlightLockQueries.put(current, lock);
    remoteManager.query(lock, threadManager.getThreadID());
    
    while (true) {
      synchronized (lock) {
        Object data = inFlightLockQueries.get(current);
        if (data instanceof Collection) {
          return (Collection<ClientServerExchangeLockContext>) data;
        } else {
          try {
            lock.wait();
          } catch (InterruptedException e) {
            //
          }
        }
      }
    }
  }
  
  private void resubmitInFlightLockQueries() {
    for (Entry<ThreadID, Object> query : inFlightLockQueries.entrySet()) {
      if (query.getValue() instanceof LockID) {
        remoteManager.query((LockID) query.getValue(), query.getKey());
      }
    }
  }
}
