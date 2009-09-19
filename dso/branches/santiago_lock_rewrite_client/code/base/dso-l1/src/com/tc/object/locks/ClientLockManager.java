/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.DumpHandler;
import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.session.SessionID;

import java.util.Collection;

public interface ClientLockManager extends TerracottaLocking, ClientHandshakeCallback, DumpHandler {
  public void notified(LockID lock, ThreadID thread);
  public void recall(LockID lock, ServerLockLevel level, int lease);
  public void award(NodeID from, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level);
  public void refuse(NodeID from, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level);
  public void info(ThreadID requestor, Collection<ClientServerExchangeLockContext> contexts);
  
  public Collection<ClientServerExchangeLockContext> getAllLockContexts();
    
  public void wait(LockID lock, WaitListener listener) throws InterruptedException;
  public void wait(LockID lock, WaitListener listener, long timeout) throws InterruptedException;
}
