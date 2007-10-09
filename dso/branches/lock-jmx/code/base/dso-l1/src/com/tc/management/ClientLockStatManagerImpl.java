/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.msg.LockStatisticsResponseMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Methods in this class are not synchronized. They should be called from a proper synchronized
// context.
public class ClientLockStatManagerImpl implements ClientLockStatManager {
  private final static int        DEFAULT_BATCH_SIZE          = 100;
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
                                                                                   LockID lockID, List stackTraces) {
    LockStatisticsResponseMessage message = (LockStatisticsResponseMessage) channel
        .createMessage(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE);
    message.initialize(lockID, stackTraces);
    return message;
  }

  public void start(DSOClientMessageChannel channel, Sink sink) {
    this.channel = channel;
    this.sink = sink;
  }

  public void recordStackTrace(LockID lockID) {
    ClientLockStatContext lockStatContext = (ClientLockStatContext) statEnabledLocks.get(lockID);
    if (lockStatContext.getLockAccessedFrequency() == 0) {

//      List stackTraces = (List) stackTracesMap.get(lockID);
//      if (stackTraces == null) {
//        stackTraces = new LRUList(batch);
//        stackTracesMap.put(lockID, stackTraces);
//      }
//      stackTraces.add(getStackTraceElements(lockStatContext.getStackTraceDepth()));
//      send(lockID, new LinkedList(stackTraces));
//      stackTraces.clear();
      
      List stackTraces = new ArrayList();
      stackTraces.add(getStackTraceElements(lockStatContext.getStackTraceDepth()));
      send(lockID, stackTraces);
    }
    lockStatContext.lockAccessed();
  }

  private StackTraceElement[] getStackTraceElements(int stackTraceDepth) {
    StackTraceElement[] stackTraces = (new Exception()).getStackTrace();
    return filterStackTracesElement(stackTraces, stackTraceDepth);
  }

  private StackTraceElement[] filterStackTracesElement(StackTraceElement[] stackTraces, int stackTraceDepth) {
    LinkedList list = new LinkedList();
    int numOfStackTraceCollected = 0;
    for (int i = 0; i < stackTraces.length; i++) {
      if (shouldIgnoreClass(stackTraces[i].getClassName())) {
        continue;
      }
      list.addLast(stackTraces[i]);
      numOfStackTraceCollected++;
      if (numOfStackTraceCollected > stackTraceDepth) { break; }
    }
    StackTraceElement[] rv = new StackTraceElement[list.size()];
    return (StackTraceElement[]) list.toArray(rv);
  }

  private boolean shouldIgnoreClass(String className) {
    for (Iterator i = IGNORE_STACK_TRACES_PACKAGE.iterator(); i.hasNext();) {
      String ignorePackage = (String) i.next();
      if (className.startsWith(ignorePackage)) { return true; }
    }
    return false;
  }

  private void send(LockID lockID, List stackTraces) {
    sink.add(createLockStatisticsResponseMessage(channel.channel(), lockID, stackTraces));
  }

  public void enableStat(LockID lockID, int lockStackTraceDepth, int lockStatCollectFrequency) {
    ClientLockStatContext lockStatContext = (ClientLockStatContext) statEnabledLocks.get(lockID);
    if (lockStatContext == null) {
      lockStatContext = new ClientLockStatContext(lockStatCollectFrequency, lockStackTraceDepth);
      statEnabledLocks.put(lockID, lockStatContext);
    }
  }

  public void disableStat(LockID lockID) {
    statEnabledLocks.remove(lockID);
  }

  public boolean isStatEnabled(LockID lockID) {
    return statEnabledLocks.containsKey(lockID);
  }

  private static class ClientLockStatContext {
    private final static int DEFAULT_DEPTH             = 0;
    private final static int DEFAULT_COLLECT_FREQUENCY = 10;

    private int              collectFrequency;
    private int              stackTraceDepth           = 0;
    private int              lockAccessedFrequency     = 0;

    public ClientLockStatContext() {
      TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("l1.lock.stacktrace");
      if (tcProperties != null) {
        this.stackTraceDepth = tcProperties.getInt("defaultDepth", DEFAULT_DEPTH);
      }
      tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("l1.lock");
      if (tcProperties != null) {
        this.collectFrequency = tcProperties.getInt("collectFrequency", DEFAULT_COLLECT_FREQUENCY);
      }
    }

    public ClientLockStatContext(int collectFrequency, int stackTraceDepth) {
      this.collectFrequency = collectFrequency;
      this.stackTraceDepth = stackTraceDepth;
    }

    public int getCollectFrequency() {
      return collectFrequency;
    }

    public void setCollectFrequency(int collectFrequency) {
      this.collectFrequency = collectFrequency;
    }

    public int getStackTraceDepth() {
      return stackTraceDepth;
    }

    public void setStackTraceDepth(int stackTraceDepth) {
      this.stackTraceDepth = stackTraceDepth;
    }

    public int getLockAccessedFrequency() {
      return this.lockAccessedFrequency;
    }

    public void lockAccessed() {
      this.lockAccessedFrequency = (this.lockAccessedFrequency + 1) % collectFrequency;
    }
  }

  private static class LRUList extends LinkedList {
    private final static int NO_LIMIT = -1;

    private int              maxSize;

    public LRUList() {
      this(NO_LIMIT);
    }

    public LRUList(int maxSize) {
      this.maxSize = maxSize;
    }

    public boolean add(Object o) {
      super.addFirst(o);
      if (maxSize != NO_LIMIT && size() > maxSize) {
        removeLast();
      }
      return true;
    }
  }

}
