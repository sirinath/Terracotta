/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.util.Util;
import com.tc.util.runtime.ThreadIDManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientLockManagerImpl implements ClientLockManager {

  private final ConcurrentMap<LockID, ClientLock> locks = new ConcurrentHashMap<LockID, ClientLock>();
  private final ReentrantReadWriteLock            stateGuard = new ReentrantReadWriteLock();
  private final Condition                         runningCondition = stateGuard.writeLock().newCondition();
  private State                                   state = State.RUNNING;
  
  private final RemoteLockManager                 remoteManager;
  private final ThreadIDManager                   threadManager;
  private final SessionManager                    sessionManager;
  private final TCLogger                          logger;


  public ClientLockManagerImpl(TCLogger logger, SessionManager sessionManager, RemoteLockManager remoteManager, ThreadIDManager threadManager) {
    this.logger = logger;
    this.remoteManager = remoteManager;
    this.threadManager = threadManager;
    this.sessionManager = sessionManager;
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
      //lockState = new ClientLockImpl();
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
  
  public void award(GroupID group, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    if (paused() || !sessionManager.isCurrentSession(group, session)) {
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

  public void recall(LockID lock, LockLevel level, int lease) {
    if (paused()) {
      logger.warn("Ignoring recall request from dead server : " + lock + ", interestedLevel : " + level);
      return;
    }
    

    ClientLock lockState = getState(lock);
    if (lockState != null) {
      lockState.recall(level, lease);
    }

  }

  public void refuse(GroupID group, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    if (paused() || !sessionManager.isCurrentSession(group, session)) {
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

  public LockID generateLockIdentifier(String str) {
    throw new AssertionError();
  }

  public LockID generateLockIdentifier(Object obj) {
    throw new AssertionError();
  }

  public LockID generateLockIdentifier(Object obj, String field) {
    throw new AssertionError();
  }

  public LockID generateLockIdentifier(Object obj, long fieldOffset) {
    throw new AssertionError();
  }

  public int globalHoldCount(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        return lockState.globalHoldCount(remoteManager, level);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public int globalPendingCount(LockID lock) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        return lockState.globalPendingCount(remoteManager);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public int globalWaitingCount(LockID lock) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        return lockState.globalWaitingCount(remoteManager);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public boolean isLocked(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        return lockState.isLocked(level);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        return lockState.isLockedBy(threadManager.getThreadID(), level);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public int localHoldCount(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        return lockState.localHoldCount(level);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void lock(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        lockState.lock(remoteManager, threadManager.getThreadID(), level);
        return;
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void lockInterruptibly(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        lockState.lockInterruptibly(remoteManager, threadManager.getThreadID(), level);
        return;
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void notify(LockID lock) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        lockState.notify(remoteManager, threadManager.getThreadID());
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
        lockState.notifyAll(remoteManager, threadManager.getThreadID());
        return;
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public boolean tryLock(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        return lockState.tryLock(remoteManager, threadManager.getThreadID(), level);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public boolean tryLock(LockID lock, LockLevel level, long timeout) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        return lockState.tryLock(remoteManager, threadManager.getThreadID(), level, timeout);
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void unlock(LockID lock, LockLevel level) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        lockState.unlock(remoteManager, threadManager.getThreadID(), level);
        return;
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void wait(LockID lock) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        lockState.wait(remoteManager, threadManager.getThreadID());
        return;
      } catch (GarbageLockException e) {
        // ignore
      }
    }
  }

  public void wait(LockID lock, long timeout) {
    waitUntilRunning();
    while (true) {
      ClientLock lockState = getOrCreateState(lock);
      try {
        lockState.wait(remoteManager, threadManager.getThreadID(), timeout);
        return;
      } catch (GarbageLockException e) {
        // ignore
      }
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
//      for (ClientLock cls : locks.values()) {
//        handshakeMessage.addClientLockState(cls);
//      }
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
}
