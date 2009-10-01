/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.SynchronizedClientLock.LockHold;

enum ClientGreediness {
  GARBAGE {
    @Override
    public boolean canAward(LockLevel level) {
      throw new GarbageLockException();
    }
    
    ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) {
      throw new GarbageLockException();
    }

    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      throw new GarbageLockException();
    }

    ClientGreediness recall(SynchronizedClientLock clientLock, ServerLockLevel interest, int lease) {
      throw new GarbageLockException();
    }
    
    ClientGreediness interrupt(RemoteLockManager remote, LockID lock, ThreadID thread) {
      throw new GarbageLockException();
    }
  },
  
  FREE {
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
        case -1:
          remote.lock(lock, thread, requestLevel);
          break;
        case 0:
          remote.tryLock(lock, thread, requestLevel, -1L);
          break;
        default:
          remote.tryLock(lock, thread, requestLevel, timeout);
          break;
      }
      return this;
    }

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

    ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold unlock) {
      for (State s : clientLock) {
        if (s == unlock) continue;
        
        if (s instanceof LockHold && s.getOwner().equals(unlock.getOwner())) {
          LockHold hold = (LockHold) s;
          if (hold.getLockLevel().isWrite()) return this;
        }
      }

      remote.wait(lock, unlock.getOwner(), -1);
      return this;
    }
    
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
    
    ClientGreediness recall(SynchronizedClientLock clientLock, ServerLockLevel interest, int lease) {
      return RECALLED;
    }

    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      return this;
    }
    
    ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) {
      switch ((int) timeout) {
        case -1:
          remote.lock(lock, thread, ServerLockLevel.WRITE);
          break;
        case 0:
          remote.tryLock(lock, thread, ServerLockLevel.WRITE, -1L);
          break;
        default:
          remote.tryLock(lock, thread, ServerLockLevel.WRITE, timeout);
          break;
      }
      return this;
    }
    
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
    
    ClientGreediness recall(SynchronizedClientLock clientLock, ServerLockLevel interest, int lease) {
      if ((lease > 0) && (clientLock.pendingCount() > 0)) {
        return LEASED_GREEDY_WRITE;
      } else {
        return RECALLED;
      }
    }

    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      return this;
    }
    
    ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      return this;
    }
    
    boolean flushOnUnlock(SynchronizedClientLock clientLock, LockHold unlock) {
      return false;
    }
  },
  
  LEASED_GREEDY_WRITE {

    @Override
    public boolean canAward(LockLevel level) {
      return true;
    }
    
    boolean flushOnUnlock(SynchronizedClientLock clientLock, LockHold unlock) {
      return false;
    }
  },
  
  RECALLED {

    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      return clientLock.doRecall(remote);
    }
    
    ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) {
      return this; //lock is being recalled - we'll get the per thread awards from the server later
    }

    ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      return clientLock.doRecall(remote);
    }
  },
  
  RECALL_IN_PROGRESS {

  };
  
  boolean canAward(LockLevel level) {
    return false;
  }
  
  ClientGreediness requestLevel(RemoteLockManager remote, LockID lock, ThreadID thread, LockLevel level, long timeout) {
    System.err.println(this + " requestLevel - WTF!");
    throw new AssertionError();
  }

  ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
    System.err.println(this + " unlocked - WTF!");
    throw new AssertionError();
  }

  ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
    System.err.println(this + " waiting - WTF!");
    throw new AssertionError();
  }
  
  ClientGreediness award(ServerLockLevel level) {
    System.err.println(this + " award - WTF!");
    throw new AssertionError();
  }

  ClientGreediness recall(SynchronizedClientLock clientLock, ServerLockLevel interest, int lease) {
    System.err.println(this + " recall - WTF!");
    throw new AssertionError();
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
}
