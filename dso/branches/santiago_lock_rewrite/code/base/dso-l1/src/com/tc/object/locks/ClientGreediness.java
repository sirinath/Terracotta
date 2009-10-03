/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.SynchronizedClientLock.LockHold;

enum ClientGreediness {
  GARBAGE {
    @Override
    ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) throws GarbageLockException {
      throw GarbageLockException.GARBAGE_LOCK_EXCEPTION;
    }
  },
  
  FREE {
    @Override
    ClientGreediness award(ServerLockLevel level) {
      switch (level) {
        case READ:
          return GREEDY_READ;
        case WRITE:
          return GREEDY_WRITE;
        default:
          throw new AssertionError();
      }
    }

    @Override
    ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) {
      ServerLockLevel requestLevel;
      switch (level) {
        case READ:
          requestLevel = ServerLockLevel.READ;
          break;
        case SYNCHRONOUS_WRITE:
        case WRITE:
          requestLevel = ServerLockLevel.WRITE;
          break;
        default:
          throw new AssertionError();
      }
      
      switch ((int) timeout) {
        case SynchronizedClientLock.BLOCKING_LOCK:
          remote.lock(lock, thread, requestLevel);
          break;
        default:
          remote.tryLock(lock, thread, requestLevel, timeout);
          break;
      }
      return this;
    }

    @Override
    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold unlock) {
      //must do remote unlock if downgraded or free
      //downgraded : hold is WRITE/SYNCWRITE and now holding just read
      //free : no holds
      for (State s : clientLock) {
        if (s == unlock) continue;
        
        if (s instanceof LockHold && s.getOwner().equals(unlock.getOwner())) {
          LockHold hold = (LockHold) s;
          if (unlock.getLockLevel().isWrite()) {
            if (hold.getLockLevel().isWrite()) return this;
          } else {
            return this;
          }
        }
      }

      if (unlock.getLockLevel().isWrite()) {
        remote.unlock(lock, unlock.getOwner(), ServerLockLevel.WRITE);
      } else {
        remote.unlock(lock, unlock.getOwner(), ServerLockLevel.READ);
      }
      return this;
    }

    @Override
    ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, ThreadID thread, long timeout) {
      remote.wait(lock, thread, timeout);
      return this;
    }
    
    @Override
    ClientGreediness interrupt(RemoteLockManager remote, LockID lock, ThreadID thread) {
      remote.interrupt(lock, thread);
      return this;
    }    
  },
  
  GREEDY_READ {

    @Override
    public boolean canAward(LockLevel level) {
      return level.isRead();
    }
    
    @Override
    ClientGreediness award(ServerLockLevel level) {
      switch (level) {
        case READ: return GREEDY_READ;
        case WRITE: return GREEDY_WRITE;
        default: throw new AssertionError();
      }
    }

    @Override
    ClientGreediness recall(SynchronizedClientLock clientLock, ServerLockLevel interest, int lease) {
      return RECALLED_READ;
    }

    @Override
    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      return this;
    }
    
    @Override
    ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) {
      return RECALLED_READ;
    }
    
    @Override
    boolean flushOnUnlock(SynchronizedClientLock clientLock, LockHold unlock) {
      return false;
    }
  },

  // I believe that LEASED_GREEDY_READ cannot exist (or at least cannot be entered)
//  LEASED_GREEDY_READ {
//
//    @Override
//    public boolean canAward(LockLevel level) {
//      return level.isRead();
//    }
//  }, 

  GREEDY_WRITE {

    @Override
    public boolean canAward(LockLevel level) {
      return true;
    }
    
    @Override
    ClientGreediness recall(SynchronizedClientLock clientLock, ServerLockLevel interest, int lease) {
      if ((lease > 0) && (clientLock.pendingCount() > 0)) {
        return LEASED_GREEDY_WRITE;
      } else {
        return RECALLED_WRITE;
      }
    }

    @Override
    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      return this;
    }
    
    @Override
    ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, ThreadID thread, long timeout) {
      return this;
    }
    
    @Override
    boolean flushOnUnlock(SynchronizedClientLock clientLock, LockHold unlock) {
      return false;
    }
  },
  
  LEASED_GREEDY_WRITE {

    @Override
    public boolean canAward(LockLevel level) {
      return true;
    }
    
    @Override
    boolean flushOnUnlock(SynchronizedClientLock clientLock, LockHold unlock) {
      return false;
    }
  },
  
  RECALLED_READ {

    @Override
    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      return clientLock.doRecall(remote);
    }
    
    @Override
    ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) {
      return this; //lock is being recalled - we'll get the per thread awards from the server later
    }

    @Override
    ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, ThreadID thread, long timeout) {
      return clientLock.doRecall(remote);
    }
    
    @Override
    ClientGreediness recallInProgress() {
      return READ_RECALL_IN_PROGRESS;
    }

    @Override
    boolean isRecalled() {
      return true;
    }
  },

  RECALLED_WRITE {

    @Override
    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      return clientLock.doRecall(remote);
    }
    
    @Override
    ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) {
      return this; //lock is being recalled - we'll get the per thread awards from the server later
    }

    @Override
    ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, ThreadID thread, long timeout) {
      return clientLock.doRecall(remote);
    }

    @Override
    ClientGreediness recallInProgress() {
      return WRITE_RECALL_IN_PROGRESS;
    }

    @Override
    boolean isRecalled() {
      return true;
    }
  },
  
  READ_RECALL_IN_PROGRESS {

  },
  
  WRITE_RECALL_IN_PROGRESS {

  };
  
  boolean canAward(LockLevel level) {
    return false;
  }
  
  
  /**
   * @throws GarbageLockException thrown if in a garbage state 
   */
  ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) throws GarbageLockException {
    System.err.println("request level while in unexpected state (" + this + ")");
    throw new AssertionError();
  }

  ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
    System.err.println("unlocked while in unexpected state (" + this + ")");
    throw new AssertionError();
  }

  ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, ThreadID thread, long timeout) {
    System.err.println("waiting while in unexpected state (" + this + ")");
    throw new AssertionError();
  }
  
  ClientGreediness award(ServerLockLevel level) {
    System.err.println("award while in unexpected state (" + this + ")");
    throw new AssertionError();
  }

  ClientGreediness recall(SynchronizedClientLock clientLock, ServerLockLevel interest, int lease) {
    return this;
  }

  ClientGreediness interrupt(RemoteLockManager remote, LockID lock, ThreadID thread) {
    return this;
  }

  boolean flushOnUnlock(SynchronizedClientLock clientLock, LockHold unlock) {
    if (unlock.getLockLevel().isRead()) {
      return false;
    }
    
    synchronized (clientLock) {
      for (State s : clientLock) {
        if (s == unlock) continue;

        if (s instanceof LockHold && s.getOwner().equals(unlock.getOwner())) {
          if (((LockHold) s).getLockLevel().isWrite()) return false;
        }
      }
    }
    return true;
  }

  ClientGreediness recallInProgress() {
    System.err.println("recall in progress while in unexpected state (" + this + ")");
    throw new AssertionError();
  }

  boolean isRecalled() {
    return false;
  }
}
