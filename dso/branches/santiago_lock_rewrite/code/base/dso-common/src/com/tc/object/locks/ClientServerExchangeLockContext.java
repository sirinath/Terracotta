/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.object.tx.TimerSpec;

import java.io.IOException;

public class ClientServerExchangeLockContext implements TCSerializable {
  private LockID   lockID;
  private NodeID   nodeID;
  private ThreadID threadID;
  private State    state;
  private long     timeout;
  private int      hashCode;

  public ClientServerExchangeLockContext() {
    // to make TCSerializable happy
  }

  public ClientServerExchangeLockContext(LockID lockID, NodeID nodeID, ThreadID threadID, State state) {
    this(lockID, nodeID, threadID, state, -1);
  }

  public ClientServerExchangeLockContext(LockID lockID, NodeID nodeID, ThreadID threadID, State state, long timeout) {
    this.lockID = lockID;
    this.nodeID = nodeID;
    this.threadID = threadID;
    this.state = state;
    this.timeout = timeout;
    this.hashCode = calculateHash();
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public LockID getLockID() {
    return lockID;
  }

  public State getState() {
    return this.state;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof ClientServerExchangeLockContext)) return false;
    ClientServerExchangeLockContext cmp = (ClientServerExchangeLockContext) o;
    return lockID.equals(cmp.lockID) && threadID.equals(cmp.threadID) && state.equals(cmp.state)
           && nodeID.equals(cmp.nodeID);
  }

  public void serializeTo(TCByteBufferOutput output) {
    LockIDSerializer ls = new LockIDSerializer(lockID);
    ls.serializeTo(output);
    NodeIDSerializer ns = new NodeIDSerializer(this.nodeID);
    ns.serializeTo(output);
    output.writeLong(threadID.toLong());
    output.writeInt(state.ordinal());
    if (state.getType() == Type.WAITER || state.getType() == Type.TRY_PENDING) {
      output.writeLong(timeout);
    }
  }

  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    LockIDSerializer ls = new LockIDSerializer();
    ls.deserializeFrom(input);
    this.lockID = ls.getLockID();
    NodeIDSerializer ns = new NodeIDSerializer();
    ns.deserializeFrom(input);
    nodeID = ns.getNodeID();
    threadID = new ThreadID(input.readLong());
    state = State.values()[input.readInt()];
    if (state.getType() == Type.WAITER || state.getType() == Type.TRY_PENDING) {
      this.timeout = input.readLong();
    } else {
      timeout = -1;
    }
    return this;
  }

  private int calculateHash() {
    return new HashCodeBuilder(5503, 6737).append(lockID).append(nodeID).append(threadID).toHashCode();
  }
  
  public long timeout() {
    return this.timeout;
  }

  public int hasCode() {
    return this.hashCode;
  }

  @Deprecated
  public LockContext getLockContext() {
    return new LockContext(lockID, nodeID, threadID, ServerLockLevel.toLegacyInt(state.getLockLevel()), "");
  }

  @Deprecated
  public WaitContext getWaitContext() {
    TimerSpec spec = timeout == -1 ? new TimerSpec() : new TimerSpec(timeout);
    return new WaitContext(lockID, nodeID, threadID, ServerLockLevel.toLegacyInt(state.getLockLevel()), "", spec);
  }

  @Deprecated
  public TryLockContext getTryWaitContext() {
    TimerSpec spec = timeout == -1 ? new TimerSpec() : new TimerSpec(timeout);
    return new TryLockContext(lockID, nodeID, threadID, ServerLockLevel.toLegacyInt(state.getLockLevel()), "", spec);
  }
}
