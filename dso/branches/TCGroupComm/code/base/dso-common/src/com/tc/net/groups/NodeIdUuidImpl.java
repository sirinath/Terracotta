/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.util.UUID;

public class NodeIdUuidImpl extends NodeIDImpl {

  public static final NodeID  NULL_ID       = new NodeIdUuidImpl("NULL-ID", new byte[0]);

  public NodeIdUuidImpl() {
    super();
  }

  public NodeIdUuidImpl(String name, byte[] uid) {
    super(name, uid);
  }
  
  /*
   * NodeID with a uuid generated
   */
  public NodeIdUuidImpl(String name) {
    super(name, UUID.getUUID().toString().getBytes());
  }
}
