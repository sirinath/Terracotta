/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ConfigItemListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigBasedConnectionAddressProvider implements ConnectionAddressProvider, ConfigItemListener {

  private final ConfigItem source;
  private List             currentAddresses = new ArrayList();

  public ConfigBasedConnectionAddressProvider(ConfigItem source) {
    this.source = source;

    this.source.addListener(this);
    this.currentAddresses.addAll(Arrays.asList((ConnectionInfo[]) source.getObject()));
  }

  public synchronized void valueChanged(Object oldValue, Object newValue) {
    ConnectionInfo[] newAddresses = (ConnectionInfo[]) this.source.getObject();
    this.currentAddresses.clear();
    this.currentAddresses.addAll(Arrays.asList(newAddresses));
  }

  public String toString() {
    return "ConnectionAddressProvider(" + currentAddresses + ")";
  }

  public ConnectionAddressIterator getIterator(int policy) {
    if (!(policy == ConnectionAddressProvider.LINEAR || policy == ConnectionAddressProvider.ROUND_ROBIN)) throw new IllegalArgumentException(
                                                                                                                                             "Invalid policy: "
                                                                                                                                                 + policy);
    final ConnectionInfo[] arr = (ConnectionInfo[]) currentAddresses
        .toArray(new ConnectionInfo[currentAddresses.size()]);
    return new ConnectionAddressIterator(policy, arr);
  }
}
