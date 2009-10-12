/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

enum ClientGreediness {
  GARBAGE {
    boolean canAward(LockLevel level) {
      return false;
    }
    
    boolean isFree() {
      return false;
    }
    
    boolean isRecalled() {
      return false;
    }
    
    public boolean isGreedy() {
      return false;
    }
    
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    ClientGreediness requested(ServerLockLevel level) throws GarbageLockException {
      throw GarbageLockException.GARBAGE_LOCK_EXCEPTION;
    }
    
    ClientGreediness recalled(ClientLock clientLock, ServerLockLevel interest, int lease) {
      return this;
    }

    public ClientGreediness recallCommitted() {
      return GARBAGE;
    }
  },
  
  FREE {
    boolean canAward(LockLevel level) {
      return false;
    }
    
    boolean isFree() {
      return true;
    }
    
    boolean isRecalled() {
      return false;
    }
    
    public boolean isGreedy() {
      return false;
    }
    
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    ClientGreediness requested(ServerLockLevel level) {
      return this;
    }
    
    @Override
    ClientGreediness awarded(ServerLockLevel level) {
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
    ClientGreediness recalled(ClientLock clientLock, ServerLockLevel interest, int lease) {
      return this;
    }
  },
  
  GREEDY_READ {
    boolean canAward(LockLevel level) {
      return level.isRead();
    }
    
    boolean isFree() {
      return false;
    }
    
    boolean isRecalled() {
      return false;
    }
    
    public boolean isGreedy() {
      return true;
    }
    
    boolean flushOnUnlock() {
      return false;
    }

    @Override
    ClientGreediness requested(ServerLockLevel level) {
      switch (level) {
        case READ:
          return this;
        case WRITE:
          return RECALLED_READ;
        default:
          throw new AssertionError();
      }
    }
    
    @Override
    ClientGreediness awarded(ServerLockLevel level) {
      switch (level) {
        case READ: return GREEDY_READ;
        case WRITE: return GREEDY_WRITE;
        default: throw new AssertionError();
      }
    }

    @Override
    ClientGreediness recalled(ClientLock clientLock, ServerLockLevel interest, int lease) {
      return RECALLED_READ;
    }
  },

  GREEDY_WRITE {
    boolean canAward(LockLevel level) {
      return true;
    }
    
    boolean isFree() {
      return false;
    }
    
    boolean isRecalled() {
      return false;
    }
    
    public boolean isGreedy() {
      return true;
    }
    
    boolean flushOnUnlock() {
      return false;
    }

    @Override
    ClientGreediness recalled(ClientLock clientLock, ServerLockLevel interest, int lease) {
      if ((lease > 0) && (clientLock.pendingCount() > 0)) {
        return GREEDY_WRITE;
      } else {
        return RECALLED_WRITE;
      }
    }
  },
  
  RECALLED_READ {
    boolean canAward(LockLevel level) {
      return false;
    }
    
    boolean isFree() {
      return false;
    }
    
    boolean isRecalled() {
      return true;
    }
    
    public boolean isGreedy() {
      return false;
    }
    
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    ClientGreediness requested(ServerLockLevel level) {
      return this; //lock is being recalled - we'll get the per thread awards from the server later
    }

    @Override
    ClientGreediness recalled(ClientLock clientLock, ServerLockLevel interest, int lease) {
      return this;
    }
    
    @Override
    ClientGreediness recallInProgress() {
      return READ_RECALL_IN_PROGRESS;
    }

    @Override
    public ClientGreediness recallCommitted() {
      return FREE;
    }
  },

  RECALLED_WRITE {
    boolean canAward(LockLevel level) {
      return false;
    }
    
    boolean isFree() {
      return false;
    }
    
    boolean isRecalled() {
      return true;
    }
    
    public boolean isGreedy() {
      return false;
    }
    
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    ClientGreediness requested(ServerLockLevel level) {
      return this; //lock is being recalled - we'll get the per thread awards from the server later
    }

    @Override
    ClientGreediness recalled(ClientLock clientLock, ServerLockLevel interest, int lease) {
      return this;
    }
    
    @Override
    ClientGreediness recallInProgress() {
      return WRITE_RECALL_IN_PROGRESS;
    }

    @Override
    public ClientGreediness recallCommitted() {
      return FREE;
    }
  },
  
  READ_RECALL_IN_PROGRESS {
    boolean canAward(LockLevel level) {
      return false;
    }
    
    boolean isFree() {
      return false;
    }
    
    boolean isRecalled() {
      return false;
    }
    
    public boolean isGreedy() {
      return false;
    }
    
    boolean flushOnUnlock() {
      return true;
    }
    
    @Override
    ClientGreediness requested(ServerLockLevel level) throws GarbageLockException {
      return this;
    }

    @Override
    ClientGreediness recalled(ClientLock clientLock, ServerLockLevel interest, int lease) {
      return this;
    }
    
    @Override
    public ClientGreediness recallCommitted() {
      return FREE;
    }
  },
  
  WRITE_RECALL_IN_PROGRESS {
    boolean canAward(LockLevel level) {
      return false;
    }
    
    boolean isFree() {
      return false;
    }
    
    boolean isRecalled() {
      return false;
    }
    
    public boolean isGreedy() {
      return false;
    }
    
    boolean flushOnUnlock() {
      return true;
    }

    @Override
    ClientGreediness requested(ServerLockLevel level) throws GarbageLockException {
      return this;
    }

    @Override
    ClientGreediness recalled(ClientLock clientLock, ServerLockLevel interest, int lease) {
      return this;
    }
    
    @Override
    public ClientGreediness recallCommitted() {
      return FREE;
    }
  };
  
  abstract boolean canAward(LockLevel level);
  
  abstract boolean isFree();
  
  abstract boolean isRecalled();
  
  abstract public boolean isGreedy();
  
  abstract boolean flushOnUnlock();

  /**
   * @throws GarbageLockException thrown if in a garbage state 
   */
  ClientGreediness requested(ServerLockLevel level) throws GarbageLockException {
    throw new AssertionError("request level while in unexpected state (" + this + ")");
  }

  ClientGreediness awarded(ServerLockLevel level) {
    throw new AssertionError("award while in unexpected state (" + this + ")");
  }

  ClientGreediness recalled(ClientLock clientLock, ServerLockLevel interest, int lease) {
    throw new AssertionError("recalled while in unexpected state (" + this + ")");
  }

  ClientGreediness recallInProgress() {
    throw new AssertionError("recall in progress while in unexpected state (" + this + ")");
  }

  public ClientGreediness recallCommitted() {
    throw new AssertionError("recall committed while in unexpected state (" + this + ")");
  }
}
