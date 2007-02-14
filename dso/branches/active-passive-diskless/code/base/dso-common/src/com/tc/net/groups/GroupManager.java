/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

public interface GroupManager {

  public NodeID join() throws GroupException;

  public void sendAll(GroupMessage msg) throws GroupException;

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException;

  public void registerForMessages(Class msgClass, GroupMessageListener listener);

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException;

}
