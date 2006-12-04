/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import junit.framework.TestCase;

public class ConnectionPolicyTest extends TestCase {
  private ConnectionPolicy policy;
  
  public void tests() throws Exception {
    int maxConnections = 2;
    policy = new ConnectionPolicyImpl(maxConnections);

    policy.clientConnected();
    assertFalse(policy.maxConnectionsExceeded());
    
    policy.clientConnected();
    assertFalse(policy.maxConnectionsExceeded());
    
    policy.clientConnected();
    assertTrue(policy.maxConnectionsExceeded());
    
    policy.clientDisconnected();
    assertFalse(policy.maxConnectionsExceeded());
    
    policy.clientConnected();
    assertTrue(policy.maxConnectionsExceeded());
    
    assertEquals(maxConnections, policy.getMaxConnections());
    
    policy = new ConnectionPolicyImpl(-1);
    for (int i=0; i<100; i++) {
      policy.clientConnected();
      assertFalse(policy.maxConnectionsExceeded());
    }
  }
}
