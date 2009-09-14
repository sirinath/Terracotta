/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCSerializable;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.StringLockID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TimerSpec;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Message for obtaining/releasing locks, and for modifying them (ie. wait/notify)
 * 
 * @author steve
 */
public class LockRequestMessage extends DSOMessageBase {

  private final static byte LOCK_ID      = 1;
  private final static byte LOCK_LEVEL   = 2;
  private final static byte THREAD_ID    = 3;
  private final static byte REQUEST_TYPE = 4;
  private final static byte WAIT_MILLIS  = 5;
  private static final byte CONTEXT      = 6;

  // private static final byte WAIT_CONTEXT = 6;
  // private static final byte LOCK_CONTEXT = 7;
  // private static final byte PENDING_LOCK_CONTEXT = 8;
  // private static final byte PENDING_TRY_LOCK_CONTEXT = 9;

  // request types
  public static enum RequestType {
    LOCK, UNLOCK, WAIT, RECALL_COMMIT, QUERY, TRY_LOCK, INTERRUPT_WAIT;
  }

  private final Set<ClientServerExchangeLockContext> contexts    = new LinkedHashSet<ClientServerExchangeLockContext>();

  // private final Set lockContexts = new LinkedHashSet();
  // private final Set waitContexts = new LinkedHashSet();
  // private final Set pendingLockContexts = new LinkedHashSet();
  // private final List pendingTryLockContexts = new ArrayList();

  private LockID                                     lockID      = StringLockID.NULL_ID;
  private ServerLockLevel                            lockLevel   = null;
  private ThreadID                                   threadID    = ThreadID.NULL_ID;
  private RequestType                                requestType = null;
  private long                                       waitMillis  = -1;

  public LockRequestMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                            MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public LockRequestMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                            TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(REQUEST_TYPE, (byte) requestType.ordinal());
    switch (requestType) {
      case LOCK:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        break;
      case UNLOCK:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        // putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        break;
      case TRY_LOCK:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        putNVPair(WAIT_MILLIS, waitMillis);
        break;
      case WAIT:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        // putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        putNVPair(WAIT_MILLIS, waitMillis);
        break;
      case INTERRUPT_WAIT:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        break;
      case QUERY:
        putNVPair(LOCK_ID, lockID);
        break;
      case RECALL_COMMIT:
        putNVPair(LOCK_ID, lockID);
        for (Iterator i = contexts.iterator(); i.hasNext();) {
          putNVPair(CONTEXT, (TCSerializable) i.next());
        }
        break;
    }
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();

    rv.append("Request Type: ").append(requestType).append('\n');
    rv.append(lockID).append(' ').append(threadID).append(' ').append("Lock Type: ").append(lockLevel).append('\n');

    if (waitMillis >= 0) {
      rv.append("Timeout : ").append(waitMillis).append("ms\n");
    }
    if (contexts.size() > 0) {
      rv.append("Holder/Waiters/Pending contexts size = ").append(contexts.size()).append('\n');
    }

    return rv.toString();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case LOCK_ID:
        // TODO: Make this use a lockID factory so that we can avoid dups
        lockID = getLockIDValue();
        return true;
      case LOCK_LEVEL:
        try {
          lockLevel = ServerLockLevel.values()[getByteValue()];
        } catch (ArrayIndexOutOfBoundsException e) {
          return false;
        }
        return true;
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case REQUEST_TYPE:
        try {
          requestType = RequestType.values()[getByteValue()];
        } catch (ArrayIndexOutOfBoundsException e) {
          return false;
        }
        return true;
      case WAIT_MILLIS:
        waitMillis = getLongValue();
        return true;
      case CONTEXT:
        contexts.add((ClientServerExchangeLockContext) getObject(new ClientServerExchangeLockContext()));
        return true;
      default:
        return false;
    }
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public LockID getLockID() {
    return lockID;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public ServerLockLevel getLockLevel() {
    return lockLevel;
  }

  public void addContext(ClientServerExchangeLockContext ctxt) {
    synchronized (contexts) {
      Assert.assertTrue(contexts.add(ctxt));
    }
  }

  public Collection<ClientServerExchangeLockContext> getContexts() {
    synchronized (contexts) {
      return contexts;
    }
  }

  public long getTimeout() {
    return waitMillis;
  }

  public void initializeInterruptWait(LockID lid, ThreadID id) {
    initialize(lid, id, null, RequestType.INTERRUPT_WAIT, -1);
  }

  public void initializeQuery(LockID lid, ThreadID id) {
    initialize(lid, id, null, RequestType.QUERY, -1);
  }

  public void initializeLock(LockID lid, ThreadID id, ServerLockLevel level, String lockObjectTypeArg) {
    initialize(lid, id, level, RequestType.LOCK, -1);
  }

  public void initializeTryLock(LockID lid, ThreadID id, TimerSpec timeout, ServerLockLevel level,
                                String lockObjectTypeArg) {
    initialize(lid, id, level, RequestType.TRY_LOCK, timeout.getMillis());
  }

  public void initializeUnlock(LockID lid, ThreadID id) {
    initialize(lid, id, null, RequestType.UNLOCK, -1);
  }

  public void initializeWait(LockID lid, ThreadID id, TimerSpec call) {
    initialize(lid, id, null, RequestType.WAIT, call.getMillis());
  }

  public void initializeRecallCommit(LockID lid) {
    initialize(lid, ThreadID.VM_ID, null, RequestType.RECALL_COMMIT, -1);
  }

  private void initialize(LockID lid, ThreadID id, ServerLockLevel level, RequestType reqType, long millis) {
    this.lockID = lid;
    this.lockLevel = level;
    this.threadID = id;
    this.requestType = reqType;
    this.waitMillis = millis;
  }

}
