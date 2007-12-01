/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
import com.tc.management.ClientLockStatContext;
import com.tc.management.ClientLockStatManager;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.impl.TCStackTraceElement;
import com.tc.object.net.DSOClientMessageChannel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Methods in this class are not synchronized. They should be called from a proper synchronized
// context, which is from the ClientLockManager context.
public class ClientLockStatisticsManagerImpl extends LockStatisticsManager implements ClientLockStatManager {
  private final static NodeID     NULL_NODE_ID                = NodeIDImpl.NULL_ID;
  private final static Set        IGNORE_STACK_TRACES_PACKAGE = new HashSet();

  private final Map               stackTracesMap              = new HashMap();
  private final Map               statEnabledLocks            = new HashMap();
  private Sink                    sink;
  private DSOClientMessageChannel channel;

  static {
    IGNORE_STACK_TRACES_PACKAGE.add("com.tc.");
    IGNORE_STACK_TRACES_PACKAGE.add("com.tcclient.");
  }

  private static LockStatisticsResponseMessage createLockStatisticsResponseMessage(ClientMessageChannel channel,
                                                                                   LockID lockID,
                                                                                   Collection lockStatElements) {
    LockStatisticsResponseMessage message = (LockStatisticsResponseMessage) channel
        .createMessage(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE);
    message.initialize(lockID, lockStatElements);
    return message;
  }

  public void start(DSOClientMessageChannel channel, Sink sink) {
    this.channel = channel;
    this.sink = sink;
  }

  public void recordLockRequested(LockID lockID, ThreadID threadID) {
    if (!isStatEnabled()) { return; }
    boolean shouldSendClientStat = shouldSendClientStat(lockID);

    StackTraceElement[] stackTraceElements = getStackTraceElements(lockStatConfig.getTraceDepth());
    super.recordLockRequested(lockID, NULL_NODE_ID, threadID, stackTraceElements);

    sendIfNeeded(lockID, shouldSendClientStat);
  }

  public void recordLockAwarded(LockID lockID, ThreadID threadID) {
    if (!isStatEnabled()) { return; }
    boolean shouldSendClientStat = shouldSendClientStat(lockID);

    StackTraceElement[] stackTraceElements = getStackTraceElements(lockStatConfig.getTraceDepth());
    boolean isLockAwardRecorded = super.recordLockAwarded(lockID, NULL_NODE_ID, threadID, false, System.currentTimeMillis(), stackTraceElements);

    sendIfNeeded(lockID, shouldSendClientStat && isLockAwardRecorded);
  }
  
  public void recordLockReleased(LockID lockID, ThreadID threadID) {
    if (!isStatEnabled()) { return; }
    boolean shouldSendClientStat = shouldSendClientStat(lockID);

    StackTraceElement[] stackTraceElements = getStackTraceElements(lockStatConfig.getTraceDepth());
    boolean isLockReleaseRecorded = super.recordLockReleased(lockID, NULL_NODE_ID, threadID, stackTraceElements);

    sendIfNeeded(lockID, shouldSendClientStat && isLockReleaseRecorded);
  }
  
  public void recordLockHopped(LockID lockID, ThreadID threadID) {
    if (!isStatEnabled()) { return; }
    boolean shouldSendClientStat = shouldSendClientStat(lockID);

    StackTraceElement[] stackTraceElements = getStackTraceElements(lockStatConfig.getTraceDepth());
    super.recordLockHopRequested(lockID, stackTraceElements);

    sendIfNeeded(lockID, shouldSendClientStat);
  }

  public void recordStackTrace(LockID lockID) {
    if (true) { return; }

    ClientLockStatContext lockStatContext = (ClientLockStatContext) statEnabledLocks.get(lockID);
    if (lockStatContext.shouldRecordStackTrace()) {

      Set stackTraces = (Set) stackTracesMap.get(lockID);
      if (stackTraces == null) {
        stackTraces = new HashSet();
        stackTracesMap.put(lockID, stackTraces);
      }

      StackTraceElement[] stackTraceElements = getStackTraceElements(lockStatContext.getStackTraceDepth());
      TCStackTraceElement tcStackTraceElement = new TCStackTraceElement(stackTraceElements);

      boolean added = stackTraces.add(tcStackTraceElement);

      if (added) {
        List stackTracesList = new ArrayList();
        stackTracesList.add(tcStackTraceElement);
      }
    }
    lockStatContext.updateCollectTimer();
  }

