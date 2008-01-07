/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
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

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class TCGroupMembershipImplTest extends TestCase {

  MessageMonitor           monitor        = new NullMessageMonitor();
  final NullSessionManager sessionManager = new NullSessionManager();
  final TCMessageFactory   msgFactory     = new TCMessageFactoryImpl(sessionManager, monitor);
  final TCMessageRouter    msgRouter      = new TCMessageRouterImpl();

  private int              groupPort1, groupPort2;
  private TCGroupMemberDiscovery discover1, discover2;
  private TCGroupMembership      group1, group2;

  protected void setUp() throws Exception {
    super.setUp();
    PortChooser pc = new PortChooser();
    groupPort1 = pc.chooseRandomPort();
    groupPort2 = pc.chooseRandomPort();
    Node[] nodes = new Node[] { new Node("localhost", groupPort1), new Node("localhost", groupPort2) };
    discover1 = new TCGroupMemberDiscoveryStatic(nodes);
    discover2 = new TCGroupMemberDiscoveryStatic(nodes);

    group1 = new TCGroupMembershipImpl(new NullConnectionPolicy(), "localhost", groupPort1, 0,
                                       new TCThreadGroup(new ThrowableHandler(TCLogging
                                           .getLogger(TCGroupMembershipImplTest.class))));
    group1.setDiscover(discover1);
    group1.start(new HashSet());
    group2 = new TCGroupMembershipImpl(new NullConnectionPolicy(), "localhost", groupPort2, 0,
                                       new TCThreadGroup(new ThrowableHandler(TCLogging
                                           .getLogger(TCGroupMembershipImplTest.class))));
    group2.setDiscover(discover2);
    group2.start(new HashSet());

  }

  protected void tearDown() throws Exception {
    super.tearDown();

    group1.shutdown();
    group2.shutdown();
  }

  public void testBasicChannelOpenClose() throws Exception {

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

  }

  /*
   * Both open channel to each other, only one direction to keep
   */
  public void testResolveTwoWayConnection() throws Exception {

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

  }

  public void testSendTo() throws Exception {
    TestGroupMessageListener listener1 = new TestGroupMessageListener(100);
    TestGroupMessageListener listener2 = new TestGroupMessageListener(100);
    group1.registerForMessages(GroupZapNodeMessage.class, listener1);
    group2.registerForMessages(GroupZapNodeMessage.class, listener2);

    TCGroupMember member1 = group1.openChannel("localhost", groupPort2);
    Thread.sleep(200);
    TCGroupMember member2 = group2.getMembers().get(0);

    long weights[] = new long[] { 1, 23, 44, 78 };
    GroupMessage sMesg = new GroupZapNodeMessage(GroupZapNodeMessage.ZAP_NODE_REQUEST,
                                                 L2HAZapNodeRequestProcessor.SPLIT_BRAIN, "Zapping node", weights);

    group1.sendTo(member1.getNodeID(), sMesg);
    GroupMessage rMesg = listener2.getNextMessageFrom(group1.getNodeID());
    assertTrue(sMesg.toString().equals(rMesg.toString()));

    group2.sendTo(member2.getNodeID(), sMesg);
    rMesg = listener1.getNextMessageFrom(group2.getNodeID());
    assertTrue(sMesg.toString().equals(rMesg.toString()));

  }

  private class MessagePackage {
    private final GroupMessage message;
    private final NodeID       nodeID;

    MessagePackage(NodeID nodeID, GroupMessage message) {
      this.message = message;
      this.nodeID = nodeID;
    }

    GroupMessage getMessage() {
      return this.message;
    }

    NodeID getNodeID() {
      return this.nodeID;
    }
  }

  private class TestGroupMessageListener implements GroupMessageListener {
    private long                                timeout;
    private LinkedBlockingQueue<MessagePackage> queue = new LinkedBlockingQueue(10);

    TestGroupMessageListener(long timeout) {
      this.timeout = timeout;
    }

    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      queue.add(new MessagePackage(fromNode, msg));
    }

    public MessagePackage poll() throws InterruptedException {
      return (queue.poll(timeout, TimeUnit.MILLISECONDS));
    }

    public GroupMessage getNextMessageFrom(NodeID nodeID) throws InterruptedException {
      MessagePackage pkg = poll();
      System.out.println("XXX expected:"+nodeID+ " got:"+pkg.getNodeID());
      assertTrue(nodeID.equals(pkg.getNodeID()));
      return (pkg.getMessage());
    }

  }

}
