/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.util.Assert;

import java.io.IOException;

/**
 * Client/Server intermediate fromat for holding the context of a lock request/award. This class bridges the types used
 * internally by the ClientLockManager and the server LockManager so they can be sent in messages back and forth to each
 * other.
 */
public class LockContext implements TCSerializable {

  private LockID    lockID;
  private int       lockLevel;
  private ChannelID channelID;
  private ThreadID  threadID;
  private int       hashCode;

  public LockContext() {
    return;
  }

  public LockContext(LockID lockID, ChannelID channelID, ThreadID threadID, int lockLevel) {
    this.lockID = lockID;
    this.channelID = channelID;
    this.threadID = threadID;
    Assert.assertFalse(LockLevel.isSynchronous(lockLevel));
    this.lockLevel = lockLevel;
    this.hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(channelID).append(threadID).append(lockLevel)
        .toHashCode();
  }

  public String toString() {
    return "LockContext [ " + lockID + ", " + LockLevel.toString(lockLevel) + ", " + channelID + ", " + threadID + ", "
           + hashCode + "] ";
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public LockID getLockID() {
    return lockID;
  }

  public int getLockLevel() {
    return this.lockLevel;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof LockContext)) return false;
    LockContext cmp = (LockContext) o;
    return lockID.equals(cmp.lockID) && threadID.equals(cmp.threadID) && lockLevel == cmp.lockLevel
           && channelID.equals(cmp.channelID);
  }

  public int hashCode() {
    return hashCode;
  }

  public void serializeTo(TCByteBufferOutput output) {
    output.writeString(lockID.asString());
    output.writeLong(channelID.toLong());
    output.writeLong(threadID.toLong());
    output.writeInt(lockLevel);
  }

  public Object deserializeFrom(TCByteBufferInputStream input) throws IOException {
    lockID = new LockID(input.readString());
    channelID = new ChannelID(input.readLong());
    threadID = new ThreadID(input.readLong());
    lockLevel = input.readInt();
    return this;
  }

}
