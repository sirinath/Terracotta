/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.net.ClientID;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.locks.LockStateNode.LockAward;
import com.tc.object.locks.LockStateNode.LockHold;
import com.tc.object.locks.LockStateNode.LockWaiter;
import com.tc.object.locks.LockStateNode.PendingLockHold;
import com.tc.object.locks.LockStateNode.MonitorBasedPendingLockHold;
import com.tc.object.msg.ClientHandshakeMessage;
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

class ClientLockImpl extends SinglyLinkedList<LockStateNode> implements ClientLock {
  private static final boolean DEBUG         = false;
  private static final Timer   LOCK_TIMER    = new Timer("ClientLockImpl Timer", true);
  protected static final int   BLOCKING_LOCK = Integer.MIN_VALUE;
  
  private final LockID         lock;
  
  private ClientGreediness     greediness    = ClientGreediness.FREE;
  private ServerLockLevel      recalledLevel;
  
  private volatile byte         gcCycleCount  = 0;
  private volatile boolean      pinned;

  public ClientLockImpl(LockID lock) {
    this.lock = lock;
  }
  
  /*
   * Try to acquire this lock locally - if successful then return, otherwise queue the request
   * and potentially call out to the server.
   */    
  public void lock(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " lock");
    if (!tryAcquireLocally(thread, level).succeeded()) {
      acquireQueued(remote, thread, level);
    }
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " locked " + level);
  }

  /*
   * Try to acquire this lock locally - if successful then return, otherwise queue the request
   * and potentially call out to the server
   */
  public void lockInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException, GarbageLockException {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " lock interruptibly");
    if (!tryAcquireLocally(thread, level).succeeded()) {
      acquireQueuedInterruptibly(remote, thread, level);
    }
  }

  /*
   * Try lock would normally just be:
   *   <code>return tryAcquire(remote, thread, level, 0).succeeeded();</code>
   * <p>
   * However because the existing contract on tryLock requires us to wait for the server
   * if the lock attempt is delegated things get significantly more complicated.
   */
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " try lock");

    // try to acquire locally
    AcquireResult local = tryAcquireLocally(thread, level);
    if (!local.delegated()) {
      // if local acquire did not require delegation to the server then return the result
      return local.succeeded();
    } else {
      //we need to talk to the server queue the request...
      PendingLockHold node = new PendingLockHold(thread, level, 0);
      try {
        synchronized (this) {
          addLast(node);
        }

        boolean delegate = true;
        // keep trying until we get a response from the server
        while (!node.serverResponded()) {
          synchronized (this) {
            // try acquire - with possible server communications if delegate is true 
            AcquireResult result = tryAcquire(delegate, remote, thread, level, -1);
            if (result.shared()) {
              unparkNextQueuedAcquire(node.getNext());
            }
            if (result.succeeded()) {
              // if acquire suceeded then return true
              return true;
            }
            if (result.delegated()) {
              // if acquire talked to the server - then set delegate to false to prevent duplicate server getting upset at duplicate messages
              delegate = false;
            }
          }
          // park this queued node and wait for any state changes that might allow us to make progress
          node.park();
          if (Thread.interrupted()) {
            Thread.currentThread().interrupt();
          }
        }

        //finally heard from the server - this our last chance to succeed
        AcquireResult localResult = tryAcquireLocally(thread, level);
        if (localResult.shared()) {
          unparkNextQueuedAcquire(node.getNext());
        }
        return localResult.succeeded();
      } finally {
        synchronized (this) {
          remove(node);
        }
      }
    }
  }

  /*
   * Try to acquire locally - if we fail then queue the request and defer to the server.
   */
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws InterruptedException, GarbageLockException {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " try lock w/ timeout");
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return tryAcquireLocally(thread, level).succeeded() || acquireQueuedTimeout(remote, thread, level, timeout);
  }

  /*
   * Release the lock and unpark an acquire if release tells us that queued acquires may now succeed.
   */
  public void unlock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to " + level + " unlock");
    if (release(remote, thread, level)) {
      unparkNextQueuedAcquire();
    }
  }

  /*
   * Find a lock waiter in the state and unpark it - while concurrently checking for a write hold by the notifying thread
   */
  public synchronized boolean notify(RemoteLockManager remote, ThreadID thread, Object waitObject) {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " notifying a single lock waiter");
    if (greediness.equals(ClientGreediness.FREE)) {
      //other L1s may be waiting (let server decide who to notify)
      return true;
    } else {
      boolean lockHeld = false;
      for (LockStateNode s : this) {
        if ((s instanceof LockHold) && s.getOwner().equals(thread) && ((LockHold) s).getLockLevel().isWrite()) {
          lockHeld = true;
        }
        if (s instanceof LockWaiter) {
          if (!lockHeld) {
            throw new IllegalMonitorStateException();
          }
          // move this waiters reacquire nodes into the queue - we must do this before returning to ensure transactional correctness on notifies.
          notify((LockWaiter) s);
          ((LockWaiter) s).unpark();
          return false;
        }
      }
      // no local waiters - defer to server
      // this seems wrong - we hold the greedy lock so no other nodes should have waiting threads - but i'm not going to question it for the moment...
      return true;
    }
  }

  /*
   * Find all the lock waiters in the state and unpark them.
   */
  public synchronized boolean notifyAll(RemoteLockManager remote, ThreadID thread, Object waitObject) {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " notifying all lock waiters");
    if (greediness.equals(ClientGreediness.FREE)) {
      //other L1s may be waiting (let server decide who to notify)
      return true;
    } else {
      boolean lockHeld = false;
      for (LockStateNode s : this) {
        if ((s instanceof LockHold) && s.getOwner().equals(thread) && ((LockHold) s).getLockLevel().isWrite()) {
          lockHeld = true;
        }
        if (s instanceof LockWaiter) {
          if (!lockHeld) {
            throw new IllegalMonitorStateException();
          }
          // move this waiters reacquire nodes into the queue - we must do this before returning to ensure transactional correctness on notifies.
          notify((LockWaiter) s);
          ((LockWaiter) s).unpark();
        }
      }
      return true;
    }    
  }

  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, Object waitObject) throws InterruptedException {
    wait(remote, listener, thread, waitObject, -1);
  }

  /*
   * Waiting involves unlocking all the write lock holds, sleeping on the original condition, until wake up, and
   * then re-acquiring the original locks in their original order.
   * 
   * This code is extraordinarily sensitive to the order of operations...
   */
  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, Object waitObject, long timeout) throws InterruptedException {
    markUsed();
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " moving to wait with " + ((timeout < 0) ? "no timeout" : (timeout + " ms")));
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }

    LockWaiter node = null;
    // stack of reacquires that should locked when leaving the wait...
    Stack<PendingLockHold> reacquireNodes = new Stack<PendingLockHold>();
    try {
      synchronized (this) {
        boolean holdsWrite = false;
        for (LockStateNode s : this) {
          // find all this threads holds...
          if ((s instanceof LockHold) && s.getOwner().equals(thread)) {
            LockHold hold = (LockHold) s;
            if (hold.getLockLevel().isWrite()) {
              holdsWrite = true;
            }
            //if the unlocking of this hold triggers a flush requirement then do it now
            if (flushOnUnlock(hold)) {
              if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " flushing transactions on wait");
              doFlush(remote);
            }
            //remove the hold from the state and push into the reacquire stack
            remove(hold);
            reacquireNodes.push(new MonitorBasedPendingLockHold(thread, hold.getLockLevel(), waitObject, -1));
          }
        }
        // add the lock waiter node to the state (and attach the reacquire stack to it - the notifying thread will need the stack later)
        node = new LockWaiter(thread, waitObject, reacquireNodes, timeout);        
        addLast(node);
        
        // if no write locks were held then throw here
        if (!holdsWrite) { throw new IllegalMonitorStateException(); }
        
        // potentially (dependent on greediness state) communicate with the server here
        if (greediness.isFree()) {
          remote.wait(lock, thread, timeout);
        } else if (greediness.isRecalled()) {
          greediness = doRecall(remote);
        }
        
        // kick the queue to allow queued acquires to grab the lock
        unparkNextQueuedAcquire();
      }
      
      //horrible listener call used by the test code
      listener.handleWaitEvent();
      
      try {
        if (timeout < 0) {
          node.park();
        } else {
          node.park(timeout);
        }
      } catch (InterruptedException e) {
        synchronized (this) {
          // potentially tell the server that the wait was interrupted - otherwise we may end up waiting for the locks to awarded by the server 
          if (greediness.isFree()) {
            remote.interrupt(lock, thread);
          }
        }
        throw e;
      }
    } finally {
      synchronized (this) {
        // ensure that our reacquire nodes have been moved into the queue
        if (node != null) {
          notify(node);
        }
      }
      //reacquire the locks in their original order using the already in place queue nodes
      while (!reacquireNodes.isEmpty()) {
        PendingLockHold qa = reacquireNodes.pop();
        try {
          acquireQueued(remote, thread, qa.getLockLevel(), qa, false);
        } catch (GarbageLockException e) {
          throw new AssertionError(e);
        }
      }
    }
  }

  public synchronized Collection<ClientServerExchangeLockContext> getStateSnapshot(ClientID client) {
    Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();

    switch (greediness) {
      case RECALLED_READ:
      case READ_RECALL_IN_PROGRESS:
      case GREEDY_READ:
        contexts.add(new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID, ServerLockContext.State.GREEDY_HOLDER_READ));
        break;
      case RECALLED_WRITE:        
      case WRITE_RECALL_IN_PROGRESS:
      case GREEDY_WRITE:
        contexts.add(new ClientServerExchangeLockContext(lock, client, ThreadID.VM_ID, ServerLockContext.State.GREEDY_HOLDER_WRITE));
        break;
      case GARBAGE:
        return Collections.emptyList();
      case FREE:
        break;
    }

    for (LockStateNode s : this) {
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
        LockWaiter lw = (LockWaiter) s;
        contexts.add(new ClientServerExchangeLockContext(lock, client, lw.getOwner(), ServerLockContext.State.WAITER, lw.getTimeout()));
      } else if (s instanceof PendingLockHold) {
        switch (((PendingLockHold) s).getLockLevel()) {
          case READ:
            PendingLockHold qla = (PendingLockHold) s;
            if (qla.getTimeout() < 0) {
              contexts.add(new ClientServerExchangeLockContext(lock, client, s.getOwner(), ServerLockContext.State.PENDING_READ));
            } else {
              contexts.add(new ClientServerExchangeLockContext(lock, client, s.getOwner(), ServerLockContext.State.TRY_PENDING_READ, qla.getTimeout()));
            }
            break;
          case WRITE:
          case SYNCHRONOUS_WRITE:
            qla = (PendingLockHold) s;
            if (qla.getTimeout() < 0) {
              contexts.add(new ClientServerExchangeLockContext(lock, client, s.getOwner(), ServerLockContext.State.PENDING_WRITE));
            } else {
              contexts.add(new ClientServerExchangeLockContext(lock, client, s.getOwner(), ServerLockContext.State.TRY_PENDING_WRITE, qla.getTimeout()));
            }
            break;
          default:
            throw new AssertionError();
        }
      }
    }
    
    return contexts;
  }

  public synchronized int pendingCount() {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : getting pending count");
    int penders = 0;
    for (LockStateNode s : this) {
      if (s instanceof PendingLockHold) {
        penders++;
      }
    }
    return penders;
  }

  public synchronized int waitingCount() {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : getting waiting count");
    int waiters = 0;
    for (LockStateNode s : this) {
      if (s instanceof LockWaiter) {
        waiters++;
      }
    }
    return waiters;
  }

  public synchronized boolean isLocked(LockLevel level) {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : getting isLocked " + level);
    for (LockStateNode s : this) {
      if ((s instanceof LockHold) && (((LockHold) s).getLockLevel().equals(level))) {
        return true;
      } else if (s instanceof LockWaiter) {
        break;
      } else if (s instanceof PendingLockHold) {
        break;
      }
    }
    return false;
  }

  public synchronized boolean isLockedBy(ThreadID thread, LockLevel level) {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : getting isLocked " + level + " by " + thread);
    for (LockStateNode s : this) {
      if ((s instanceof LockHold) && (((LockHold) s).getLockLevel().equals(level) || (level == null)) && s.getOwner().equals(thread)) {
        return true;
      } else if (s instanceof LockWaiter) {
        break;
      } else if (s instanceof PendingLockHold) {
        break;
      }
    }
    return false;
  }

  public synchronized int holdCount(LockLevel level) {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : getting hold count @ " + level);
    int holders = 0;
    for (LockStateNode s : this) {
      if ((s instanceof LockHold) && ((LockHold) s).getLockLevel().equals(level)) {
        holders++;
      } else if (s instanceof LockWaiter) {
        break;
      } else if (s instanceof PendingLockHold) {
        break;
      }
    }
    return holders;
  }

  public void pinLock() {
    pinned = true;
  }

  public void unpinLock() {
    pinned = false;
  }

  /*
   * Called by the stage thread (the transaction apply thread) when the server wishes to notify a thread waiting on this lock
   */
  public void notified(ThreadID thread) {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server notifying " + thread);
    synchronized (this) {
      for (LockStateNode s : this) {
        if ((s instanceof LockWaiter) && s.getOwner().equals(thread)) {
          final LockWaiter waiter = (LockWaiter) s;
          // move the waiting nodes reacquires into the queue in this thread so we can be certain that the lock state has changed by the time the server gets the txn ack.
          notify(waiter);
          waiter.unpark();
          break;
        }
      }
    }
  }

  /*
   * Move the given waiters reacquire nodes into the queue
   */
  private synchronized void notify(LockWaiter waiter) {
    if (remove(waiter) != null) {
      Stack<PendingLockHold> reacquires = waiter.getReacquires();
      for (int i = reacquires.size() - 1; i >= 0; i--) {
        addLast(reacquires.get(i));
      }
    }
  }
  
  /*
   * Called by the locking stage thread to request a greedy recall.
   */
  public synchronized void recall(final RemoteLockManager remote, final ServerLockLevel interest, int lease) {
    // transition the greediness state
    greediness = greediness.recall(this, interest, lease);
    addRecalledLevel(interest);

    if (greediness.isRecalled()) {
      if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server requested recall " + interest);
      greediness = doRecall(remote);
    } else if (greediness.isGreedy()) {
      if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server granted leased " + interest);
      // schedule the greedy lease
      LOCK_TIMER.schedule(new TimerTask() {
        @Override
        public void run() {
          if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(ClientLockImpl.this) + "] : doing recall commit after lease expiry");
          recall(remote, interest, -1);
        }
      }, lease);
    } else {
      System.err.println("XXXXXXXX Greedy recall of " + lock + " when " + greediness + " XXXXXXXX");
    }
  }

  /*
   * Called by the stage thread to indicate that the tryLock attempt has failed.
   */
  public void refuse(ThreadID thread, ServerLockLevel level) {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : server refusing lock request " + level);
    // kick the locking thread
    unparkQueuedAcquire(thread);
  }

  /*
   * Called by the stage thread when the server has awarded a lock (either greedy or per thread).
   */
  public synchronized void award(RemoteLockManager remote, ThreadID thread, ServerLockLevel level) {
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
        remote.unlock(lock, thread, level);
      }
    }
  }

  /**
   * Our locks behave in a slightly bizarre way - we don't queue very strictly, if the head of the
   * acquire queue fails, we allow acquires further down to succeed.  This is different to the JDK
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
      // because of our loose queuing everything except a exclusive acquire is `shared'
//      return this != SUCCEEDED;
      return this == SUCCEEDED_SHARED;
    }

    public boolean succeeded() {
      return (this == SUCCEEDED) | (this == SUCCEEDED_SHARED);
    }
    
    public boolean delegated() {
      return this == DELEGATED;
    }
  }

  /*
   * Try to acquire the lock (optionally with delegation to the server)
   */
  private AcquireResult tryAcquire(boolean delegate, RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws GarbageLockException {
    if (delegate) {
      return tryAcquireWithDelegation(remote, thread, level, timeout);
    } else {
      return tryAcquireLocally(thread, level);
    }
  }
  
  /*
   * Attempt to acquire the lock at the given level locally
   */
  private AcquireResult tryAcquireLocally(ThreadID thread, LockLevel level) {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " attempting to acquire " + level);
    // if this is a concurrent acquire then just let it through.
    if (level == LockLevel.CONCURRENT) {
      return AcquireResult.SUCCEEDED_SHARED;
    }
    
    synchronized (this) {
      //What can we glean from local lock state
      LockHold newHold = new LockHold(thread, level);
      LockLevel firstHold = null;
      for (Iterator<LockStateNode> it = iterator(); it.hasNext();) {
        LockStateNode s = it.next();
        if (s instanceof LockHold) {
          if (s.getOwner().equals(thread)) {
            firstHold = ((LockHold) s).getLockLevel();
          }
          
          AcquireResult result = ((LockHold) s).allowsHold(newHold);
          if (!result.delegated()) {
            if (result.succeeded()) {
              addFirst(newHold);
              if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " awarded " + level + " due to existing thread hold " + s);
            } else {
              if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " denied " + level + " due to existing thread hold " + s);
            }
            return result;
          }
        } else if (s instanceof LockAward) {
          AcquireResult result = ((LockAward) s).allowsHold(newHold);
          if (!result.delegated()) {
            if (result.succeeded()) {
              if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " awarded " + level + " due to per thread award");
              it.remove();
              addFirst(newHold);
            }
            return result;
          }
        } else {
          break;
        }
      }
  
      //Lock upgrade not supported check
      if (level.isWrite() && (firstHold != null) && firstHold.isRead()) {
        throw new TCLockUpgradeNotSupportedError();
      }
      
      //Thread level lock state did not give us a definitive answer
      if (greediness.canAward(level)) {
        if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " awarded " + level + " due to client greedy hold");
        addFirst(newHold);
        return level.isWrite() ? AcquireResult.SUCCEEDED : AcquireResult.SUCCEEDED_SHARED;
      } else {
        return AcquireResult.DELEGATED;
      }
    }
  }
  
  /*
   * Try to acquire the lock and delegate to the server if necessary
   */
  private synchronized AcquireResult tryAcquireWithDelegation(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws GarbageLockException {
    // try to do things locally first...
    AcquireResult local = tryAcquireLocally(thread, level);
    if (local.delegated()) {
      // if local calls for delegation then fire into the server.
      if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + thread + " denied " + level + " - contacting server...");
      ServerLockLevel requestLevel = ServerLockLevel.fromClientLockLevel(level);
      
      greediness = greediness.requestLevel(requestLevel);      
      if (greediness.isFree()) {
        switch ((int) timeout) {
          case ClientLockImpl.BLOCKING_LOCK:
            remote.lock(lock, thread, requestLevel);
            break;
          default:
            remote.tryLock(lock, thread, requestLevel, timeout);
            break;
        }
      } else if (greediness.isRecalled()) {
        addRecalledLevel(requestLevel);
        if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : client initiated recall " + requestLevel);
        greediness = doRecall(remote);
      }      
      return AcquireResult.DELEGATED;
    } else {
      return local;
    }
  }
  
  /*
   * Unlock and return true if acquires might now succeed.
   */
  private boolean release(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    // concurrent unlocks are implicitly okay - we don't monitor concurrent locks
    if (level == LockLevel.CONCURRENT) {
      // concurrent unlocks do not change the state - no reason why queued acquires would succeed
      return false;
    }
    
    LockHold unlock = null;
    synchronized (this) {
      for (Iterator<LockStateNode> it = iterator(); it.hasNext();) {
        LockStateNode s = it.next();
        if (s instanceof LockHold) {
          LockHold hold = (LockHold) s;
          if (hold.getOwner().equals(thread) && hold.getLockLevel().equals(level)) {
            unlock = hold;
            break;
          }
        }
      }
      
      if (unlock == null) {
        throw new IllegalMonitorStateException();
      }
      
      if (!flushOnUnlock(unlock)) {
        return doRelease(remote, unlock);
      }
    }

    remote.flush(lock);
    
    synchronized (this) {
      return doRelease(remote, unlock);
    }
  }

  private synchronized boolean doRelease(RemoteLockManager remote, LockHold unlock) {
    if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + unlock.getOwner() + " unlocking " + unlock.getLockLevel());
    remove(unlock);
    if (greediness.isFree()) {
      remoteUnlock(remote, unlock);
    } else if (greediness.isRecalled()) {
      greediness = doRecall(remote);
    }
    
    if (DEBUG) System.err.println("\t" + ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : " + unlock.getOwner() + " unlocked " + unlock.getLockLevel());
    // this is wrong - but shouldn't break anything
    return true;
  }
  
  private void remoteUnlock(RemoteLockManager remote, LockHold unlock) {
    for (LockStateNode s : this) {
      if (s == unlock) continue;
      
      if (s instanceof LockHold && s.getOwner().equals(unlock.getOwner())) {
        LockHold hold = (LockHold) s;
        if (unlock.getLockLevel().isWrite()) {
          if (hold.getLockLevel().isWrite()) return;
        } else {
          return;
        }
      }
    }

    remote.unlock(lock, unlock.getOwner(), ServerLockLevel.fromClientLockLevel(unlock.getLockLevel()));
  }
  
  private synchronized boolean flushOnUnlock(LockHold unlock) {
    if (unlock.getLockLevel().flushOnUnlock()) {
      return true;
    }
    
    if (!greediness.flushOnUnlock()) {
      return false;
    }
    
    if (unlock.getLockLevel().isRead()) {
      return false;
    }
      
    for (LockStateNode s : this) {
      if (s == unlock) continue;

      if (s instanceof LockHold && s.getOwner().equals(unlock.getOwner())) {
        if (((LockHold) s).getLockLevel().isWrite()) return false;
      }
    }
    return true;
  }
  
  /*
   * Conventional acquire queued - uses a LockSupport based queue object.
   */
  private void acquireQueued(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException {
    final PendingLockHold node = new PendingLockHold(thread, level, -1);
    synchronized (this) {
      addLast(node);
    }
    acquireQueued(remote, thread, level, node, true);
  }

  /*
   * Generic acquire - uses an already existing queued node - used during wait notify
   */
  private void acquireQueued(RemoteLockManager remote, ThreadID thread, LockLevel level, PendingLockHold node, boolean delegate) throws GarbageLockException {    
    boolean interrupted = false;
    try {
      for (;;) {
        synchronized (this) {
          // try to acquire before sleeping
          AcquireResult result = tryAcquire(delegate, remote, thread, level, BLOCKING_LOCK);
          if (result.delegated()) {
            // we contacted server - disable delegation to prevent multiple messages
            delegate = false;
          }
          if (result.shared()) {
            unparkNextQueuedAcquire(node.getNext());
          }
          if (result.succeeded()) {
            // we succeeded return interrupted state
            remove(node);
            return;
          }
        }
        // park the thread and wait for unpark
        node.park();
        if (Thread.interrupted()) {
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
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /*
   * Just like acquireQueued but throws InterruptedException if unparked by interrupt rather then saving the interrupt state
   */
  private void acquireQueuedInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException, GarbageLockException {
    final PendingLockHold node = new PendingLockHold(thread, level, -1);
    synchronized (this) {
      addLast(node);
    }
    try {
      boolean delegate = true;
      for (;;) {
        synchronized (this) {
          AcquireResult result = tryAcquire(delegate, remote, thread, level, BLOCKING_LOCK);
          if (result.delegated()) {
            delegate = false;
          }
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

  /*
   * Acquire queued - waiting for at most timeout milliseconds.
   */
  private boolean acquireQueuedTimeout(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws InterruptedException, GarbageLockException {
    long lastTime = System.currentTimeMillis();
    final PendingLockHold node = new PendingLockHold(thread, level, timeout);
    synchronized (this) {
      addLast(node);
    }
    try {
      boolean delegate = true;
      for (;;) {
        synchronized (this) {
          AcquireResult result = tryAcquire(delegate, remote, thread, level, timeout);
          if (result.delegated()) {
            delegate = false;
          }
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

  /*
   * Unpark the first queued acquire
   */
  private synchronized void unparkNextQueuedAcquire() {
    if (!isEmpty()) {
      unparkNextQueuedAcquire(getFirst());
    }
  }

  /*
   * Unpark the next queued acquire (after supplied node)
   */
  private synchronized void unparkNextQueuedAcquire(LockStateNode node) {
    while (node != null) {
      if (node instanceof PendingLockHold) {
        ((PendingLockHold) node).acked();
        ((PendingLockHold) node).unpark();
        if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : unparked " + node.getOwner() + " wanting " + ((PendingLockHold) node).getLockLevel());
        return;
      }
      node = node.getNext();
    }
  }

  /*
   * Unpark the queued acquire associated with the given thread - used by per-thread awards
   */
  private synchronized boolean unparkQueuedAcquire(ThreadID thread) {
    for (LockStateNode s : this) {
      if ((s instanceof PendingLockHold) && s.getOwner().equals(thread)) {
        PendingLockHold acquire = (PendingLockHold) s;
        acquire.acked();
        acquire.unpark();
        if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : unparked " + thread + " wanting " + ((PendingLockHold) s).getLockLevel());
        return true;
      }
    }
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : failed to unpark " + thread);
    return false;
  }

  private synchronized ClientGreediness doRecall(final RemoteLockManager remote) {
    if (canRecallNow(recalledLevel)) {
      doFlush(remote);
      if (remote.isTransactionsForLockFlushed(lock, new LockFlushCallback() {
        public void transactionsForLockFlushed(LockID id) {
          synchronized (ClientLockImpl.this) {
            if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : doing recall commit (having flushed transactions)");
            greediness = recallCommit(remote);
          }
        }
      })) {
        if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : doing recall commit " + greediness);
        return recallCommit(remote);
      }
      return greediness.recallInProgress();
    } else {
      if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : cannot recall right now");
      return this.greediness;
    }
  }

  private synchronized ClientGreediness recallCommit(RemoteLockManager remote) {
    if (!ClientGreediness.FREE.equals(greediness)) {
      Collection<ClientServerExchangeLockContext> contexts = getFilteredStateSnapshot(remote.getClientID(), false);
      if (DEBUG) {
        synchronized (System.err) {
          System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : recalling :");
          for (ClientServerExchangeLockContext c : contexts) {
            System.err.println("\t" + c);
          }
        }
      }
      remote.recallCommit(lock, contexts);
      if (DEBUG) System.err.println(ManagerUtil.getClientID() + " : " + lock + "[" + System.identityHashCode(this) + "] : free'd greedy lock");
      
      recalledLevel = null;
    }
    return ClientGreediness.FREE;
  }
  
  private synchronized void addRecalledLevel(ServerLockLevel level) {
    if (recalledLevel == null) {
      recalledLevel = level;
    } else if (level == ServerLockLevel.WRITE) {
      recalledLevel = level;
    }
  }
  
  private synchronized boolean canRecallNow(ServerLockLevel level) {
    for (LockStateNode s : this) {
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
  private void doFlush(RemoteLockManager remote) {
    UnsafeUtil.monitorExit(this);
    try {
      remote.flush(lock);
    } finally {
      UnsafeUtil.monitorEnter(this);
    }
  }
  
  public synchronized boolean tryMarkAsGarbage(RemoteLockManager remote) {
    if (!pinned && isEmpty() && gcCycleCount > 0) {
      greediness = doRecall(remote);
      greediness = ClientGreediness.GARBAGE;
      return true;
    } else {
      gcCycleCount = (byte) Math.max(Byte.MAX_VALUE, gcCycleCount++);
      return false;
    }
  }  
  
  private void markUsed() {
    gcCycleCount = 0;
  }

  public synchronized void initializeHandshake(ClientID client, ClientHandshakeMessage message) {
    Collection<ClientServerExchangeLockContext> contexts = getFilteredStateSnapshot(client, true);

    for (ClientServerExchangeLockContext c : contexts) {
      if (DEBUG) System.err.println("Handshaking : " + ManagerUtil.getClientID() + " : " + c);
      message.addLockContext(c);
    }
  }
  
  private synchronized Collection<ClientServerExchangeLockContext> getFilteredStateSnapshot(ClientID client, boolean greedy) {
    Collection<ClientServerExchangeLockContext> fullState = getStateSnapshot(client);
    Collection<ClientServerExchangeLockContext> legacyState = new ArrayList();
    
    Map<ThreadID, ClientServerExchangeLockContext> holds = new HashMap<ThreadID, ClientServerExchangeLockContext>();
    Map<ThreadID, ClientServerExchangeLockContext> pends = new HashMap<ThreadID, ClientServerExchangeLockContext>();
    
    for (ClientServerExchangeLockContext context : fullState) {
      switch (context.getState()) {
        case HOLDER_READ:
          if (holds.get(context.getThreadID()) == null) {
            holds.put(context.getThreadID(), context);
          }
          break;
        case HOLDER_WRITE:
          holds.put(context.getThreadID(), context);
          break;
        case PENDING_READ:
        case TRY_PENDING_READ:
          if (pends.get(context.getThreadID()) == null) {
            pends.put(context.getThreadID(), context);
          }
          break;
        case PENDING_WRITE:
        case TRY_PENDING_WRITE:
          pends.put(context.getThreadID(), context);
          break;
        case WAITER:
          legacyState.add(context);
          break;
        case GREEDY_HOLDER_READ:
        case GREEDY_HOLDER_WRITE:
          if (greedy) {
            return Collections.singletonList(context);
          }
          break;
      }
    }
    legacyState.addAll(holds.values());
    legacyState.addAll(pends.values());
    
    return legacyState;
  }
  
  public synchronized String toString() {
    StringBuilder sb = new StringBuilder();
    
    sb.append("SynchronizedClientLock : ").append(lock).append('\n');
    sb.append("GC Cycle Count : ").append(gcCycleCount).append('\n');
    sb.append("Greediness : ").append(greediness).append('\n');
    sb.append("State:").append('\n');
    for (LockStateNode s : this) {
      sb.append('\t').append(s).append('\n');
    }
    
    return sb.toString();
  }
}