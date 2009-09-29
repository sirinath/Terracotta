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

    ClientGreediness recall(ClientLock clientLock, ServerLockLevel interest, int lease) {
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

    ClientGreediness unlocked(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      //must do remote unlock if downgraded or free
      //downgraded : hold is WRITE/SYNCWRITE and now holding just read
      //free : no holds
      //XXX this code is really ugly
      if (clientLock.isLockedBy(hold.getOwner(), LockLevel.WRITE)) {
        return this;
      } else if (clientLock.isLockedBy(hold.getOwner(), LockLevel.SYNCHRONOUS_WRITE)) {
        return this;
      } else if (clientLock.isLockedBy(hold.getOwner(), LockLevel.READ) && hold.getLockLevel().isWrite()) {
        clientLock.doFlush(remote);
        remote.unlock(lock, hold.getOwner(), ServerLockLevel.WRITE);
      } else if (!clientLock.isLockedBy(hold.getOwner(), LockLevel.READ)) {
        clientLock.doFlush(remote);
        remote.unlock(lock, hold.getOwner(), ServerLockLevel.READ);
      }
      return this;
    }

    ClientGreediness waiting(RemoteLockManager remote, LockID lock, SynchronizedClientLock clientLock, LockHold hold) {
      if (clientLock.isLockedBy(hold.getOwner(), LockLevel.WRITE)) {
        return this;
      } else if (clientLock.isLockedBy(hold.getOwner(), LockLevel.SYNCHRONOUS_WRITE)) {
        return this;
      } else if (clientLock.isLockedBy(hold.getOwner(), LockLevel.READ) && hold.getLockLevel().isWrite()) {
        clientLock.doFlush(remote);
        remote.wait(lock, hold.getOwner(), -1);
      } else if (!clientLock.isLockedBy(hold.getOwner(), LockLevel.READ)) {
        clientLock.doFlush(remote);
        remote.wait(lock, hold.getOwner(), -1);
      }
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
    
    ClientGreediness recall(ClientLock clientLock, ServerLockLevel interest, int lease) {
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
    
    ClientGreediness recall(ClientLock clientLock, ServerLockLevel interest, int lease) {
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
  },
  
  LEASED_GREEDY_WRITE {

    @Override
    public boolean canAward(LockLevel level) {
      return true;
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

  ClientGreediness recall(ClientLock clientLock, ServerLockLevel interest, int lease) {
    System.err.println(this + " recall - WTF!");
    throw new AssertionError();
  }

  ClientGreediness interrupt(RemoteLockManager remote, LockID lock, ThreadID thread) {
    return this;
  }
}
