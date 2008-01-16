/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.msg.L2StateMessageFactory;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.state.Enrollment;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageFactory;
import com.tc.net.protocol.tcm.TCMessageFactoryImpl;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.session.NullSessionManager;
import com.tc.util.ObjectIDSet2;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class TCGroupManagerImplTest extends TestCase {

  private final static String      LOCALHOST      = "localhost";
  MessageMonitor                   monitor        = new NullMessageMonitor();
  final NullSessionManager         sessionManager = new NullSessionManager();
  final TCMessageFactory           msgFactory     = new TCMessageFactoryImpl(sessionManager, monitor);
  final TCMessageRouter            msgRouter      = new TCMessageRouterImpl();

  private int                      groupPorts[];
  private TCGroupMemberDiscovery   discovers[];
  private TCGroupManagerImpl       groups[];
  private TestGroupMessageListener listeners[];
  private Node                     nodes[];

  private void setupGroups(int n) throws Exception {
    groupPorts = new int[n];
    discovers = new TCGroupMemberDiscoveryStatic[n];
    groups = new TCGroupManagerImpl[n];
    listeners = new TestGroupMessageListener[n];
    nodes = new Node[n];

    PortChooser pc = new PortChooser();
    for (int i = 0; i < n; ++i) {
      groupPorts[i] = pc.chooseRandomPort();
      nodes[i] = new Node(LOCALHOST, groupPorts[i]);
    }
    for (int i = 0; i < n; ++i) {
      discovers[i] = new TCGroupMemberDiscoveryStatic(nodes);
      groups[i] = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, groupPorts[i], 0,
                                         new TCThreadGroup(new ThrowableHandler(TCLogging
                                             .getLogger(TCGroupManagerImplTest.class))));
      groups[i].setDiscover(discovers[i]);
      groups[i].start(new HashSet());
      listeners[i] = new TestGroupMessageListener(1000);
    }
  }

  private void tearGroups() throws Exception {
    for (int i = 0; i < groups.length; ++i) {
      groups[i].shutdown();
    }
  }

  public void testBasicChannelOpenClose() throws Exception {
    setupGroups(2);

    // open test
    TCGroupMember member1 = groups[0].openChannel(LOCALHOST, groupPorts[1]);

    // wait for channel established at group2
    for (int i = 0; i < 30; i++) {
      if (groups[1].size() > 0) break;
      Thread.sleep(100);
    }

    assertEquals(1, groups[1].size());
    TCGroupMember member2 = groups[1].getMembers().get(0);
    assertTrue("Expected  " + member1.getSrcNodeID() + " but got " + member2.getSrcNodeID(), member1.getSrcNodeID()
        .equals(member2.getSrcNodeID()));
    assertTrue("Expected  " + member1.getDstNodeID() + " but got " + member2.getDstNodeID(), member1.getDstNodeID()
        .equals(member2.getDstNodeID()));

    // close test
    member1.getChannel().close();
    for (int i = 0; i < 30; i++) {
      if (groups[1].size() == 0) break;
      Thread.sleep(100);
    }
    assertEquals(0, groups[1].size());

    tearGroups();
  }

  /*
   * Both open channel to each other, only one direction to keep
   */
  public void testResolveTwoWayConnection() throws Exception {
    setupGroups(2);

    TCGroupMember member1 = groups[0].openChannel(LOCALHOST, groupPorts[1]);
    TCGroupMember member2 = groups[1].openChannel(LOCALHOST, groupPorts[0]);

    // wait one channel to be closed.
    Thread.sleep(1000);

    assertEquals(1, groups[0].size());
    assertEquals(1, groups[1].size());
    TCGroupMember m1 = groups[0].getMembers().get(0);
    TCGroupMember m2 = groups[1].getMembers().get(0);
    assertTrue("Expected  " + m1.getSrcNodeID() + " but got " + m2.getSrcNodeID(), m1.getSrcNodeID()
        .equals(m2.getSrcNodeID()));
    assertTrue("Expected  " + m1.getDstNodeID() + " but got " + m2.getDstNodeID(), m1.getDstNodeID()
        .equals(m2.getDstNodeID()));

    tearGroups();
  }

  public void testSendTo() throws Exception {
    setupGroups(2);

    TestGroupMessageListener listener1 = new TestGroupMessageListener(100);
    TestGroupMessageListener listener2 = new TestGroupMessageListener(100);
    groups[0].registerForMessages(GroupZapNodeMessage.class, listener1);
    groups[1].registerForMessages(GroupZapNodeMessage.class, listener2);

    TCGroupMember member1 = groups[0].openChannel(LOCALHOST, groupPorts[1]);
    Thread.sleep(200);
    TCGroupMember member2 = groups[1].getMembers().get(0);

    long weights[] = new long[] { 1, 23, 44, 78 };
    GroupMessage sMesg = new GroupZapNodeMessage(GroupZapNodeMessage.ZAP_NODE_REQUEST,
                                                 L2HAZapNodeRequestProcessor.SPLIT_BRAIN, "Zapping node", weights);

    groups[0].sendTo(member1.getPeerNodeID(), sMesg);
    GroupMessage rMesg = listener2.getNextMessageFrom(groups[0].getNodeID());
    assertTrue(sMesg.toString().equals(rMesg.toString()));

    groups[1].sendTo(member2.getPeerNodeID(), sMesg);
    rMesg = listener1.getNextMessageFrom(groups[1].getNodeID());
    assertTrue(sMesg.toString().equals(rMesg.toString()));

    tearGroups();
  }

  public void testSendTCGroupPingMessage() throws Exception {
    int nGrp = 2;
    setupGroups(nGrp);

    groups[0].join(nodes[0], nodes);
    groups[1].join(nodes[1], nodes);
    Thread.sleep(500);
    assertEquals(1, groups[0].size());
    assertEquals(1, groups[1].size());

    TCGroupMember member = groups[0].getMembers().get(0);
    TCGroupPingMessage ping = (TCGroupPingMessage) member.getChannel().createMessage(TCMessageType.GROUP_PING_MESSAGE);
    ping.okMessage();
    ping.send();
    LinkedBlockingQueue<TCGroupPingMessage> pingQueue = ((TCGroupManagerImpl) groups[1]).getPingQueue();
    TCGroupPingMessage rcvPing = pingQueue.poll(1000, TimeUnit.MILLISECONDS);
    assertTrue(rcvPing.isOkMessage());

    member = groups[1].getMembers().get(0);
    ping = (TCGroupPingMessage) member.getChannel().createMessage(TCMessageType.GROUP_PING_MESSAGE);
    ping.denyMessage();
    ping.send();
    pingQueue = ((TCGroupManagerImpl) groups[0]).getPingQueue();
    rcvPing = pingQueue.poll(1000, TimeUnit.MILLISECONDS);
    assertTrue(!rcvPing.isOkMessage());

    tearGroups();
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
    Thread.sleep(500);
    assertEquals(1, groups[0].size());
    assertEquals(1, groups[1].size());

    GroupMessage sMesg = createTestObjectSyncMessage();
    TCGroupMember member = groups[0].getMembers().get(0);
    groups[0].sendTo(member.getPeerNodeID(), sMesg);
    GroupMessage rMesg = listeners[1].getNextMessageFrom(groups[0].getNodeID());
    assertTrue(cmpObjectSyncMessage((ObjectSyncMessage) sMesg, (ObjectSyncMessage) rMesg));

    sMesg = createTestObjectSyncMessage();
    member = groups[1].getMembers().get(0);
    groups[1].sendTo(member.getPeerNodeID(), sMesg);
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
    return ((o1.getType() == o2.getType() && o1.getMessageID().equals(o2.getMessageID()) && o1.getGCedObjectIDs()
        .equals(o2.getGCedObjectIDs())));
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
    Thread.sleep(1500);
    for (int i = 0; i < nGrp; ++i) {
      assertEquals(nGrp - 1, groups[i].size());
    }

    // test with one to one first
    GroupMessage sMesg = createGCResultMessage();
    TCGroupMember member = groups[0].getMembers().get(0);
    groups[0].sendTo(member.getPeerNodeID(), sMesg);
    TestGroupMessageListener listener = listenerMap.get(member.getPeerNodeID());
    GroupMessage rMesg = listener.getNextMessageFrom(groups[0].getNodeID());
    assertTrue(cmpGCResultMessage((GCResultMessage) sMesg, (GCResultMessage) rMesg));

    sMesg = createGCResultMessage();
    member = groups[1].getMembers().get(0);
    groups[1].sendTo(member.getPeerNodeID(), sMesg);
    listener = listenerMap.get(member.getPeerNodeID());
    rMesg = listener.getNextMessageFrom(groups[1].getNodeID());
    assertTrue(cmpGCResultMessage((GCResultMessage) sMesg, (GCResultMessage) rMesg));

    // test with broadcast
    sMesg = createGCResultMessage();
    groups[0].sendAll(sMesg);
    for (int i = 0; i < groups[0].size(); ++i) {
      TCGroupMember m = groups[0].getMembers().get(i);
      TestGroupMessageListener l = listenerMap.get(m.getPeerNodeID());
      rMesg = l.getNextMessageFrom(groups[0].getNodeID());
      assertTrue(cmpGCResultMessage((GCResultMessage) sMesg, (GCResultMessage) rMesg));
    }

    Thread.sleep(200);
    tearGroups();
  }

  private L2StateMessage createL2StateMessage() {
    long weights[] = new long[] { 1, 23, 44, 78 };
    Enrollment enroll = new Enrollment(new NodeIdUuidImpl("test"), true, weights);
    L2StateMessage message = new L2StateMessage(L2StateMessage.START_ELECTION, enroll);
    return (message);
  }

  private boolean cmpL2StateMessage(L2StateMessage o1, L2StateMessage o2) {
    return (o1.getEnrollment().equals(o2.getEnrollment()) && (o1.getType() == o2.getType()) && o1.getMessageID()
        .equals(o2.getMessageID()));
  }

  public void testSendToAndWait() throws Exception {
    int nGrp = 5;
    setupGroups(nGrp);
    HashMap<NodeID, TestGroupMessageListener> listenerMap = new HashMap<NodeID, TestGroupMessageListener>();

    for (int i = 0; i < nGrp; ++i) {
      listeners[i] = new responseL2StateMessageListener(groups[i], 100);
      groups[i].registerForMessages(L2StateMessage.class, listeners[i]);
      listenerMap.put(groups[i].getNodeID(), listeners[i]);
    }
    for (int i = 0; i < nGrp; ++i) {
      groups[i].join(nodes[i], nodes);
    }
    Thread.sleep(1500);
    for (int i = 0; i < nGrp; ++i) {
      assertEquals(nGrp - 1, groups[i].size());
    }

    for (int i = 0; i < groups[0].getMembers().size(); ++i) {
      GroupMessage sMesg = createL2StateMessage();
      TCGroupMember member = groups[0].getMembers().get(i);
      groups[0].sendToAndWaitForResponse(member.getPeerNodeID(), sMesg);
      TestGroupMessageListener listener = listenerMap.get(member.getPeerNodeID());
      GroupMessage rMesg = listener.getNextMessageFrom(groups[0].getNodeID());
      assertTrue(cmpL2StateMessage((L2StateMessage) sMesg, (L2StateMessage) rMesg));

      sMesg = createL2StateMessage();
      member = groups[1].getMembers().get(i);
      groups[1].sendToAndWaitForResponse(member.getPeerNodeID(), sMesg);
      listener = listenerMap.get(member.getPeerNodeID());
      rMesg = listener.getNextMessageFrom(groups[1].getNodeID());
      assertTrue(cmpL2StateMessage((L2StateMessage) sMesg, (L2StateMessage) rMesg));
    }

    Thread.sleep(200);
    tearGroups();
  }

  public void testSendAllAndWait() throws Exception {
    int nGrp = 5;
    setupGroups(nGrp);
    HashMap<NodeID, TestGroupMessageListener> listenerMap = new HashMap<NodeID, TestGroupMessageListener>();

    for (int i = 0; i < nGrp; ++i) {
      listeners[i] = new responseL2StateMessageListener(groups[i], 100);
      groups[i].registerForMessages(L2StateMessage.class, listeners[i]);
      listenerMap.put(groups[i].getNodeID(), listeners[i]);
    }
    for (int i = 0; i < nGrp; ++i) {
      groups[i].join(nodes[i], nodes);
    }
    Thread.sleep(1500);
    for (int i = 0; i < nGrp; ++i) {
      assertEquals(nGrp - 1, groups[i].size());
    }

    for (int m = 0; m < nGrp; ++m) {
      TCGroupManagerImpl ms = groups[m];
      GroupMessage sMesg = createL2StateMessage();
      ms.sendAllAndWaitForResponse(sMesg);
      for (int i = 0; i < ms.getMembers().size(); ++i) {
        TCGroupMember member = ms.getMembers().get(i);
        TestGroupMessageListener listener = listenerMap.get(member.getPeerNodeID());
        GroupMessage rMesg = listener.getNextMessageFrom(ms.getNodeID());
        assertTrue(cmpL2StateMessage((L2StateMessage) sMesg, (L2StateMessage) rMesg));
      }
    }

    Thread.sleep(200);
    tearGroups();
  }

  public void testZapNode() throws Exception {
    int nGrp = 2;
    MyGroupEventListener eventListeners[] = new MyGroupEventListener[nGrp];
    MyZapNodeRequestProcessor zaps[] = new MyZapNodeRequestProcessor[nGrp];
    NodeID nodeIDs[] = new NodeID[nGrp];
    setupGroups(nGrp);
    HashMap<NodeID, TestGroupMessageListener> listenerMap = new HashMap<NodeID, TestGroupMessageListener>();

    for (int i = 0; i < nGrp; ++i) {
      eventListeners[i] = new MyGroupEventListener();
      groups[i].registerForGroupEvents(eventListeners[i]);
      zaps[i] = new MyZapNodeRequestProcessor();
      groups[i].setZapNodeRequestProcessor(zaps[i]);
      groups[i].registerForMessages(TestMessage.class, listeners[i]);
      listenerMap.put(groups[i].getNodeID(), listeners[i]);
    }
    for (int i = 0; i < nGrp; ++i) {
      nodeIDs[i] = groups[i].join(nodes[i], nodes);
    }
    Thread.sleep(500);
    for (int i = 0; i < nGrp; ++i) {
      assertEquals(nGrp - 1, groups[i].size());
    }

    TestMessage msg1 = new TestMessage("Hello there");
    TCGroupMember member = groups[0].getMembers().get(0);
    groups[0].sendAll(msg1);
    TestGroupMessageListener listener = listenerMap.get(member.getPeerNodeID());
    TestMessage msg2 = (TestMessage) listener.getNextMessageFrom(groups[0].getNodeID());
    assertEquals(msg1, msg2);

    TestMessage msg3 = new TestMessage("Hello back");
    member = groups[1].getMembers().get(0);
    groups[1].sendAll(msg3);
    listener = listenerMap.get(member.getPeerNodeID());
    TestMessage msg4 = (TestMessage) listener.getNextMessageFrom(groups[1].getNodeID());
    assertEquals(msg3, msg4);

    System.err.println("ZAPPING NODE : " + nodeIDs[1]);
    groups[0].zapNode(nodeIDs[1], 01, "test : Zap the other node " + nodeIDs[1] + " from " + nodeIDs[0]);

    Object r1 = zaps[0].outgoing.take();
    Object r2 = zaps[1].incoming.take();
    assertEquals(r1, r2);

    r1 = zaps[0].outgoing.poll(500);
    assertNull(r1);
    r2 = zaps[1].incoming.poll(500);
    assertNull(r2);

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
      // System.out.println("XXX expected:" + nodeID + " got:" + pkg.getNodeID());
      assertTrue(nodeID.equals(pkg.getNodeID()));
      return (pkg.getMessage());
    }
  }

  private class responseL2StateMessageListener extends TestGroupMessageListener {
    TCGroupManagerImpl manager;

    responseL2StateMessageListener(TCGroupManagerImpl manager, long timeout) {
      super(timeout);
      this.manager = manager;
    }

    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      L2StateMessage message = (L2StateMessage) msg;
      GroupMessage resultAgreed = L2StateMessageFactory.createResultAgreedMessage(message, message.getEnrollment());
      try {
        manager.sendTo(message.messageFrom(), resultAgreed);
      } catch (GroupException e) {
        throw new RuntimeException(e);
      }
      super.messageReceived(fromNode, msg);
    }
  }

  private static final class MyZapNodeRequestProcessor implements ZapNodeRequestProcessor {

    public NoExceptionLinkedQueue outgoing = new NoExceptionLinkedQueue();
    public NoExceptionLinkedQueue incoming = new NoExceptionLinkedQueue();

    public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int type, String reason) {
      outgoing.put(reason);
      return true;
    }

    public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason, long[] weights) {
      incoming.put(reason);
    }

    public long[] getCurrentNodeWeights() {
      return new long[0];
    }
  }

  private static final class MyGroupEventListener implements GroupEventsListener {

    private NodeID lastNodeJoined;
    private NodeID lastNodeLeft;

    public void nodeJoined(NodeID nodeID) {
      System.err.println("\n### nodeJoined -> " + nodeID);
      lastNodeJoined = nodeID;
    }

    public void nodeLeft(NodeID nodeID) {
      System.err.println("\n### nodeLeft -> " + nodeID);
      lastNodeLeft = nodeID;
    }

    public NodeID getLastNodeJoined() {
      return lastNodeJoined;
    }

    public NodeID getLastNodeLeft() {
      return lastNodeLeft;
    }

    public void reset() {
      lastNodeJoined = lastNodeLeft = null;
    }
  }

  private static final class TestMessage extends AbstractGroupMessage {

    // to make serialization sane
    public TestMessage() {
      super(0);
    }

    public TestMessage(String message) {
      super(0);
      this.msg = message;
    }

    String msg;

    @Override
    protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
      msg = in.readUTF();

    }

    @Override
    protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
      out.writeUTF(msg);

    }

    public int hashCode() {
      return msg.hashCode();
    }

    public boolean equals(Object o) {
      if (o instanceof TestMessage) {
        TestMessage other = (TestMessage) o;
        return this.msg.equals(other.msg);
      }
      return false;
    }

    public String toString() {
      return "TestMessage [ " + msg + "]";
    }
  }

}
