/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.lockmanager.api.WaitListener;
import com.tc.util.SinglyLinkedList;
import com.tc.util.Util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

public class SynchronizedClientLock extends SinglyLinkedList<State> implements ClientLock {

  
  public void award(ThreadID thread, ServerLockLevel level) {
    throw new AssertionError();
  }

  public Collection<ClientServerExchangeLockContext> getStateSnapshot() {
    throw new AssertionError();
  }

  public int pendingCount() {
    throw new AssertionError();
  }

  public int waitingCount() {
    throw new AssertionError();
  }

  public boolean isLocked(LockLevel level) {
    throw new AssertionError();
  }

  public boolean isLockedBy(ThreadID thread, LockLevel level) {
    throw new AssertionError();
  }

  public int holdCount(LockLevel level) {
    throw new AssertionError();
  }

  public void lock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    if (tryAcquire(remote, thread, level) < 0) {
      Util.selfInterruptIfNeeded(lockQueued(remote, thread, level));
    }
  }

  public void lockInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    if (tryAcquire(remote, thread, level) < 0) {
      lockQueuedInterruptibly(remote, thread, level);
    }
  }

  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    return tryAcquire(remote, thread, level) >= 0;
  }

  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return tryAcquire(remote, thread, level) >= 0 || lockQueuedTimeout(remote, thread, level, timeout);
  }

  private boolean lockQueued(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    QueuedLockAcquire node = new QueuedLockAcquire(thread, level);
    boolean interrupted = false;
    
    addLast(node);
    
    while (true) {
      if (tryLock(remote, thread, level)) {
        remove(node);
        return interrupted;
      }
      LockSupport.park();
      if (Thread.interrupted()) {
        interrupted = true;
      }
    }
  }
  
  private void lockQueuedInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException {
    QueuedLockAcquire node = new QueuedLockAcquire(thread, level);
    addLast(node);
    
    while (true) {
      if (tryLock(remote, thread, level)) {
        remove(node);
        return;
      }
      LockSupport.park();
      if (Thread.interrupted()) {
        break;
      }
    }
    remove(node);
    throw new InterruptedException();
  }
  
  private boolean lockQueuedTimeout(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws InterruptedException {
    long lastTime = System.currentTimeMillis();
    QueuedLockAcquire node = new QueuedLockAcquire(thread, level);
    while (true) {
      if (tryAcquire(remote, thread, level) >= 0) {
        remove(node);
        return true;
      }
      if (timeout <= 0) {
        remove(node);
        return false;
      }
      LockSupport.parkNanos(timeout * 1000);
      if (Thread.interrupted()) {
        break;
      }
      long now = System.currentTimeMillis();
      timeout -= now - lastTime;
      lastTime = now;
    }
    remove(node);
    throw new InterruptedException();
  }
  
  private int tryAcquire(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    return -1;
  }
  
  public void notified(ThreadID thread) {
    throw new AssertionError();
  }

  public boolean notify(RemoteLockManager remote, ThreadID thread) {
    throw new AssertionError();
  }

  public boolean notifyAll(RemoteLockManager remote, ThreadID thread) {
    throw new AssertionError();
  }

  public void recall(ServerLockLevel interest, int lease) {
    throw new AssertionError();
  }

  public void refuse(ThreadID thread, ServerLockLevel level) {
    throw new AssertionError();
  }

  public void unlock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    throw new AssertionError();
  }

  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread) {
    throw new AssertionError();
  }

  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, long timeout) {
    throw new AssertionError();
  }

  static class LockHold extends State {
    final LockLevel level;
    
    LockHold(ThreadID owner, LockLevel level) {
      super(owner);
      this.level = level;
    }
    
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof LockHold) {
        return super.equals(o) && level.equals(((LockHold) o).level);
      } else {
        return false;
      }
    }
  }
  
  static class QueuedLockAcquire extends State {
    final LockLevel level;
    
    QueuedLockAcquire(ThreadID owner, LockLevel level) {
      super(owner);
      this.level = level;
    }
    
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof QueuedLockAcquire) {
        return super.equals(o) && level.equals(((QueuedLockAcquire) o).level);
      } else {
        return false;
      }
    }
  }
  
  static class LockWaiter extends State {
    final List<QueuedLockAcquire> reacquire;
    
    LockWaiter(ThreadID owner, List<QueuedLockAcquire> reacquire) {
      super(owner);
      this.reacquire = reacquire;
    }
    
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof LockWaiter) {
        return super.equals(o) && reacquire.equals(((LockWaiter) o).reacquire);
      } else {
        return false;
      }
    }
  }
  
  static class LockAward extends State {
    final ServerLockLevel level;
    
    LockAward(ThreadID target, ServerLockLevel level) {
      super(target);
      this.level = level;
    }
    
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof LockAward) {
        return super.equals(o) && level.equals(((LockAward) o).level);
      } else {
        return false;
      }
    }
  }

  public boolean garbageCollect() {
    return false;
  }
}

abstract class State implements SinglyLinkedList.LinkedNode<State> {

  private final ThreadID owner;
  
  private State next;

  State(ThreadID owner) {
    this.owner = owner;
    this.next = null;
  }
  
  public State getNext() {
    return next;
  }

  public State setNext(State newNext) {
    State old = next;
    next = newNext;
    return old;
  }
  
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof State) {
      return (owner.equals(((State) o).owner));
    } else {
      return false;
    }
  }
}
