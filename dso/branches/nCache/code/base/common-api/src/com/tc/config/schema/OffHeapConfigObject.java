/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

public class OffHeapConfigObject {

  private final boolean enabled;
  private final String  maxDataSize;

  public OffHeapConfigObject(final boolean enabled, final String maxDataSize) {
    this.enabled = enabled;
    this.maxDataSize = maxDataSize;
    System.out.println(this);
  }

  public String getMaxDataSize() {
    return maxDataSize;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public String toString() {
    return ("XXX OffHpCfgObject : " + isEnabled() + "/" + getMaxDataSize());
  }
}
