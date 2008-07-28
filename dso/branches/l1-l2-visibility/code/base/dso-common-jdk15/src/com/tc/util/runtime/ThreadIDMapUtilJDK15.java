/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.util.HashMap;
import java.util.Map;

public class ThreadIDMapUtilJDK15 implements ThreadIDMap {
  private final Map<Long, Long> threadIDMap = new HashMap<Long, Long>();

  public synchronized void addTCThreadID(long tcThreadID) {
    threadIDMap.put(Thread.currentThread().getId(), tcThreadID);
  }

  public synchronized Long getTCThreadID(long vmThreadID) {
    return threadIDMap.get(vmThreadID);
  }
}
