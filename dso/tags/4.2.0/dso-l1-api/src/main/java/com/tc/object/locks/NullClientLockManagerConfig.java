/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;


public class NullClientLockManagerConfig implements ClientLockManagerConfig {

  private long timeoutInterval = ClientLockManagerConfig.DEFAULT_TIMEOUT_INTERVAL;
  
  public NullClientLockManagerConfig() {
    this.timeoutInterval = ClientLockManagerConfig.DEFAULT_TIMEOUT_INTERVAL; 
  }
  
  public NullClientLockManagerConfig(long timeoutInterval) {
    this.timeoutInterval = timeoutInterval;
  }
  
  @Override
  public long getTimeoutInterval() {
    return timeoutInterval;
  }
  
  public void setTimeoutInterval(long timeoutInterval) {
    this.timeoutInterval = timeoutInterval;
  }

  @Override
  public int getStripedCount() {
    return ClientLockManagerConfig.DEFAULT_STRIPED_COUNT;
  }

}
