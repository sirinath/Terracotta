/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.exception.ImplementMe;

/*
 * This is a simple class that is a dummy group manager. All it does is it treats this one node that it runs in as a
 * group. This is needed for two reasons, 1) to easly disable, test the rest of the system and 2) to provide an
 * interface level replacement for tribes in 1.4 JVM
 */
public class SingleNodeGroupManager implements GroupManager {

  NodeID thisNode;

  public NodeID join() throws GroupException {
    if (thisNode != null) { throw new GroupException("Already Joined"); }
    this.thisNode = new NodeID("CurrentNode");
    return this.thisNode;
  }

  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    throw new ImplementMe();
    
  }

  public void sendAll(GroupMessage msg) throws GroupException {
    throw new ImplementMe();
    
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException {
    throw new ImplementMe();
  }

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException {
    throw new ImplementMe();
    
  }

}
