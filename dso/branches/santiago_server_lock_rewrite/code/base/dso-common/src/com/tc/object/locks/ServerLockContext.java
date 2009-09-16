/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.SinglyLinkedList.LinkedNode;

public abstract class ServerLockContext implements LinkedNode<ServerLockContext> {
  private State             state;
  private final ClientID    clientID;
  private final ThreadID    threadID;

  private ServerLockContext next;

  public ServerLockContext(ClientID clientID, ThreadID threadID) {
    this.clientID = clientID;
    this.threadID = threadID;
  }

  public enum Type {
    GREEDY_HOLDER, HOLDER, TRY_PENDING, WAITER, PENDING;
  }

  public enum State {
    GREEDY_HOLDER_READ(ServerLockLevel.READ, Type.GREEDY_HOLDER), GREEDY_HOLDER_WRITE(ServerLockLevel.WRITE,
        Type.GREEDY_HOLDER), HOLDER_READ(ServerLockLevel.READ, Type.HOLDER), HOLDER_WRITE(ServerLockLevel.WRITE,
        Type.HOLDER), WAITER(ServerLockLevel.WRITE, Type.WAITER), TRY_PENDING_READ(ServerLockLevel.READ,
        Type.TRY_PENDING), TRY_PENDING_WRITE(ServerLockLevel.WRITE, Type.TRY_PENDING), PENDING_READ(
        ServerLockLevel.READ, Type.PENDING), PENDING_WRITE(ServerLockLevel.WRITE, Type.PENDING);

    private ServerLockLevel level;
    private Type            type;

    private State(ServerLockLevel level, Type type) {
      this.level = level;
      this.type = type;
    }

    public ServerLockLevel getLockLevel() {
      return level;
    }

    public Type getType() {
      return type;
    }

    public String toString() {
      return type + ":" + level;
    }
  }

  public final void setState(ServerLockContextStateMachine machine, State newState) {
    if (!machine.canSetState(this.state, newState)) { throw new IllegalStateException("Old=" + this.state + " to "
                                                                                      + newState); }
    this.state = newState;
  }

  public final ThreadID getThreadID() {
    return this.threadID;
  }

  public final ClientID getClientID() {
    return this.clientID;
  }

  public final State getState() {
    return this.state;
  }

  public ServerLockContext getNext() {
    return next;
  }

  public ServerLockContext setNext(ServerLockContext next) {
    ServerLockContext prev = this.next;
    this.next = next;
    return prev;
  }
}
