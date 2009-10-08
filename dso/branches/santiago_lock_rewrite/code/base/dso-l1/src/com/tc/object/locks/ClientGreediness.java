/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

enum ClientGreediness {
  GARBAGE {
    @Override
    ClientGreediness requestLevel(ServerLockLevel level) throws GarbageLockException {
      throw GarbageLockException.GARBAGE_LOCK_EXCEPTION;
    }
  },
  
  FREE {
    @Override
    boolean isFree() {
      return true;
    }
    
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
    ClientGreediness requestLevel(ServerLockLevel level) {
      return this;
    }
  },
  
  GREEDY_READ {

    @Override
    public boolean canAward(LockLevel level) {
      return level.isRead();
    }
    
    public boolean isGreedy() {
      return true;
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
    ClientGreediness recall(ClientLock clientLock, ServerLockLevel interest, int lease) {
      return RECALLED_READ;
    }

    @Override
    ClientGreediness requestLevel(ServerLockLevel level) {
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
    boolean flushOnUnlock() {
      return false;
    }
  },

  GREEDY_WRITE {

    @Override
    public boolean canAward(LockLevel level) {
      return true;
    }
    
    public boolean isGreedy() {
      return true;
    }
    
    @Override
    boolean flushOnUnlock() {
      return false;
    }
    
    @Override
    ClientGreediness recall(ClientLock clientLock, ServerLockLevel interest, int lease) {
      if ((lease > 0) && (clientLock.pendingCount() > 0)) {
        return GREEDY_WRITE;
      } else {
        return RECALLED_WRITE;
      }
    }
  },
  
  RECALLED_READ {

    @Override
    ClientGreediness requestLevel(ServerLockLevel level) {
      return this; //lock is being recalled - we'll get the per thread awards from the server later
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
    ClientGreediness requestLevel(ServerLockLevel level) {
      return this; //lock is being recalled - we'll get the per thread awards from the server later
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

  /**
   * @throws GarbageLockException thrown if in a garbage state 
   */
  ClientGreediness requestLevel(ServerLockLevel level) throws GarbageLockException {
    throw new AssertionError("request level while in unexpected state (" + this + ")");
  }

  ClientGreediness award(ServerLockLevel level) {
    throw new AssertionError("award while in unexpected state (" + this + ")");
  }

  ClientGreediness recall(ClientLock clientLock, ServerLockLevel interest, int lease) {
    return this;
  }

  ClientGreediness recallInProgress() {
    throw new AssertionError("recall in progress while in unexpected state (" + this + ")");
  }
}
