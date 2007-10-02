/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Methods in this class are not synchronized. They should be called from a proper synchronized
// context.
public class ClientLockStatManagerImpl implements ClientLockStatManager {
  private final static int DEFAULT_BATCH_SIZE = 100;
  
  private final Map stackTracesMap = new HashMap();
  private final Set statEnabledLocks = new HashSet();
  private final int batch;
  private Sink sink;
  private DSOClientMessageChannel channel;
  
  private static LockStatisticsResponseMessage createLockStatisticsResponseMessage(ClientMessageChannel channel, LockID lockID, List stackTraces) {
    LockStatisticsResponseMessage message = (LockStatisticsResponseMessage) channel.createMessage(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE);
    message.initialize(lockID, stackTraces);
    return message;
  } 
  
  public ClientLockStatManagerImpl() {
    TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("l1.lock.stacktrace");
    this.batch = tcProperties.getInt("batch", DEFAULT_BATCH_SIZE);
  }
  
  public void start(DSOClientMessageChannel channel, Sink sink) {
    this.channel = channel;
    this.sink = sink;
  }
  
  public void recordStackTrace(LockID lockID, Throwable t) {  
    List stackTraces = (List)stackTracesMap.get(lockID);
    if (stackTraces == null) {
      stackTraces = new LRUList(batch);
      stackTracesMap.put(lockID, stackTraces);
    }
    stackTraces.add(t.getStackTrace());
    if (stackTraces.size() == batch) {
      send(lockID, new LinkedList(stackTraces));
      stackTraces.clear();
    }
  }
  
  private void send(LockID lockID, List stackTraces) {
    sink.add(createLockStatisticsResponseMessage(channel.channel(), lockID, stackTraces));
  }
  
  public void enableStat(LockID lockID) {
    statEnabledLocks.add(lockID);
  }
  
  public void disableStat(LockID lockID) {
    statEnabledLocks.remove(lockID);
  }
  
  public boolean isStatEnabled(LockID lockID) {
    return statEnabledLocks.contains(lockID);
  }
  
  private static class LRUList extends LinkedList {
    private final static int NO_LIMIT = -1;
    
    private int maxSize;
    
    public LRUList() {
      this(NO_LIMIT);
    }
    
    public LRUList(int maxSize) {
      this.maxSize = maxSize;
    }
    
    public boolean add(Object o) {
      super.add(o);
      if (maxSize != NO_LIMIT && size() > maxSize) {
        removeFirst();
      }
      return true;
    }
  }

}
