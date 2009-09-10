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
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.locks.LockLevel;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TimerSpec;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Message for obtaining/releasing locks, and for modifying them (ie. wait/notify)
 *
 * @author steve
 */
public class LockRequestMessage extends DSOMessageBase {

  private final static byte                  LOCK_ID                         = 1;
  private final static byte                  LOCK_LEVEL                      = 2;
  private final static byte                  THREAD_ID                       = 3;
  private final static byte                  REQUEST_TYPE                    = 4;
  private final static byte                  WAIT_MILLIS                     = 5;
  private static final byte                  WAIT_CONTEXT                    = 6;
  private static final byte                  LOCK_CONTEXT                    = 7;
  private static final byte                  PENDING_LOCK_CONTEXT            = 8;
  private static final byte                  PENDING_TRY_LOCK_CONTEXT        = 9;

  // request types
  public static enum RequestType {LOCK, UNLOCK, WAIT, RECALL_COMMIT, QUERY, TRY_LOCK, INTERRUPT_WAIT;}

  private final Set                          lockContexts                    = new LinkedHashSet();
  private final Set                          waitContexts                    = new LinkedHashSet();
  private final Set                          pendingLockContexts             = new LinkedHashSet();
  private final List                         pendingTryLockContexts          = new ArrayList();

  private LockID                             lockID                          = LockID.NULL_ID;
  private LockLevel                          lockLevel                       = null;
  private ThreadID                           threadID                        = ThreadID.NULL_ID;
  private RequestType                        requestType                     = null;
  private long                               waitMillis                      = -1;

  public LockRequestMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
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
        putNVPair(LOCK_ID, lockID.asString());
        putNVPair(THREAD_ID, threadID.toLong());
        putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        break;
      case UNLOCK:
        putNVPair(LOCK_ID, lockID.asString());
        putNVPair(THREAD_ID, threadID.toLong());
        //putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        break;
      case TRY_LOCK:
        putNVPair(LOCK_ID, lockID.asString());
        putNVPair(THREAD_ID, threadID.toLong());
        putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        putNVPair(WAIT_MILLIS, waitMillis);
        break;
      case WAIT:
        putNVPair(LOCK_ID, lockID.asString());
        putNVPair(THREAD_ID, threadID.toLong());
        //putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        putNVPair(WAIT_MILLIS, waitMillis);
        break;
      case INTERRUPT_WAIT:
        putNVPair(LOCK_ID, lockID.asString());
        putNVPair(THREAD_ID, threadID.toLong());
        break;
      case QUERY:
        putNVPair(LOCK_ID, lockID.asString());
        break;
      case RECALL_COMMIT:
        putNVPair(LOCK_ID, lockID.asString());
        for (Iterator i = lockContexts.iterator(); i.hasNext();) {
          putNVPair(LOCK_CONTEXT, (TCSerializable) i.next());
        }

        for (Iterator i = waitContexts.iterator(); i.hasNext();) {
          putNVPair(WAIT_CONTEXT, (TCSerializable) i.next());
        }

        for (Iterator i = pendingLockContexts.iterator(); i.hasNext();) {
          putNVPair(PENDING_LOCK_CONTEXT, (TCSerializable) i.next());
        }

        for (Iterator i = pendingTryLockContexts.iterator(); i.hasNext();) {
          putNVPair(PENDING_TRY_LOCK_CONTEXT, (TCSerializable) i.next());
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
    if (waitContexts.size() > 0) {
      rv.append("Wait contexts size = ").append(waitContexts.size()).append('\n');
    }
    if (lockContexts.size() > 0) {
      rv.append("Lock contexts size = ").append(lockContexts.size()).append('\n');
    }
    if (pendingLockContexts.size() > 0) {
      rv.append("Pending Lock contexts size = ").append(pendingLockContexts.size()).append('\n');
    }

    return rv.toString();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case LOCK_ID:
        // TODO: Make this use a lockID factory so that we can avoid dups
        lockID = new LockID(getStringValue());
        return true;
      case LOCK_LEVEL:
        try {
          lockLevel = LockLevel.values()[getByteValue()];
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
      case LOCK_CONTEXT:
        lockContexts.add(getObject(new LockContext()));
        return true;
      case WAIT_CONTEXT:
        waitContexts.add(getObject(new WaitContext()));
        return true;
      case PENDING_LOCK_CONTEXT:
        pendingLockContexts.add(getObject(new LockContext()));
        return true;
      case PENDING_TRY_LOCK_CONTEXT:
        pendingTryLockContexts.add(getObject(new TryLockContext()));
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

  public LockLevel getLockLevel() {
    return lockLevel;
  }

  public void addLockContext(LockContext ctxt) {
    synchronized (lockContexts) {
      Assert.assertTrue(lockContexts.add(ctxt));
    }
  }

  public Collection getLockContexts() {
    synchronized (lockContexts) {
      return new LinkedHashSet(lockContexts);
    }
  }

  public void addWaitContext(WaitContext ctxt) {
    synchronized (waitContexts) {
      Assert.assertTrue(waitContexts.add(ctxt));
    }
  }

  public Collection getWaitContexts() {
    synchronized (waitContexts) {
      return new LinkedHashSet(waitContexts);
    }
  }

  public void addPendingLockContext(LockContext ctxt) {
    synchronized (pendingLockContexts) {
      Assert.assertTrue(pendingLockContexts.add(ctxt));
    }
  }

  public Collection getPendingLockContexts() {
    synchronized (pendingLockContexts) {
      return new LinkedHashSet(pendingLockContexts);
    }
  }

  public void addPendingTryLockContext(LockContext ctxt) {
    Assert.eval(ctxt instanceof TryLockContext);
    synchronized (pendingTryLockContexts) {
      pendingTryLockContexts.add(ctxt);
    }
  }

  public Collection getPendingTryLockContexts() {
    synchronized (pendingTryLockContexts) {
      return new ArrayList(pendingTryLockContexts);
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

  public void initializeLock(LockID lid, ThreadID id, LockLevel level, String lockObjectTypeArg) {
    initialize(lid, id, level, RequestType.LOCK, -1);
  }

  public void initializeTryLock(LockID lid, ThreadID id, TimerSpec timeout, LockLevel level, String lockObjectTypeArg) {
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

  private void initialize(LockID lid, ThreadID id, LockLevel level, RequestType reqType, long millis) {
    this.lockID = lid;
    this.lockLevel = level;
    this.threadID = id;
    this.requestType = reqType;
    this.waitMillis = millis;
  }

}
