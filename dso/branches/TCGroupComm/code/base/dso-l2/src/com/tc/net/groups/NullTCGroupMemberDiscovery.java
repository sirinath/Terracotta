/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.exception.ImplementMe;

public class NullTCGroupMemberDiscovery implements TCGroupMemberDiscovery {

  public Node getLocalNode() {
    throw new ImplementMe();
  }

  public void setLocalNode(Node local) {
    return;
  }

  public void setTCGroupManager(TCGroupManagerImpl manager) {
    return;
  }

  public void start() {
    return;
  }

  public void stop() {
    return;
  }

}