  public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    disableLockStatistics();
    super.setLockStatisticsEnabled(true);
    super.setLockStatisticsConfig(traceDepth, gatherInterval);
  }

  // TODO: We do not need to disable on a per lock basis
  public void disableStat(LockID lockID) {
    disableLockStatistics();
  }

  public boolean isStatEnabled() {
    return super.isClientStatEnabled();
  }

  protected LockStatisticsInfo newLockStatisticsContext(LockID lockID) {
    return new ClientLockStatisticsInfoImpl(lockID, lockStatConfig.getGatherInterval());
  }

  protected void disableLockStatistics() {
    super.clear();
  }

  private Collection getLockStatElements(LockID lockID) {
    LockStatisticsInfo lsc = getLockStatInfo(lockID);
    if (lsc == null) { return Collections.EMPTY_LIST; }

    lsc.aggregateLockHoldersData();
    return lsc.children();
  }

  private boolean shouldSendClientStat(LockID lockID) {
    if (!lockStatisticsEnabled) { return false; }

    ClientLockStatisticsInfoImpl lsc = (ClientLockStatisticsInfoImpl) getOrCreateLockStatInfo(lockID);
    return (lsc != null && lsc.getRecordedFrequency() == 0);
  }

  private StackTraceElement[] getStackTraceElements(int stackTraceDepth) {
    StackTraceElement[] stackTraces = (new Exception()).getStackTrace();
    return filterStackTracesElement(stackTraces, stackTraceDepth);
  }

  private StackTraceElement[] filterStackTracesElement(StackTraceElement[] stackTraces, int stackTraceDepth) {
    stackTraces = fixTCInstrumentationStackTraces(stackTraces);

    List list = new ArrayList();
    int numOfStackTraceCollected = 0;
    for (int i = 0; i < stackTraces.length; i++) {
      if (shouldIgnoreClass(stackTraces[i].getClassName())) {
        continue;
      }
      list.add(stackTraces[i]);
      numOfStackTraceCollected++;
      if (numOfStackTraceCollected >= stackTraceDepth) {
        break;
      }
    }
    StackTraceElement[] rv = new StackTraceElement[list.size()];
    return (StackTraceElement[]) list.toArray(rv);
  }

  private StackTraceElement[] fixTCInstrumentationStackTraces(StackTraceElement[] stackTraces) {
    LinkedList list = new LinkedList();
    for (int i = 0; i < stackTraces.length; i++) {
      if (isTCInstrumentationStackTrace(stackTraces, i)) {
        setStackTraceLineNumber(stackTraces[i + 1], stackTraces[i].getLineNumber());
        list.addLast(stackTraces[i + 1]);
        i++;
      } else {
        list.addLast(stackTraces[i]);
      }
    }
    StackTraceElement[] rv = new StackTraceElement[list.size()];
    return (StackTraceElement[]) list.toArray(rv);
  }

  private boolean isTCInstrumentationStackTrace(StackTraceElement[] stackTraces, int index) {
    if (stackTraces[index].getMethodName().startsWith(ByteCodeUtil.TC_METHOD_PREFIX)) {
      if (!stackTraces[index + 1].getMethodName().startsWith(ByteCodeUtil.TC_METHOD_PREFIX)) {
        if (stackTraces[index].getMethodName().endsWith(stackTraces[index + 1].getMethodName())) { return true; }
      }
    }
    return false;
  }

  private void setStackTraceLineNumber(StackTraceElement se, int newLineNumber) {
    try {
      Field f = StackTraceElement.class.getDeclaredField("lineNumber");
      f.setAccessible(true);
      f.set(se, new Integer(newLineNumber));
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

  private boolean shouldIgnoreClass(String className) {
    for (Iterator i = IGNORE_STACK_TRACES_PACKAGE.iterator(); i.hasNext();) {
      String ignorePackage = (String) i.next();
      if (className.startsWith(ignorePackage)) { return true; }
    }
    return false;
  }

  private void sendIfNeeded(LockID lockID, boolean shouldSend) {
    if (shouldSend) {
      Collection lockStatElements = getLockStatElements(lockID);
      
//      System.err.println("==========================");
//      System.err.println(lockStatElements);
//      System.err.println("==========================");
      
      if (lockStatElements.size() > 0) {
        sink.add(createLockStatisticsResponseMessage(channel.channel(), lockID, lockStatElements));
      }
    }
  }
}
