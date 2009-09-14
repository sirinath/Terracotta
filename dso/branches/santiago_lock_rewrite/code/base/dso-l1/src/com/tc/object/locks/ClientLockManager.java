/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.GroupID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.session.SessionID;

public interface ClientLockManager extends TerracottaLocking, ClientHandshakeCallback {
  public void notified(LockID lock, ThreadID thread);
  public void recall(LockID lock, LockLevel level, int lease);
  public void award(GroupID group, SessionID session, LockID lock, ThreadID thread, LockLevel level);
  public void refuse(GroupID group, SessionID session, LockID lock, ThreadID thread, LockLevel level);
}
