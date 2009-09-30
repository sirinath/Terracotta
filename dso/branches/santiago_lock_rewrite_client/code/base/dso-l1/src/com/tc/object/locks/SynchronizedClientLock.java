/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.net.ClientID;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.util.SinglyLinkedList;
import com.tc.util.UnsafeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.LockSupport;

public class SynchronizedClientLock extends SinglyLinkedList<State> implements ClientLock {
  private static final boolean DEBUG    = false;
  
  private static final Timer LOCK_TIMER = new Timer();
  
  private final LockID       lock;
  
  private ClientGreediness   greediness = ClientGreediness.FREE;
  private ServerLockLevel    recalledLevel;
  private RemoteLockManager  remoteManager;
  
  private volatile int       gcCycleCount = 0;

  public SynchronizedClientLock(LockID lock, RemoteLockManager remote) {
    this.lock = lock;
    this.remoteManager = remote;
  }
  
  public void lock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " lock");
//    if (!tryAcquire(remote, thread, level, -1).succeeded()) {
      if (acquireQueued(remote, thread, level))
        Thread.currentThread().interrupt();
//    }
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " locked " + level);
  }

  public void lockInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " lock interruptibly");
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
//    if (!tryAcquire(remote, thread, level, -1).succeeded()) {
      acquireQueuedInterruptibly(remote, thread, level);
//    }
  }

  /**
   * Try lock would normally just be:
   *   <code>return tryAcquire(remote, thread, level, 0).succeeeded();</code>
   * <p>
   * However because the existing contract on tryLock requires us to wait for the server
   * if the lock attempt is delegated things get significantly more complicated.
   */
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " try lock");

