/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import java.io.Serializable;

public class CacheData implements Serializable {
  private Object         value;
  private long           maxIdleMillis;
  private Timestamp      timestamp;               // timestamp contains the time when the CacheData will be expired

  private transient long createTime;
  private transient long lastAccessedTimeInMillis;
  private transient long startMillis;
  private boolean        invalidated = false;

  public CacheData(Object value, long maxIdleSeconds) {
    this.value = value;
    this.maxIdleMillis = maxIdleSeconds * 1000;
    this.createTime = System.currentTimeMillis();
    this.startMillis = System.currentTimeMillis();
    this.timestamp = new Timestamp(this.createTime + maxIdleMillis);
    this.lastAccessedTimeInMillis = 0;
  }

  public CacheData() {
    this.createTime = System.currentTimeMillis();
    this.startMillis = System.currentTimeMillis();
    this.lastAccessedTimeInMillis = System.currentTimeMillis();
  }

  Timestamp getTimestamp() {
    return timestamp;
  }

  synchronized boolean isValid() {
    if (invalidated) { return false; }
    return getIdleMillis() < getMaxInactiveMillis();
  }

  synchronized long getIdleMillis() {
    if (lastAccessedTimeInMillis == 0) return 0;
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

  public synchronized Object getValue() {
    return value;
  }

  synchronized void invalidate() {
    this.invalidated = true;
  }

  synchronized boolean isInvalidated() {
    return this.invalidated;
  }
  
  public int hashCode() {
    return this.value.hashCode();
  }
  
  public boolean equals(Object obj) {
    if (! (obj instanceof CacheData)) { return false; }
    CacheData cd = (CacheData)obj;
    return this.value.equals(cd.value) && (this.maxIdleMillis == cd.maxIdleMillis);
  }

}
