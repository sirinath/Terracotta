/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.TCLogger;
import com.tc.net.NodeID;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.util.Util;
import com.tc.util.runtime.ThreadIDManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientLockManagerImpl implements ClientLockManager, ClientLockManagerTestMethods {
  private static final WaitListener NULL_LISTENER = new WaitListener() {
    public void handleWaitEvent() {
      //
    }
  };

  private final Timer                             gcTimer;  
  private final ConcurrentMap<LockID, ClientLock> locks;
  private final ReentrantReadWriteLock            stateGuard = new ReentrantReadWriteLock();
  private final Condition                         runningCondition = stateGuard.writeLock().newCondition();
  private State                                   state = State.RUNNING;
  
  private final RemoteLockManager                 remoteManager;
  private final ThreadIDManager                   threadManager;
  private final SessionManager                    sessionManager;
  private final TCLogger                          logger;

  private final ConcurrentMap<ThreadID, Object>   inFlightLockQueries = new ConcurrentHashMap<ThreadID, Object>();
  
  public ClientLockManagerImpl(TCLogger logger, SessionManager sessionManager, RemoteLockManager remoteManager, ThreadIDManager threadManager, ClientLockManagerConfig config) {
    this.logger = logger;
    this.remoteManager = remoteManager;
    this.threadManager = threadManager;
    this.sessionManager = sessionManager;
    
    this.locks = new ConcurrentHashMap<LockID, ClientLock>(config.getStripedCount());
    this.gcTimer = new Timer("ClientLockManager LockGC", true);
    long halfGCPeriod = Math.max(config.getTimeoutInterval() / 2, 15000);
    gcTimer.schedule(new LockGcTimerTask(), halfGCPeriod, halfGCPeriod);
  }
  
  private ClientLock getOrCreateClientLockState(LockID lock) {
    ClientLock lockState = locks.get(lock);
    if (lockState == null) {
      lockState = new ClientLockImpl(lock);
//      lockState = new WrappedClientLock(lock, remoteManager);
      ClientLock racer = locks.putIfAbsent(lock, lockState);
      if (racer != null) {
        return racer;
      }
    }

    return lockState;
  }
  
  private ClientLock getClientLockState(LockID lock) {
    return locks.get(lock);
  }
  
  /***********************************/
  /* BEGIN TerracottaLocking METHODS */
  /***********************************/
  
  public void lock(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.lock(remoteManager, threadManager.getThreadID(), level);
        break;
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
      }
    }
  }

  public boolean tryLock(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        return lockState.tryLock(remoteManager, threadManager.getThreadID(), level);
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
      }
    }
  }

  public boolean tryLock(LockID lock, LockLevel level, long timeout) throws InterruptedException {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        return lockState.tryLock(remoteManager, threadManager.getThreadID(), level, timeout);
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
      }
    }
  }

  public void lockInterruptibly(LockID lock, LockLevel level) throws InterruptedException {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.lockInterruptibly(remoteManager, threadManager.getThreadID(), level);
        break;
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
      }
    }
  }

  public void unlock(LockID lock, LockLevel level) {
    waitUntilRunning();
    ClientLock lockState = getOrCreateClientLockState(lock);
    lockState.unlock(remoteManager, threadManager.getThreadID(), level);
  }

  public Notify notify(LockID lock) {
    waitUntilRunning();
    ClientLock lockState = getOrCreateClientLockState(lock);
    ThreadID thread = threadManager.getThreadID();
    if (lockState.notify(remoteManager, thread)) {
      return new Notify(lock, thread, false);
    } else {
      return null;
    }
  }

  public Notify notifyAll(LockID lock) {
    waitUntilRunning();
    ClientLock lockState = getOrCreateClientLockState(lock);
    ThreadID thread = threadManager.getThreadID();
    if (lockState.notifyAll(remoteManager, thread)) {
      return new Notify(lock, thread, true);
    } else {
      return null;
    }
  }

  public void wait(LockID lock) throws InterruptedException {
    wait(lock, NULL_LISTENER);
  }

  public void wait(LockID lock, long timeout) throws InterruptedException {
    wait(lock, NULL_LISTENER, timeout);
  }

  public boolean isLocked(LockID lock, LockLevel level) {
    waitUntilRunning();
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      if (lockState.isLocked(level)) {
        return true;
      }
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
    ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      return false;
    } else{
      return lockState.isLockedBy(threadManager.getThreadID(), level);
    }
  }

  public int localHoldCount(LockID lock, LockLevel level) {
    waitUntilRunning();
    ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      return 0;
    } else {
      return lockState.holdCount(level);
    }
  }

  public int globalHoldCount(LockID lock, LockLevel level) {
    waitUntilRunning();
    
    int holdCount = 0;
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      holdCount += lockState.holdCount(level);
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
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      pendingCount += lockState.pendingCount();
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
    
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      return lockState.waitingCount();
    } else {
      return 0;
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

  /***********************************/
  /* END  TerracottaLocking METHODS  */
  /***********************************/

  /***********************************/
  /* BEGIN ClientLockManager METHODS */
  /***********************************/

  public void award(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    if (paused() || !sessionManager.isCurrentSession(node, session)) {
      logger.warn("Ignoring lock award from a dead server :" + session + ", " + sessionManager + " : "
                       + lock + " " + thread + " " + level + " state = " + state);
      return;
    }
    
    if (ThreadID.VM_ID.equals(thread)) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      lockState.award(remoteManager, thread, level);
      return;
    } else {
      while (true) {
        ClientLock lockState = getClientLockState(lock);
        if (lockState == null) {
          remoteManager.unlock(lock, thread, level);
          return;
        } else {
          lockState.award(remoteManager, thread, level);
          return;
        }
      }
    }
  }

  public void notified(LockID lock, ThreadID thread) {
    if (paused()) {
      logger.warn("Ignoring notified call from dead server : " + lock + ", " + thread);
      return;
    }

    final ClientLock lockState = getClientLockState(lock);
    while (true) {
      if (lockState == null) {
        throw new AssertionError(lock);
      } else {
        lockState.notified(thread);
        return;
      }
    }
  }

  public void recall(LockID lock, ServerLockLevel level, int lease) {
    if (paused()) {
      logger.warn("Ignoring recall request from dead server : " + lock + ", interestedLevel : " + level);
      return;
    }
    

    ClientLock lockState = getClientLockState(lock);
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
    
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      lockState.refuse(thread, level);
      return;
    }
  }

  public void info(ThreadID requestor, Collection<ClientServerExchangeLockContext> contexts) {
    Object old = inFlightLockQueries.put(requestor, contexts);
    synchronized (old) {
      old.notifyAll();
    }
  }
  
  /***********************************/
  /* END  ClientLockManager METHODS  */
  /***********************************/
  
  /***********************************/
  /* BEGIN Stupid Wait Test METHODS  */
  /***********************************/

  public void wait(LockID lock, WaitListener listener) throws InterruptedException {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.wait(remoteManager, listener, threadManager.getThreadID());
        break;
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
      }
    }
  }

  public void wait(LockID lock, WaitListener listener, long timeout) throws InterruptedException {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.wait(remoteManager, listener, threadManager.getThreadID(), timeout);
        break;
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
      }
    }
  }

  /***********************************/
  /* END  Stupid Wait Test METHODS   */
  /***********************************/

  /***********************************/
  /*  BEGIN ClientHandshake METHODS  */
  /***********************************/

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    stateGuard.writeLock().lock();
    try {
      state = state.initialize();
      if (state == State.STARTING) {
        for (ClientLock cls : locks.values()) {
          cls.initializeHandshake(handshakeMessage);
        }
      }
    } finally {
      stateGuard.writeLock().unlock();
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
        runningCondition.signalAll();
      }
    } finally {
      stateGuard.writeLock().unlock();
    }
    resubmitInFlightLockQueries();
  }
  
  /***********************************/
  /*   END ClientHandshake METHODS   */
  /***********************************/

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
    StringBuilder sb = new StringBuilder("ClientLockManager [").append(locks.size()).append(" locks]:\n");
    for (Entry<LockID, ClientLock> entry : locks.entrySet()) {
      sb.append("\tLock : ").append(entry.getKey()).append('\n');
      for (ClientServerExchangeLockContext c : entry.getValue().getStateSnapshot()) {
        sb.append("\t\t").append(c).append('\n');
      }
    }
    return sb.toString();
  }

  public void dumpToLogger() {
    logger.info(dump());
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

  class LockGcTimerTask extends TimerTask {
    @Override
    public void run() {
      int gcCount = 0;
      for (Entry<LockID, ClientLock> entry : locks.entrySet()) {
        if (entry.getValue().tryToMarkAsGarbage(remoteManager) && locks.remove(entry.getKey(), entry.getValue())) {
          gcCount++;
        }
      }
      logger.info("Lock GC collected " + gcCount + " garbage locks");
    }
  }

  public int runLockGc() {
    new LockGcTimerTask().run();
    return locks.size();
  }
}
