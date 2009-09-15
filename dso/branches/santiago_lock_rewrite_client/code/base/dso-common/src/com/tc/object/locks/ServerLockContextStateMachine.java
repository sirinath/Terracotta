/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.ServerLockContext.State;

public interface ServerLockContextStateMachine {
  public boolean canSetState(State oldState, State newState);
}