//    switch (tryAcquire(remote, thread, level, 0)) {
//      case SUCCEEDED_SHARED:
//        return true;
//      case SUCCEEDED:
//        return true;
//      case FAILED:
//        return false;
//      case DELEGATED:
//        break;
//      default:
//        throw new AssertionError();
//    }

    QueuedLockAcquire node = new QueuedLockAcquire(thread, level);
    try {
      synchronized (this) {
        addLast(node);
      }

      while (!node.serverResponded()) {
        synchronized (this) {
          switch (tryAcquire(remote, thread, level, 0)) {
            case SUCCEEDED_SHARED:
              unparkNextQueuedAcquire(node.getNext());
              return true;
            case SUCCEEDED:
              return true;
            case FAILED:
              unparkNextQueuedAcquire(node.getNext());
              return false;
            case DELEGATED:
              unparkNextQueuedAcquire(node.getNext());
              break;
            default:
              throw new AssertionError();
          }
        }
        node.park();
        if (Thread.interrupted()) {
          Thread.currentThread().interrupt();
        }
      }

      synchronized (this) {
        switch (tryAcquire(remote, thread, level, 0)) {
          case SUCCEEDED_SHARED:
            unparkNextQueuedAcquire(node.getNext());
            return true;
          case SUCCEEDED:
            return true;
          case FAILED:
            unparkNextQueuedAcquire(node.getNext());
            return false;
          case DELEGATED:
            unparkNextQueuedAcquire(node.getNext());
            return false;
          default:
            throw new AssertionError();
        }
      }
    } finally {
      synchronized (this) {
        remove(node);
      }
    }
  }

  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws InterruptedException {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " try lock w/ timeout");
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return /*tryAcquire(remote, thread, level, timeout).succeeded() || */acquireQueuedTimeout(remote, thread, level, timeout);
  }

  public void unlock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " unlock");
    if (tryRelease(remote, thread, level)) {
      unparkNextQueuedAcquire();
    }
  }

  /**
   * Find a lock waiter in the state and unpark it.
   */
  public synchronized boolean notify(RemoteLockManager remote, ThreadID thread) {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " notifying a single lock waiter");
    if (greediness.equals(ClientGreediness.FREE)) {
      //other L1s may be waiting (let server decide who to notify)
      return true;
    } else {
      boolean lockHeld = false;
      for (State s : this) {
        if ((s instanceof LockHold) && s.getOwner().equals(thread) && ((LockHold) s).getLockLevel().isWrite()) {
          lockHeld = true;
        }
        if (s instanceof LockWaiter) {
          if (!lockHeld) {
            throw new IllegalMonitorStateException();
          }
          ((LockWaiter) s).unpark();
          return false;
        }
      }
      //no local waiters - defer to server
      return true;
    }
  }

  /**
   * Find all the lock waiters in the state and unpark them.
   */
  public synchronized boolean notifyAll(RemoteLockManager remote, ThreadID thread) {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " notifying all lock waiters");
    if (greediness.equals(ClientGreediness.FREE)) {
      //other L1s may be waiting (let server decide who to notify)
      return true;
    } else {
      boolean lockHeld = false;
      for (State s : this) {
        if ((s instanceof LockHold) && s.getOwner().equals(thread) && ((LockHold) s).getLockLevel().isWrite()) {
          lockHeld = true;
        }
        if (s instanceof LockWaiter) {
          if (!lockHeld) {
            throw new IllegalMonitorStateException();
          }
          ((LockWaiter) s).unpark();
        }
      }
      return true;
    }    
  }

  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread) throws InterruptedException {
    wait(remote, listener, thread, -1);
  }

  /**
   * Waiting involves unlocking all the write lock holds, sleeping on the original condition, until wake up, and
   * then re-acquiring the original locks in their original order. 
   */
  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, long timeout) throws InterruptedException {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " moving to wait with " + ((timeout < 0) ? "no timeout" : (timeout + " ms")));
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }

    LockWaiter node = new LockWaiter(thread, lock.javaObject());
    Stack<LockHold> holds = new Stack<LockHold>();
    try {
      synchronized (this) {
        addLast(node);
        for (State s : this) {
          if ((s instanceof LockHold) && s.getOwner().equals(thread) && ((LockHold) s).getLockLevel().isWrite()) {
            LockHold hold = (LockHold) s;
            holds.push(hold);            
          }
        }

        if (holds.isEmpty()) { throw new IllegalMonitorStateException(); }
      }
      
      for (LockHold hold : holds) {
        if (hold.getLockLevel().flushOnUnlock() || greediness.flushOnUnlock(this, hold)) {
          remote.flush(lock);
        }
        
        synchronized (this) {
          remove(hold);
          greediness = greediness.waiting(remote, lock, this, hold);
          unparkNextQueuedAcquire();
        }
      }

      listener.handleWaitEvent();
      if (timeout < 0) {
        node.park();
      } else {
        node.park(timeout);
      }
    } finally {
      synchronized (this) {
        greediness = greediness.interrupt(remote, lock, thread);
        remove(node);
      }
      while (!holds.isEmpty()) {
        LockHold lh = holds.pop();
        acquireQueued(remote, thread, lh.getLockLevel(), new MonitorBasedQueuedLockAcquire(thread, lh.getLockLevel(), lock.javaObject()));
      }
    }
  }

  public synchronized Collection<ClientServerExchangeLockContext> getStateSnapshot() {
    ClientID client = ManagerUtil.getClientID();
    Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();

    switch (greediness) {
      case GREEDY_READ:
        contexts.add(new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID, ServerLockContext.State.GREEDY_HOLDER_READ));
        break;
      case LEASED_GREEDY_WRITE:
      case GREEDY_WRITE:
        contexts.add(new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID, ServerLockContext.State.GREEDY_HOLDER_WRITE));
        break;
      case GARBAGE:
        return Collections.emptyList();
      case FREE:
      case RECALLED:
      case RECALL_IN_PROGRESS:
        break;
    }

    for (State s : this) {
      if (s instanceof LockHold) {
        switch (((LockHold) s).getLockLevel()) {
          case READ:
            contexts.add(new ClientServerExchangeLockContext(lock, client, s.getOwner(), ServerLockContext.State.HOLDER_READ));
            break;
          case WRITE:
          case SYNCHRONOUS_WRITE:
            contexts.add(new ClientServerExchangeLockContext(lock, client, s.getOwner(), ServerLockContext.State.HOLDER_WRITE));
            break;
          default:
            throw new AssertionError();
        }
      } else if (s instanceof LockWaiter) {
        contexts.add(new ClientServerExchangeLockContext(lock, client, s.getOwner(), ServerLockContext.State.WAITER));
      } else if (s instanceof QueuedLockAcquire) {
        switch (((QueuedLockAcquire) s).getLockLevel()) {
          case READ:
            contexts.add(new ClientServerExchangeLockContext(lock, client, s.getOwner(), ServerLockContext.State.PENDING_READ, -1));
            break;
          case WRITE:
          case SYNCHRONOUS_WRITE:
            contexts.add(new ClientServerExchangeLockContext(lock, client, s.getOwner(), ServerLockContext.State.PENDING_WRITE, -1));
            break;
          default:
            throw new AssertionError();
        }
      }
    }
    
    return contexts;
  }

  public synchronized int pendingCount() {
    int penders = 0;
    for (State s : this) {
      if (s instanceof QueuedLockAcquire) {
        penders++;
      }
    }
    return penders;
  }

  public synchronized int waitingCount() {
    int waiters = 0;
    for (State s : this) {
      if (s instanceof LockWaiter) {
        waiters++;
      }
    }
    return waiters;
  }

  public synchronized boolean isLocked(LockLevel level) {
    for (State s : this) {
      if ((s instanceof LockHold) && (((LockHold) s).level.equals(level))) {
        return true;
      } else if (s instanceof LockWaiter) {
        break;
      } else if (s instanceof QueuedLockAcquire) {
        break;
      }
    }
    return false;
  }

  public synchronized boolean isLockedBy(ThreadID thread, LockLevel level) {
    for (State s : this) {
      if ((s instanceof LockHold) && ((LockHold) s).getLockLevel().equals(level) && s.getOwner().equals(thread)) {
        return true;
      } else if (s instanceof LockWaiter) {
        break;
      } else if (s instanceof QueuedLockAcquire) {
        break;
      }
    }
    return false;
  }

  public synchronized int holdCount(LockLevel level) {
    int holders = 0;
    for (State s : this) {
      if ((s instanceof LockHold) && ((LockHold) s).level.equals(level)) {
        holders++;
      } else if (s instanceof LockWaiter) {
        break;
      } else if (s instanceof QueuedLockAcquire) {
        break;
      }
    }
    return holders;
  }

  public void notified(ThreadID thread) {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server notifying " + thread);
    LockWaiter waiter = null;
    synchronized (this) {
      for (State s : this) {
        if ((s instanceof LockWaiter) && s.getOwner().equals(thread)) {
          waiter = (LockWaiter) s;
          break;
        }
      }
    }

    if (waiter != null) {
      waiter.unpark();
    }
  }

  public synchronized void recall(final RemoteLockManager remote, final ServerLockLevel interest, int lease) {
    greediness = greediness.recall(null, interest, lease);
    recalledLevel = interest;
    
    if (greediness.equals(ClientGreediness.RECALLED)) {
      if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server requested recall " + interest);
      greediness = doRecall(remote);
    } else if (greediness.equals(ClientGreediness.LEASED_GREEDY_WRITE)) {
      if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server granted leased " + interest);
      LOCK_TIMER.schedule(new TimerTask() {
        @Override
        public void run() {
          doRecall(remote);
        }
      }, lease);
    }
  }

  public void refuse(ThreadID thread, ServerLockLevel level) {
    // if this is a try lock w/out timeout then we need to kick the locking thread
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server refusing lock request " + level);
    unparkQueuedAcquire(thread);
  }

  public synchronized void award(ThreadID thread, ServerLockLevel level) {
    if (ThreadID.VM_ID.equals(thread)) {
      if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server awarded greedy " + level);
      greediness = greediness.award(level);
      unparkNextQueuedAcquire();
    } else {
      if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server awarded per-thread " + level + " to " + thread);
      LockAward award = new LockAward(thread, level);
      addFirst(award);
      if (!unparkQueuedAcquire(thread)) {
        remove(award);
        remoteManager.unlock(lock, thread, level);
      }
    }
  }

  /**
   * Our locks behave in a slightly bizarre way - we don't queue very strictly, if the head of the
   * acquire queue fails, we allow acquires further down to suceed.  This is different to the JDK
   * RRWL - suspect this is a historical accident.
   */
  static enum AcquireResult {
    /**
     * Acquire succeeded - other threads may succeed now too.
     */
    SUCCEEDED_SHARED,
    /**
     * Acquire succeeded - other threads will fail in acquire
     */
    SUCCEEDED,
    /**
     * Acquire was delegated to the server - used by tryLock.
     */
    DELEGATED,
    /**
     * Acquire was refused - other threads might succeed though.
     */
    FAILED;

    public boolean shared() {
      return this != SUCCEEDED;
    }

    public boolean succeeded() {
      return (this == SUCCEEDED) | (this == SUCCEEDED_SHARED);
    }
  }

  /**
   * Attempt to acquire the lock at the given level:
   * <p>
   * This is a two stage process:
   * <ol>
   * <li>Attempt a decision based on thread level state.</li>
   * <li>Ask the client greediness if it can award - if not request the lock from the server.</li>
   * </ol>
   */
  private synchronized AcquireResult tryAcquire(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to acquire " + level);
    if (level == LockLevel.CONCURRENT) {
      return AcquireResult.SUCCEEDED_SHARED;
    }
    
    //What can we glean from local lock state
    LockHold newHold = new LockHold(thread, level);
    LockLevel holdLevel = null;
    for (Iterator<State> it = iterator(); it.hasNext();) {
      State s = it.next();
      if (s instanceof LockHold) {
        LockHold hold = (LockHold) s;
        if (hold.getOwner().equals(thread)) {
          if (level.isRead()) {
            if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " awarded " + level + " due to existing thread hold");
            addFirst(newHold);
            return hold.getLockLevel().isWrite() ? AcquireResult.SUCCEEDED : AcquireResult.SUCCEEDED_SHARED;
          }
          if (hold.getLockLevel().isWrite()) {
            if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " awarded " + level + " due to existing WRITE hold");
            addFirst(newHold);
            return AcquireResult.SUCCEEDED;
          } else {
            holdLevel = hold.getLockLevel();
          }
        } else {
          if (hold.getLockLevel().isWrite()) {
            if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " denied " + level + " due to other thread holding WRITE");
            return AcquireResult.FAILED;
          }
        }
      } else if (s instanceof LockAward) {
        LockAward award = (LockAward) s;
        if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " found per thread award for " + award.getOwner() + " @ " + award.getLockLevel());
        if (award.getOwner().equals(thread)) {
          switch (level) {
            case READ:
              if (award.getLockLevel().equals(ServerLockLevel.READ)) {
                it.remove();
                if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " awarded " + level + " due to per thread award");
                addFirst(newHold);
                return AcquireResult.SUCCEEDED_SHARED;
              }
              break;
            case SYNCHRONOUS_WRITE:
            case WRITE:
              if (award.getLockLevel().equals(ServerLockLevel.WRITE)) {
                it.remove();
                if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " awarded " + level + " due to per thread award");
                addFirst(newHold);
                return AcquireResult.SUCCEEDED;
              }
              break;
            default:
              throw new AssertionError();
          }
        }
      }
    }

    //Lock upgrade not supported check
    if (level.isWrite() && (holdLevel != null) && holdLevel.isRead()) {
      throw new TCLockUpgradeNotSupportedError();
    }
    
    //Local lock state did not give us a definitive answer
    if (greediness.canAward(level)) {
      if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " awarded " + level + " due to client greedy hold");
      addFirst(newHold);
      return level.isWrite() ? AcquireResult.SUCCEEDED : AcquireResult.SUCCEEDED_SHARED;
    } else {
      if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " denied " + level + " - contacting server...");
      greediness = greediness.requestLevel(remote, lock, thread, level, timeout);
      return AcquireResult.DELEGATED;
    }
  }

  /**
   * Unlock and return <code>true</code> if acquires might now succeed.
   */
  private boolean tryRelease(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    if (level == LockLevel.CONCURRENT) {
      return false;
    }
    
    LockHold unlocked = null;
    synchronized (this) {
      for (Iterator<State> it = iterator(); it.hasNext();) {
        State s = it.next();
        if (s instanceof LockHold) {
          LockHold hold = (LockHold) s;
          if (hold.getOwner().equals(thread) && (hold.getLockLevel().equals(level) || (level == null))) {
            unlocked = hold;
            break;
          }
        }
      }
      
      if (unlocked == null) {
        if (level == null) {
          return false; //tolerate this for the moment... (this covers concurrent locks - when we aren't told what we are unlocking)
        } else {
          throw new IllegalMonitorStateException();
        }
      }
    }

    if (unlocked.getLockLevel().flushOnUnlock() || greediness.flushOnUnlock(this, unlocked)) {
      remote.flush(lock);
    }
    
    synchronized (this) {
      if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " unlocking " + level);
      remove(unlocked);
      greediness = greediness.unlocked(remote, lock, this, unlocked);
      // this is wrong - but shouldn't break anything
      return true;
    }
  }

  /**
   * Conventional acquire queued - uses a LockSupport based queue object.
   */
  private boolean acquireQueued(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    final QueuedLockAcquire node = new QueuedLockAcquire(thread, level);
    return acquireQueued(remote, thread, level, node);
  }

  /**
   * Generic acquire - allows wait to use a wait/notify based queue object to prevent deadlocks due to lock
   * inversion.
   */
  private boolean acquireQueued(RemoteLockManager remote, ThreadID thread, LockLevel level, State node) {
    synchronized (this) {
      addLast(node);
    }
    try {
      boolean interrupted = false;
      for (;;) {
        synchronized (this) {
          AcquireResult result = tryAcquire(remote, thread, level, -1);
          if (result.shared()) {
            unparkNextQueuedAcquire(node.getNext());
          }
          if (result.succeeded()) {
            remove(node);
            return interrupted;
          }
        }
        try {
          node.park();
          if (Thread.interrupted()) {
            interrupted = true;
          }
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } catch (RuntimeException ex) {
      synchronized (this) {
        remove(node);
        unparkNextQueuedAcquire();
      }
      throw ex;
    } catch (TCLockUpgradeNotSupportedError e) {
      synchronized (this) {
        remove(node);
        unparkNextQueuedAcquire();
      }
      throw e;
    }
  }

  /**
   * Just like acquireQueued but throws InterruptedException if unparked by interrupt rather then saving the interrupt state
   */
  private void acquireQueuedInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException {
    final QueuedLockAcquire node = new QueuedLockAcquire(thread, level);
    synchronized (this) {
      addLast(node);
    }
    try {
      for (;;) {
        synchronized (this) {
          AcquireResult result = tryAcquire(remote, thread, level, -1);
          if (result.shared()) {
            unparkNextQueuedAcquire(node.getNext());
          }
          if (result.succeeded()) {
            remove(node);
            return;
          }
        }
        node.park();
        if (Thread.interrupted()) {
          break;
        }
      }
    } catch (RuntimeException ex) {
      synchronized (this) {
        remove(node);
        unparkNextQueuedAcquire();
      }
      throw ex;
    } catch (TCLockUpgradeNotSupportedError e) {
      synchronized (this) {
        remove(node);
        unparkNextQueuedAcquire();
      }
      throw e;
    }
    // Arrive here only if interrupted
    synchronized (this) {
      remove(node);
    }
    throw new InterruptedException();
  }

  /**
   * Acquire queued - waiting for at most timeout milliseconds.
   */
  private boolean acquireQueuedTimeout(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws InterruptedException {
    long lastTime = System.currentTimeMillis();
    final QueuedLockAcquire node = new QueuedLockAcquire(thread, level);
    synchronized (this) {
      addLast(node);
    }
    try {
      for (;;) {
        synchronized (this) {
          AcquireResult result = tryAcquire(remote, thread, level, timeout);
          if (result.shared()) {
            unparkNextQueuedAcquire(node.getNext());
          }
          if (result.succeeded()) {
            remove(node);
            return true;
          } else {
            if (timeout <= 0) {
              remove(node);
              return false;
            }
          }
        }
        node.park(timeout);
        if (Thread.interrupted()) break;
        long now = System.currentTimeMillis();
        timeout -= now - lastTime;
        lastTime = now;
      }
    } catch (RuntimeException ex) {
      synchronized (this) {
        remove(node);
        unparkNextQueuedAcquire();
      }
      throw ex;
    } catch (TCLockUpgradeNotSupportedError e) {
      synchronized (this) {
        remove(node);
        unparkNextQueuedAcquire();
      }
      throw e;
    }
    // Arrive here only if interrupted
    synchronized (this) {
      remove(node);
    }
    throw new InterruptedException();
  }

  /**
   * Unpark the first queued acquire
   */
  private synchronized void unparkNextQueuedAcquire() {
    if (!isEmpty()) {
      unparkNextQueuedAcquire(getFirst());
    }
  }

  /**
   * Unpark the next queued acquire (after supplied node)
   */
  private synchronized void unparkNextQueuedAcquire(State node) {
    while (node != null) {
      if (node instanceof QueuedLockAcquire) {
        ((QueuedLockAcquire) node).acked();
        ((QueuedLockAcquire) node).unpark();
        if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : unparked " + node.getOwner() + " wanting " + ((QueuedLockAcquire) node).getLockLevel());
        return;
      }
      node = node.getNext();
    }
  }

  /**
   * Unpark the queued acquire associated with the given thread - used by per-thread awards
   */
  private synchronized boolean unparkQueuedAcquire(ThreadID thread) {
    for (State s : this) {
      if ((s instanceof QueuedLockAcquire) && s.getOwner().equals(thread)) {
        QueuedLockAcquire acquire = (QueuedLockAcquire) s;
        acquire.acked();
        acquire.unpark();
        if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : unparked " + thread + " wanting " + ((QueuedLockAcquire) s).getLockLevel());
        return true;
      }
    }
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : failed to unpark " + thread);
    return false;
  }

  protected synchronized ClientGreediness doRecall(final RemoteLockManager remote) {
    if (canRecallNow(recalledLevel)) {
      doFlush(remote);
      if (remote.isTransactionsForLockFlushed(lock, new LockFlushCallback() {
        public void transactionsForLockFlushed(LockID id) {
          synchronized (SynchronizedClientLock.this) {
            if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : doing recall commit (having flushed transactions)");
            greediness = recallCommit(remote);
          }
        }
      })) {
        if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : doing recall commit " + greediness);
        return recallCommit(remote);
      }
      return ClientGreediness.RECALL_IN_PROGRESS;
    } else {
      if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : cannot recall right now");
      return this.greediness;
    }
  }

  private synchronized ClientGreediness recallCommit(RemoteLockManager remote) {
    remote.recallCommit(lock, getLegacyStateSnapshot());
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : free'd greedy lock");
    return ClientGreediness.FREE;
  }
  
  private synchronized boolean canRecallNow(ServerLockLevel level) {
    for (State s : this) {
      if (s instanceof LockHold) {
        switch (level) {
          case WRITE: //any hold will block a recall for write
            return false;
          case READ: //a write hold will block a read recall
            if (((LockHold) s).getLockLevel().isWrite()) return false;
            break;
          case NONE:
            throw new AssertionError();
        }
      }
    }
    return true;
  }

  //bleagh this is horrible - actively looking at this
  protected void doFlush(RemoteLockManager remote) {
    UnsafeUtil.monitorExit(this);
    try {
      remote.flush(lock);
    } finally {
      UnsafeUtil.monitorEnter(this);
    }
  }
  
  static class LockHold extends State {
    private final LockLevel level;
    
    LockHold(ThreadID owner, LockLevel level) {
      super(owner);
      this.level = level;
    }
    
    LockLevel getLockLevel() {
      return level;
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
    
    public String toString() {
      return super.toString() + " : " + level;
    }
  }
  
  static class QueuedLockAcquire extends State {
    private final LockLevel level;
    private final Thread javaThread;
    private volatile boolean serverResponded = false;
    
    QueuedLockAcquire(ThreadID owner, LockLevel level) {
      super(owner);
      this.javaThread = Thread.currentThread();
      this.level = level;
    }
    
    LockLevel getLockLevel() {
      return level;
    }
    
    Thread getJavaThread() {
      return javaThread;
    }
    
    void park() {
      LockSupport.park();
    }
    
    void park(long timeout) {
      LockSupport.parkNanos(timeout * 1000000L);
    }
    
    void unpark() {
      LockSupport.unpark(javaThread);
    }

    boolean serverResponded() {
      return serverResponded;
    }
    
    void acked() {
      serverResponded = true;
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
    
    
    public String toString() {
      return super.toString() + " : " + level;
    }
  }
  
  static class MonitorBasedQueuedLockAcquire extends QueuedLockAcquire {

    private final Object javaObject;
    private boolean      parked = false;
    
    MonitorBasedQueuedLockAcquire(ThreadID owner, LockLevel level, Object javaObject) {
      super(owner, level);
      this.javaObject = javaObject;
    }
    
    void park() {
      synchronized (javaObject) {
        parked = true;
        while (parked) {
          try {
            javaObject.wait();
          } catch (InterruptedException e) {
            //
          }
        }
        Thread.interrupted();
        parked = false;
      }
    }

    /**
     * MonitorBasedQueuedLockAcquires are only used to reacquire locks after waiting
     *  - they should always park indefinitely
     */
    void park(long timeout) {
      throw new AssertionError();
    }
    
    void unpark() {
      if (parked) {
        parked = false;
        getJavaThread().interrupt();
      }
    }
  }
  
  static class LockWaiter extends State {
    
    private final Object waitObject;
    
    LockWaiter(ThreadID owner, Object waitObject) {
      super(owner);
      this.waitObject = waitObject;
    }
    
    void park() throws InterruptedException {
      synchronized (waitObject) {
        waitObject.wait();
      }
    }

    void park(long timeout) throws InterruptedException {
      synchronized (waitObject) {
        waitObject.wait(timeout);
      }
    }
    
    void unpark() {
      synchronized (waitObject) {
        waitObject.notify();
      }
    }
    
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof LockWaiter) {
        return super.equals(o);
      } else {
        return false;
      }
    }
  }
  
  static class LockAward extends State {
    private final ServerLockLevel level;
    
    LockAward(ThreadID target, ServerLockLevel level) {
      super(target);
      this.level = level;
    }
    
    ServerLockLevel getLockLevel() {
      return level;
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
    
    public String toString() {
      return super.toString() + " : " + level;
    }
  }

  public synchronized boolean garbageCollect() {
    if (isEmpty() && gcCycleCount > 0) {
      greediness = ClientGreediness.GARBAGE;
      return true;
    } else {
      gcCycleCount++;
      return false;
    }
  }  
  
  private void markUsed() {
    gcCycleCount = 0;
  }

  
  @Deprecated
  public Collection<ClientServerExchangeLockContext> getLegacyStateSnapshot() {
    Collection<ClientServerExchangeLockContext> fullState = getStateSnapshot();
    Collection<ClientServerExchangeLockContext> legacyState = new ArrayList();
    
    legacyState.addAll(getLockHolds(fullState));
    
    for (ClientServerExchangeLockContext context : fullState) {
      switch (context.getState()) {
        case PENDING_READ:
        case PENDING_WRITE:
        case TRY_PENDING_READ:
        case TRY_PENDING_WRITE:
        case WAITER:
          legacyState.add(context);
          break;
        default:
          break;
      }
    }
    
    return legacyState;
  }
  
  private Collection<ClientServerExchangeLockContext> getLockHolds(Collection<ClientServerExchangeLockContext> fullState) {
    //Lock Holds First
    for (ClientServerExchangeLockContext context : fullState) {
      if (context.getState().getType().equals(Type.GREEDY_HOLDER)) {
        return Collections.singletonList(context);
      }
    }
    
    Map<ThreadID, ClientServerExchangeLockContext> holds = new HashMap<ThreadID, ClientServerExchangeLockContext>();
    for (ClientServerExchangeLockContext context : fullState) {
      switch (context.getState()) {
        case HOLDER_READ:
        case HOLDER_WRITE:
          ClientServerExchangeLockContext current = holds.get(context.getThreadID());
          if (current == null) {
            holds.put(context.getThreadID(), context);
          } else {
            if (context.getState().getLockLevel().equals(ServerLockLevel.WRITE)) {
              holds.put(context.getThreadID(), context);
            }
          }
          break;
        default:
          break;
      }
    }
    
    return holds.values();
  }
  
  public synchronized String toString() {
    StringBuilder sb = new StringBuilder();
    
    sb.append("SynchronizedClientLock : ").append(lock).append('\n');
    sb.append("Greediness : ").append(greediness).append('\n');
    sb.append("State:").append('\n');
    for (State s : this) {
      sb.append('\t').append(s).append('\n');
    }
    
    return sb.toString();
  }
}

abstract class State implements SinglyLinkedList.LinkedNode<State> {

  private final ThreadID owner;
  
  private State next;

  State(ThreadID owner) {
    this.owner = owner;
    this.next = null;
  }
  
  void park() throws InterruptedException {
    throw new AssertionError();
  }

  void park(long timeout) throws InterruptedException {
    throw new AssertionError();
  }

  void unpark() {
    throw new AssertionError();
  }
  
  ThreadID getOwner() {
    return owner;
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
  
  public String toString() {
    return getClass().getSimpleName() + " : " + owner;
  }
}
