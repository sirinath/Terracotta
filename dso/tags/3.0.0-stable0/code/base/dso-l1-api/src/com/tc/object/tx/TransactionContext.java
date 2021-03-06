/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;

import java.util.List;

/**
 * Transaction context
 */
public interface TransactionContext {

  /**
   * Returns the transaction type that corresponds to the lock type that
   * initiated this transaction.
   * 
   * @return Type of transaction based on the lock
   * @see #getEffectiveType()
   */
  public TxnType getLockType();

  /**
   * Returns the effective transaction type.
   *
   * Note that this can be different from the type that was requested by the
   * lock when that lock is nested within another lock. For instance, if a
   * write lock is already active and a nested read lock is obtained, the 
   * operations are effectively guarded against writes and not only reads.
   * 
   * @return Type of transaction based on the context
   * @see #getLockType()
   */
  public TxnType getEffectiveType();
  
  /**
   * Evaluates the effective transaction type to see if it's read-only.
   * 
   * Read the docs of {@link #getEffectiveType()} for more details.
   * 
   * @return {@code true} when the effective transaction type is read-only, or
   * {@code false} when it's not
   * @see #getEffectiveType()
   */
  public boolean isEffectivelyReadOnly();

  /**
   * @return First lock identifier in the transaction
   */
  public LockID getLockID();

  /**
   * @return All lock identifiers that have been involved in the transaction
   */
  public List getAllLockIDs();

  /**
   * Remove a lock previously involved in the transaction
   * 
   * @param id Identifier
   */
  public void removeLock(LockID id);

}