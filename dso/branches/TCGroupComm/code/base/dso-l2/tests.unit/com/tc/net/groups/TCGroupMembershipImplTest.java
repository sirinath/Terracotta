/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.msg.ObjectSyncMessage;
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
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.session.NullSessionManager;
import com.tc.util.ObjectIDSet2;
import com.tc.util.PortChooser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class TCGroupMembershipImplTest extends TestCase {

  MessageMonitor                   monitor        = new NullMessageMonitor();
  final NullSessionManager         sessionManager = new NullSessionManager();
  final TCMessageFactory           msgFactory     = new TCMessageFactoryImpl(sessionManager, monitor);
  final TCMessageRouter            msgRouter      = new TCMessageRouterImpl();

  private int                      groupPort1, groupPort2;
  private TCGroupMemberDiscovery   discover1, discover2;
  private TCGroupMembership        group1, group2;

  private int                      groupPorts[];
  private TCGroupMemberDiscovery   discovers[];
  private TCGroupMembership        groups[];
  private TestGroupMessageListener listeners[];
  private Node                     nodes[];

  protected void setUp2Groups() throws Exception {
    PortChooser pc = new PortChooser();
    groupPort1 = pc.chooseRandomPort();
    groupPort2 = pc.chooseRandomPort();
    nodes = new Node[] { new Node("localhost", groupPort1), new Node("localhost", groupPort2) };
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

  protected void tearDown2Groups() throws Exception {
    group1.shutdown();
    group2.shutdown();
  }

  private void setupGroups(int n) throws Exception {
    groupPorts = new int[n];
    discovers = new TCGroupMemberDiscoveryStatic[n];
    groups = new TCGroupMembershipImpl[n];
    listeners = new TestGroupMessageListener[n];
    nodes = new Node[n];

    PortChooser pc = new PortChooser();
    for (int i = 0; i < n; ++i) {
      groupPorts[i] = pc.chooseRandomPort();
      nodes[i] = new Node("localhost", groupPorts[i]);
    }
    for (int i = 0; i < n; ++i) {
      discovers[i] = new TCGroupMemberDiscoveryStatic(nodes);
      groups[i] = new TCGroupMembershipImpl(new NullConnectionPolicy(), "localhost", groupPorts[i], 0,
                                            new TCThreadGroup(new ThrowableHandler(TCLogging
                                                .getLogger(TCGroupMembershipImplTest.class))));
      groups[i].setDiscover(discovers[i]);
      groups[i].start(new HashSet());
      listeners[i] = new TestGroupMessageListener(100);
    }
  }

  private void tearGroups() throws Exception {
    for (int i = 0; i < groups.length; ++i) {
      groups[i].shutdown();
    }
  }

  public void testBasicChannelOpenClose() throws Exception {
    setUp2Groups();

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

    tearDown2Groups();
  }

  /*
   * Both open channel to each other, only one direction to keep
   */
  public void testResolveTwoWayConnection() throws Exception {
    setUp2Groups();

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

    tearDown2Groups();
  }

  public void testSendTo() throws Exception {
    setUp2Groups();

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

    tearDown2Groups();
  }

  private ObjectSyncMessage createTestObjectSyncMessage() {
    Set dnaOids = new ObjectIDSet2();
    for (long i = 1; i <= 100; ++i) {
      dnaOids.add(new ObjectID(i));
    }
    int count = 10;
    TCByteBuffer[] serializedDNAs = new TCByteBuffer[] {};
    ObjectStringSerializer objectSerializer = new ObjectStringSerializer();
    Map roots = new HashMap();
    long sID = 10;
    ObjectSyncMessage message = new ObjectSyncMessage(ObjectSyncMessage.MANAGED_OBJECT_SYNC_TYPE);
    message.initialize(dnaOids, count, serializedDNAs, objectSerializer, roots, sID);
    return (message);
  }

  private boolean cmpObjectSyncMessage(ObjectSyncMessage o1, ObjectSyncMessage o2) {
    return ((o1.getDnaCount() == o2.getDnaCount()) && o1.getOids().equals(o2.getOids())
            && o1.getRootsMap().equals(o2.getRootsMap()) && (o1.getType() == o2.getType()) && o1.getMessageID()
        .equals(o2.getMessageID()));
  }

  public void testJoin() throws Exception {
    int nGrp = 2;
    setupGroups(nGrp);

    groups[0].registerForMessages(ObjectSyncMessage.class, listeners[0]);
    groups[1].registerForMessages(ObjectSyncMessage.class, listeners[1]);

    groups[0].join(nodes[0], nodes);
    groups[1].join(nodes[1], nodes);
    Thread.sleep(200);
    assertEquals(1, groups[0].size());
    assertEquals(1, groups[1].size());

    GroupMessage sMesg = createTestObjectSyncMessage();
    TCGroupMember member = groups[0].getMembers().get(0);
    groups[0].sendTo(member.getNodeID(), sMesg);
    GroupMessage rMesg = listeners[1].getNextMessageFrom(groups[0].getNodeID());
    assertTrue(cmpObjectSyncMessage((ObjectSyncMessage) sMesg, (ObjectSyncMessage) rMesg));

    sMesg = createTestObjectSyncMessage();
    member = groups[1].getMembers().get(0);
    groups[1].sendTo(member.getNodeID(), sMesg);
    rMesg = listeners[0].getNextMessageFrom(groups[1].getNodeID());
    assertTrue(cmpObjectSyncMessage((ObjectSyncMessage) sMesg, (ObjectSyncMessage) rMesg));

    tearGroups();
  }

  private GCResultMessage createGCResultMessage() {
    ObjectIDSet2 oidSet = new ObjectIDSet2();
    for (long i = 1; i <= 100; ++i) {
      oidSet.add(new ObjectID(i));
    }
    GCResultMessage message = new GCResultMessage(GCResultMessage.GC_RESULT, oidSet);
    return (message);
  }
  
  private boolean cmpGCResultMessage(GCResultMessage o1, GCResultMessage o2) {
    return ((o1.getType() == o2.getType() &&
        o1.getMessageID().equals(o2.getMessageID()) &&
        o1.getGCedObjectIDs().equals(o2.getGCedObjectIDs())));
  }

  public void testSendToAll() throws Exception {
    int nGrp = 5;
    setupGroups(nGrp);
    HashMap<NodeID, TestGroupMessageListener> listenerMap = new HashMap<NodeID, TestGroupMessageListener>();

    for (int i = 0; i < nGrp; ++i) {
      groups[i].registerForMessages(GCResultMessage.class, listeners[i]);
      listenerMap.put(groups[i].getNodeID(), listeners[i]);
    }
    for (int i = 0; i < nGrp; ++i) {
      groups[i].join(nodes[i], nodes);
    }
    Thread.sleep(500);
    for (int i = 0; i < nGrp; ++i) {
      assertEquals(4, groups[i].size());
    }

    // test with one to one first
    GroupMessage sMesg = createGCResultMessage();
    TCGroupMember member = groups[0].getMembers().get(0);
    groups[0].sendTo(member.getNodeID(), sMesg);
    TestGroupMessageListener listener = listenerMap.get(member.getNodeID());
    GroupMessage rMesg = listener.getNextMessageFrom(groups[0].getNodeID());
    assertTrue(cmpGCResultMessage((GCResultMessage) sMesg, (GCResultMessage) rMesg));

    sMesg = createGCResultMessage();
    member = groups[1].getMembers().get(0);
    groups[1].sendTo(member.getNodeID(), sMesg);
    listener = listenerMap.get(member.getNodeID());
    rMesg = listener.getNextMessageFrom(groups[1].getNodeID());
    assertTrue(cmpGCResultMessage((GCResultMessage) sMesg, (GCResultMessage) rMesg));

    // test with broadcast
    sMesg = createGCResultMessage();
    groups[0].sendAll(sMesg);
    for(int i = 0; i < groups[0].size(); ++i) {
      TCGroupMember m = groups[0].getMembers().get(i);
      TestGroupMessageListener l = listenerMap.get(m.getNodeID());
      rMesg = l.getNextMessageFrom(groups[0].getNodeID());
      assertTrue(cmpGCResultMessage((GCResultMessage) sMesg, (GCResultMessage) rMesg));
    }

    Thread.sleep(200);
    tearGroups();
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
      System.out.println("XXX expected:" + nodeID + " got:" + pkg.getNodeID());
      assertTrue(nodeID.equals(pkg.getNodeID()));
      return (pkg.getMessage());
    }

  }

}
