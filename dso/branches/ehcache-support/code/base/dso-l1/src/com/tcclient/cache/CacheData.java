/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;


public class CacheData {
  private Object            value;
  private final long        createTime;
  private long              lastAccessedTimeInMillis;
  private long              maxIdleMillis;
  private final Timestamp   timestamp;
  private transient long    startMillis;
  private transient boolean invalidated = false;

  public CacheData(Object value, long maxIdleSeconds) {
    this.value = value;
    this.createTime = System.currentTimeMillis();
    this.lastAccessedTimeInMillis = 0;
    setMaxInactiveMillis(maxIdleSeconds * 1000);
    this.timestamp = new Timestamp(this.createTime + maxIdleMillis);
  }

  Timestamp getTimestamp() {
    return timestamp;
  }

  public synchronized boolean isValid() {
    if (invalidated) { return false; }
    final boolean isValid = getIdleMillis() < getMaxInactiveMillis();
    return isValid;
  }

  synchronized long getIdleMillis() {
    if (lastAccessedTimeInMillis == 0) return 0;
    if (startMillis > lastAccessedTimeInMillis) return startMillis - lastAccessedTimeInMillis;
    return Math.max(System.currentTimeMillis() - lastAccessedTimeInMillis, 0);
  }
  
  synchronized void start() {
    startMillis = System.currentTimeMillis();
  }
  
  synchronized void finish() {
    startMillis = 0;
    lastAccessedTimeInMillis = System.currentTimeMillis();
  }
  
  synchronized void accessed() {
    lastAccessedTimeInMillis = System.currentTimeMillis();
  }
  
  synchronized long getMaxInactiveMillis() {
    return maxIdleMillis;
  }
  
  synchronized Object getValue() {
    return value;
  }
  
  synchronized void invalidate() {
    this.invalidated = true;
  }

  private void setMaxInactiveMillis(long v) {
    maxIdleMillis = v;
  }
}
