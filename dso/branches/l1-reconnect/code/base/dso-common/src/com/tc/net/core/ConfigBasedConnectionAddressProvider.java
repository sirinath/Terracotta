/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.config.schema.dynamic.ConfigItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigBasedConnectionAddressProvider implements ConnectionAddressProvider {

  private List             currentAddresses = new ArrayList();

  public ConfigBasedConnectionAddressProvider(ConfigItem source) {
    this.currentAddresses.addAll(Arrays.asList((ConnectionInfo[]) source.getObject()));
  }

  public synchronized String toString() {
    return "ConnectionAddressProvider(" + currentAddresses + ")";
  }

  public synchronized ConnectionAddressIterator getIterator() {
    final ConnectionInfo[] arr = (ConnectionInfo[]) currentAddresses
        .toArray(new ConnectionInfo[currentAddresses.size()]);
    return new ConnectionAddressIterator(arr);
  }
}
