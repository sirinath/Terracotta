/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class ThreadIDMapInfoJDK15 {
  private static final Map threadIDMap = new HashMap();

  public synchronized static void addThread(Long tcThreadID) {
    Assert.assertFalse(threadIDMap.containsKey(Thread.currentThread().getId()));
    threadIDMap.put(new Long(Thread.currentThread().getId()), new Long(tcThreadID));
  }

  public synchronized static Long getThread(Long vmThreadID) {
    return (Long) threadIDMap.get(vmThreadID);
  }
}
