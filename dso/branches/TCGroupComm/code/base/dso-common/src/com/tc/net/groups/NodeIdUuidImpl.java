/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.util.UUID;

public class NodeIdUuidImpl extends NodeIDImpl implements Comparable {

  public NodeIdUuidImpl() {
    super();
  }

  /*
   * NodeID with a uuid generated
   */
  public NodeIdUuidImpl(String name) {
    super(name, UUID.getUUID().toString().getBytes());
  }
  
  public int compareTo(Object arg0) {
    NodeIDImpl target = (NodeIDImpl)arg0;
    byte[] uid = getUID();
    byte[] targetUid = target.getUID();
    if (uid.length > targetUid.length) return(1);
    if (uid.length < targetUid.length) return(-1);
    for(int i = 0; i < uid.length; ++i) {
      if (uid[i] > targetUid[i]) return (1);
      if (uid[i] < targetUid[i]) return (-1);
    }
    return 0;
  }
  
  public byte getType() {
    return L2_NODE_TYPE;
  }

}
