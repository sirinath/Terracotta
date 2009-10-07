/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.ClientLockImpl.AcquireResult;
import com.tc.util.SinglyLinkedList;

import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.LockSupport;

import junit.framework.Assert;

abstract class LockStateNode implements SinglyLinkedList.LinkedNode<LockStateNode> {
  private static final Timer STATE_NODE_TIMER = new Timer("ClientLockImpl Timer", true);

  private final ThreadID     owner;
  
  private LockStateNode      next;

  LockStateNode(ThreadID owner) {
    this.owner = owner;
    this.next = null;
  }
  
  /**
   * @throws InterruptedException can be thrown by certain subclasses
   */
  void park() throws InterruptedException {
    throw new AssertionError();
  }

  /**
   * @throws InterruptedException can be thrown by certain subclasses
   */
  void park(long timeout) throws InterruptedException {
    throw new AssertionError();
  }

  void unpark() {
    throw new AssertionError();
  }
  
  ThreadID getOwner() {
    return owner;
  }
  
  public LockStateNode getNext() {
    return next;
  }

  public LockStateNode setNext(LockStateNode newNext) {
    LockStateNode old = next;
    next = newNext;
    return old;
  }
  
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof LockStateNode) {
      return (owner.equals(((LockStateNode) o).owner));
    } else {
      return false;
    }
  }
  
  public String toString() {
    return getClass().getSimpleName() + " : " + owner;
  }
  
  static class LockHold extends LockStateNode {
    private final LockLevel level;
    
    LockHold(ThreadID owner, LockLevel level) {
      super(owner);
      this.level = level;
    }
    
    LockLevel getLockLevel() {
      return level;
    }
    
    AcquireResult allowsHold(LockHold newHold) {
      if (getOwner().equals(newHold.getOwner())) {
        if (newHold.getLockLevel().isRead()) {
          return getLockLevel().isWrite() ? AcquireResult.SUCCEEDED : AcquireResult.SUCCEEDED_SHARED;
        }
        if (level.isWrite()) {
          return AcquireResult.SUCCEEDED;
        }
      } else {
        if (level.isWrite()) {
          return AcquireResult.FAILED;
        }
      }
      
      return AcquireResult.DELEGATED;
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
    
    public int hashCode() {
      return (5 * super.hashCode()) ^ (7 * level.hashCode());
    }
    
    public String toString() {
      return super.toString() + " : " + level;
    }
  }
  
  static class PendingLockHold extends LockStateNode {
    private final LockLevel  level;
    private final Thread     javaThread;
    private final long       waitTime;
    private volatile boolean serverResponded = false;
    
    PendingLockHold(ThreadID owner, LockLevel level, long timeout) {
      super(owner);
      this.javaThread = Thread.currentThread();
      this.level = level;
      this.waitTime = timeout;
    }
    
    LockLevel getLockLevel() {
      return level;
    }
    
    Thread getJavaThread() {
      return javaThread;
    }
    
    long getTimeout() {
      return waitTime;
    }
    
    void park() {
      Assert.assertEquals(getJavaThread(), Thread.currentThread());
      LockSupport.park();
    }
    
    void park(long timeout) {
      Assert.assertEquals(getJavaThread(), Thread.currentThread());
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
      } else if (o instanceof PendingLockHold) {
        return super.equals(o) && level.equals(((PendingLockHold) o).level);
      } else {
        return false;
      }
    }
    
    public int hashCode() {
      return (5 * super.hashCode()) ^ (7 * level.hashCode());
    }
        
    public String toString() {
      return super.toString() + " : " + level;
    }
  }
  
  static class MonitorBasedPendingLockHold extends PendingLockHold {

    private final Object waitObject;
    private boolean      unparked = false;
    
    MonitorBasedPendingLockHold(ThreadID owner, LockLevel level, Object waitObject, long timeout) {
      super(owner, level, timeout);
      if (waitObject == null) {
        this.waitObject = this;
      } else {
        this.waitObject = waitObject;
      }
    }
    
    void park() {
      synchronized (waitObject) {
        while (!unparked) {
          try {
            waitObject.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
        unparked = false;
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
      STATE_NODE_TIMER.schedule(new TimerTask() {
        @Override
        public void run() {
          synchronized (waitObject) {
            unparked = true;
            waitObject.notifyAll();
          }
        }
      }, 0);
    }
  }
  
  static class LockWaiter extends LockStateNode {
    
    private final Object waitObject;
    private final long   waitTime;
    private final Stack<PendingLockHold> reacquires;
    
    private boolean      notified;
    
    LockWaiter(ThreadID owner, Object waitObject, Stack<PendingLockHold> reacquires, long timeout) {
      super(owner);
      if (waitObject == null) {
        this.waitObject = this;
      } else {
        this.waitObject = waitObject;
      }
      this.reacquires = reacquires;
      this.waitTime = timeout;
    }
    
    long getTimeout() {
      return waitTime;
    }

    Stack<PendingLockHold> getReacquires() {
      return reacquires;
    }
    
    void park() throws InterruptedException {
      synchronized (waitObject) {
        while (!notified) {
          waitObject.wait();
        }
      }
    }

    void park(long timeout) throws InterruptedException {
      synchronized (waitObject) {
        waitObject.wait(timeout);
      }
    }
    
    void unpark() {
      STATE_NODE_TIMER.schedule(new TimerTask() {
        @Override
        public void run() {
          synchronized (waitObject) {
            notified = true;
            waitObject.notifyAll();
          }
        }
      }, 0);
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
    
    public int hashCode() {
      return super.hashCode();
    }
  }
  
  static class LockAward extends LockStateNode {
    private final ServerLockLevel level;
    
    LockAward(ThreadID target, ServerLockLevel level) {
      super(target);
      this.level = level;
    }
    
    ServerLockLevel getLockLevel() {
      return level;
    }
    
    AcquireResult allowsHold(LockHold newHold) {
      if (getOwner().equals(newHold.getOwner())) {
        switch (newHold.getLockLevel()) {
          case READ:
            if (level.equals(ServerLockLevel.READ)) {
              return AcquireResult.SUCCEEDED_SHARED;
            }
            break;
          case SYNCHRONOUS_WRITE:
          case WRITE:
            if (level.equals(ServerLockLevel.WRITE)) {
              return AcquireResult.SUCCEEDED;
            }
            break;
          default:
            throw new AssertionError();
        }
      }
      return AcquireResult.DELEGATED;
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
    
    public int hashCode() {
      return (5 * super.hashCode()) ^ (7 * level.hashCode());
    }
    
    public String toString() {
      return super.toString() + " : " + level;
    }
  }  
}