/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.group.GroupChannel;

import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.Serializable;

public class TribesGroupManager implements GroupManager, ChannelListener, MembershipListener {

  private static final TCLogger logger = TCLogging.getLogger(TribesGroupManager.class);

  private final GroupChannel    group;

  private Member                thisMember;
  private NodeID                thisNodeID;

  public TribesGroupManager() {
    group = new GroupChannel();
    group.addChannelListener(this);
    group.addMembershipListener(this);
  }

  public NodeID join() throws GroupException {
    try {
      group.start(Channel.DEFAULT);
      this.thisMember = group.getLocalMember(false);
      this.thisNodeID = new NodeID(this.thisMember.getName());
      return this.thisNodeID;
    } catch (ChannelException e) {
      logger.error(e);
      throw new GroupException(e);
    }
  }

  public boolean accept(Serializable msg, Member sender) {
    throw new ImplementMe();
  }

  public void messageReceived(Serializable msg, Member sender) {
    throw new ImplementMe();

  }

  public void memberAdded(Member member) {
    throw new ImplementMe();

  }

  public void memberDisappeared(Member member) {
    throw new ImplementMe();

  }

}
