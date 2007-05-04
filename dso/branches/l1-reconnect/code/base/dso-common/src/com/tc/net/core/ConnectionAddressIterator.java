/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

public class ConnectionAddressIterator {

  private final int              policy;
  private final ConnectionInfo[] cis;
  private int                    current = -1;

  public ConnectionAddressIterator(int policy, ConnectionInfo[] cis) {
    this.policy = policy;
    this.cis = cis;
  }

  public boolean hasNext() {
    return (policy == ConnectionAddressProvider.ROUND_ROBIN) ? cis.length != 0 : (current < (cis.length - 1));
  }

  public ConnectionInfo next() {
    if (!hasNext()) return null;
    current = (current + 1) % cis.length;
    return cis[current];
  }
}
