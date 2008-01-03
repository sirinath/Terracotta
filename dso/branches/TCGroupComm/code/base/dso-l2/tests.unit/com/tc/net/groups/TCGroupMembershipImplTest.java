/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageFactory;
import com.tc.net.protocol.tcm.TCMessageFactoryImpl;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.session.NullSessionManager;
import com.tc.util.PortChooser;

import junit.framework.TestCase;

public class TCGroupMembershipImplTest extends TestCase {

  MessageMonitor           monitor        = new NullMessageMonitor();
  final NullSessionManager sessionManager = new NullSessionManager();
  final TCMessageFactory   msgFactory     = new TCMessageFactoryImpl(sessionManager, monitor);
  final TCMessageRouter    msgRouter      = new TCMessageRouterImpl();

  public void testBasicChannelOpenClose() throws Exception {
    PortChooser pc = new PortChooser();
    final int groupPort1 = pc.chooseRandomPort();
    final int groupPort2 = pc.chooseRandomPort();
    Node[] nodes = new Node[] { new Node("localhost", groupPort1), new Node("localhost", groupPort2) };
    final TCGroupMemberDiscovery discover1 = new TCGroupMemberDiscoveryStatic(nodes);
    final TCGroupMemberDiscovery discover2 = new TCGroupMemberDiscoveryStatic(nodes);

    TCGroupMembership group1 = new TCGroupMembershipImpl(new NullConnectionPolicy(), "localhost", groupPort1, 0,
                                                         new TCThreadGroup(new ThrowableHandler(TCLogging
                                                             .getLogger(TCGroupMembershipImplTest.class))));
    group1.setDiscover(discover1);
    group1.start();
    TCGroupMembership group2 = new TCGroupMembershipImpl(new NullConnectionPolicy(), "localhost", groupPort2, 0,
                                                         new TCThreadGroup(new ThrowableHandler(TCLogging
                                                             .getLogger(TCGroupMembershipImplTest.class))));
    group2.setDiscover(discover2);
    group2.start();

    // open test
    TCGroupMember member1 = group1.openChannel("localhost", groupPort2);

    // wait for channel established at group2
    for (int i = 0; i < 30; i++) {
      if (group2.size() > 0) break;
      Thread.sleep(100);
    }

    assertEquals(1, group2.size());
    TCGroupMember member2 = group2.getMembers().get(0);
    assertTrue("Expected  " + member1.getSrcNodeID() + " but got " + member2.getSrcNodeID(), member1.getSrcNodeID()
        .equals(member2.getSrcNodeID()));
    assertTrue("Expected  " + member1.getDstNodeID() + " but got " + member2.getDstNodeID(), member1.getDstNodeID()
        .equals(member2.getDstNodeID()));

    // close test
    member1.getChannel().close();
    for (int i = 0; i < 30; i++) {
      if (group2.size() == 0) break;
      Thread.sleep(100);
    }
    assertEquals(0, group2.size());

    group1.shutdown();
    group2.shutdown();
  }

  /*
   * Both open channel to each other, only one direction to keep
   */
  public void testResolveTwoWayConnection() throws Exception {
    PortChooser pc = new PortChooser();
    final int groupPort1 = pc.chooseRandomPort();
    final int groupPort2 = pc.chooseRandomPort();
    Node[] nodes = new Node[] { new Node("localhost", groupPort1), new Node("localhost", groupPort2) };
    final TCGroupMemberDiscovery discover1 = new TCGroupMemberDiscoveryStatic(nodes);
    final TCGroupMemberDiscovery discover2 = new TCGroupMemberDiscoveryStatic(nodes);

    TCGroupMembership group1 = new TCGroupMembershipImpl(new NullConnectionPolicy(), "localhost", groupPort1, 0,
                                                         new TCThreadGroup(new ThrowableHandler(TCLogging
                                                             .getLogger(TCGroupMembershipImplTest.class))));
    group1.setDiscover(discover1);
    group1.start();
    TCGroupMembership group2 = new TCGroupMembershipImpl(new NullConnectionPolicy(), "localhost", groupPort2, 0,
                                                         new TCThreadGroup(new ThrowableHandler(TCLogging
                                                             .getLogger(TCGroupMembershipImplTest.class))));
    group2.setDiscover(discover2);
    group2.start();

    TCGroupMember member1 = group1.openChannel("localhost", groupPort2);
    TCGroupMember member2 = group2.openChannel("localhost", groupPort1);

    // wait one channel to be closed.
    Thread.sleep(1000);

    assertEquals(1, group1.size());
    assertEquals(1, group2.size());
    TCGroupMember m1 = group1.getMembers().get(0);
    TCGroupMember m2 = group2.getMembers().get(0);
    assertTrue("Expected  " + m1.getSrcNodeID() + " but got " + m2.getSrcNodeID(), m1.getSrcNodeID()
        .equals(m2.getSrcNodeID()));
    assertTrue("Expected  " + m1.getDstNodeID() + " but got " + m2.getDstNodeID(), m1.getDstNodeID()
        .equals(m2.getDstNodeID()));

    group1.shutdown();
    group2.shutdown();
  }

}
