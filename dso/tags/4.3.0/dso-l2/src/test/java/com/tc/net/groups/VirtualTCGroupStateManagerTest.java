/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.groups;

import com.google.common.base.Throwables;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.async.impl.MockSink;
import com.tc.async.impl.StageManagerImpl;
import com.tc.config.NodesStore;
import com.tc.config.NodesStoreImpl;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerConfig;
import com.tc.l2.state.StateManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.TestThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.net.groups.HaConfigForGroupNameTests;
import com.tc.objectserver.persistence.TestClusterStatePersistor;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.State;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class VirtualTCGroupStateManagerTest extends TCTestCase {

  private final static String   LOCALHOST = "localhost";
  private static final TCLogger logger    = TCLogging.getLogger(VirtualTCGroupStateManagerTest.class);
  private TestThrowableHandler throwableHandler;
  private TCThreadGroup         threadGroup;

  @Override
  public void setUp() {
    throwableHandler = new TestThrowableHandler(logger);
    threadGroup = new TCThreadGroup(throwableHandler, "VirtualTCGroupStateManagerTestGroup");
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      throwableHandler.throwIfNecessary();
    } catch (Throwable throwable) {
      throw Throwables.propagate(throwable);
    }
  }

  public void testStateManagerTwoServers() throws Exception {
    // 2 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesConcurrentJoining(4, 2);
  }

  public void testStateManagerThreeServers() throws Exception {
    // 3 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesConcurrentJoining(6, 3);
  }

  public void testStateManagerSixServers() throws Exception {
    // 6 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesConcurrentJoining(8, 6);
  }

  public void testStateManagerMixJoinAndElect3() throws Exception {
    // 3 nodes mix join and election
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesMixJoinAndElect(6, 3);
  }

  public void testStateManagerMixJoinAndElect6() throws Exception {
    // 6 nodes mix join and election
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesMixJoinAndElect(8, 6);
  }

  public void testStateManagerJoinLater3() throws Exception {
    // first node shall be active and remaining 2 nodes join later
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesJoinLater(6, 3);
  }

  public void testStateManagerJoinLater6() throws Exception {
    // first node shall be active and remaining 5 nodes join later
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesJoinLater(9, 6);
  }

  // -----------------------------------------------------------------------

  private void nodesConcurrentJoining(int nodes, int virtuals) throws Exception {
    System.out.println("*** Testing total=" + nodes + " with " + virtuals + " nodes join at same time.");
// force gc to try and free uncollected ports
    System.gc();
    
    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
      groupMgr[i] = createTCGroupManager(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgr = new VirtualTCGroupManagerImpl[virtuals];
    Node[] virtualNodes = new Node[virtuals];
    HashSet<String> names = new HashSet<String>();
    for (int i = 0; i < virtuals; ++i) {
      virtualNodes[i] = allNodes[i];
      names.add(virtualNodes[i].getServerNodeName());
    }
    for (int i = 0; i < virtuals; ++i) {
      virtualMgr[i] = new VirtualTCGroupManagerImpl(groupMgr[i], new HaConfigForGroupNameTests(names).getClusterInfo());
    }

    ChangeSink[] sinks = new ChangeSink[nodes];
    StateManager[] managers = new StateManager[nodes];
    L2StateMessageStage[] msgStages = new L2StateMessageStage[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, groupMgr, msgStages);
    }

    ElectionThread[] elections = new ElectionThread[virtuals];
    for (int i = 0; i < virtuals; ++i) {
      elections[i] = new ElectionThread(managers[i]);
    }

    // joining
    System.out.println("*** Start Joining...");
    Set<Node> nodeSet = new HashSet<Node>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    for (int i = 0; i < nodes; ++i) {
      groupMgr[i].join(allNodes[i], nodeStore);
    }
    ThreadUtil.reallySleep(1000 * nodes);

    System.out.println("*** Start Election...");
    // run them concurrently
    for (int i = 0; i < virtuals; ++i) {
      elections[i].start();
    }
    for (int i = 0; i < virtuals; ++i) {
      elections[i].join();
    }

    ThreadUtil.reallySleep(1000 * nodes);
    // verification
    int activeCount = 0;
    for (int i = 0; i < virtuals; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertEquals("Active coordinator", 1, activeCount);

    stopMessageStages(msgStages);
    shutdown(groupMgr);
  }

  private void stopMessageStages(L2StateMessageStage[] msgStages) {
    stopMessageStages(msgStages, 0, msgStages.length);
  }

  private void stopMessageStages(L2StateMessageStage[] msgStages, int start, int end) {
    for (int i = start; i < end; ++i) {
      msgStages[i].requestStop();
    }
  }

  private void shutdown(TCGroupManagerImpl[] groupMgr) {
    // shut them down
    shutdown(groupMgr, 0, groupMgr.length);
    // sleep for old stuffs to go away
    ThreadUtil.reallySleep(5000);
  }

  private void shutdown(TCGroupManagerImpl[] groupMgr, int start, int end) {
    for (int i = start; i < end; ++i) {
      try {
        ThreadUtil.reallySleep(100);
        groupMgr[i].stop(1000);
      } catch (Exception ex) {
        System.out.println("*** Failed to stop Server[" + i + "] " + groupMgr[i] + " " + ex);
      }
    }
    System.out.println("*** shutdown done");
  }

  private void nodesMixJoinAndElect(int nodes, int virtuals) throws Exception {
    System.out.println("*** Testing total=" + nodes + " with " + virtuals
                       + " nodes mixed join and election at same time.");
// force gc to try and free uncollected ports
    System.gc();
    
    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
      groupMgr[i] = createTCGroupManager(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgr = new VirtualTCGroupManagerImpl[virtuals];
    Node[] virtualNodes = new Node[virtuals];
    HashSet<String> names = new HashSet<String>();
    for (int i = 0; i < virtuals; ++i) {
      virtualNodes[i] = allNodes[i];
      names.add(virtualNodes[i].getServerNodeName());
    }
    for (int i = 0; i < virtuals; ++i) {
      virtualMgr[i] = new VirtualTCGroupManagerImpl(groupMgr[i], new HaConfigForGroupNameTests(names).getClusterInfo());
    }

    ChangeSink[] sinks = new ChangeSink[nodes];
    StateManager[] managers = new StateManager[nodes];
    L2StateMessageStage[] msgStages = new L2StateMessageStage[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, groupMgr, msgStages);
    }

    ElectionThread[] elections = new ElectionThread[virtuals];
    for (int i = 0; i < virtuals; ++i) {
      elections[i] = new ElectionThread(managers[i]);
    }

    // Joining and Electing
    System.out.println("*** Start Joining and Electing...");
    Set<Node> nodeSet = new HashSet<Node>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    groupMgr[0].join(allNodes[0], nodeStore);
    elections[0].start();
    for (int i = 1; i < virtuals; ++i) {
      groupMgr[i].join(allNodes[i], nodeStore);
      elections[i].start();
    }
    for (int i = virtuals + 1; i < nodes; ++i) {
      groupMgr[i].join(allNodes[i], nodeStore);
    }

    for (int i = 0; i < virtuals; ++i) {
      elections[i].join();
    }

    ThreadUtil.reallySleep(1000 * nodes);
    // verification
    int activeCount = 0;
    for (int i = 0; i < virtuals; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertEquals("Active coordinator", 1, activeCount);

    stopMessageStages(msgStages);
    shutdown(groupMgr);
  }

  private void nodesJoinLater(int nodes, int virtuals) throws Exception {
// force gc to try and free uncollected ports
    System.gc();
    System.out.println("*** Testing total=" + nodes + " with " + virtuals + " nodes join at later time.");

    final LinkedBlockingQueue<NodeID> joinedNodes = new LinkedBlockingQueue<NodeID>();
    NodeID[] ids = new NodeID[nodes];
    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
      groupMgr[i] = createTCGroupManager(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgr = new VirtualTCGroupManagerImpl[virtuals];
    Node[] virtualNodes = new Node[virtuals];
    HashSet<String> names = new HashSet<String>();
    for (int i = 0; i < virtuals; ++i) {
      virtualNodes[i] = allNodes[i];
      names.add(virtualNodes[i].getServerNodeName());
    }
    for (int i = 0; i < virtuals; ++i) {
      virtualMgr[i] = new VirtualTCGroupManagerImpl(groupMgr[i], new HaConfigForGroupNameTests(names).getClusterInfo());
    }

    ChangeSink[] sinks = new ChangeSink[nodes];
    final StateManager[] managers = new StateManager[nodes];
    L2StateMessageStage[] msgStages = new L2StateMessageStage[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, groupMgr, msgStages);
    }

    ElectionThread[] elections = new ElectionThread[virtuals];
    for (int i = 0; i < virtuals; ++i) {
      elections[i] = new ElectionThread(managers[i]);
    }

    // the first node to be the active one
    System.out.println("*** First node joins to be an active node...");
    Set<Node> nodeSet = new HashSet<Node>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    ids[0] = groupMgr[0].join(allNodes[0], nodeStore);
    managers[0].startElection();
    ThreadUtil.reallySleep(100);

    // move following join nodes to passive-standby
    virtualMgr[0].registerForGroupEvents(new MyGroupEventListener(virtualMgr[0].getLocalNodeID()) {
      @Override
      public void nodeJoined(NodeID nodeID) {
        // save nodeID for moving to passive
        joinedNodes.add(nodeID);
      }
    });

    System.out.println("***  Remaining nodes join");
    nodeSet = new HashSet<Node>();
    Collections.addAll(nodeSet, allNodes);
    nodeStore = new NodesStoreImpl(nodeSet);
    for (int i = 1; i < nodes; ++i) {
      ids[i] = groupMgr[i].join(allNodes[i], nodeStore);
    }

    ThreadUtil.reallySleep(1000);
    int nodesNeedToMoveToPassive = virtuals - 1;
    while (nodesNeedToMoveToPassive > 0) {
      NodeID toBePassiveNode = joinedNodes.take();
      System.out.println("*** moveNodeToPassiveStandby -> " + toBePassiveNode);
      managers[0].moveNodeToPassiveStandby(toBePassiveNode);
      --nodesNeedToMoveToPassive;
    }
    assertTrue(nodesNeedToMoveToPassive == 0);

    ThreadUtil.reallySleep(1000 * nodes);
    // verification: first node must be active
    int activeCount = 0;
    for (int i = 0; i < virtuals; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertEquals("Active coordinator", 1, activeCount);
    assertTrue("Node-0 must be active coordinator", managers[0].isActiveCoordinator());

    // check API
    try {
      // active is supported not to move itself to passive stand-by
      managers[0].moveNodeToPassiveStandby(ids[0]);
      throw new RuntimeException("moveNodeToPassiveStandy expected to trows an expection");
    } catch (Exception x) {
      // expected
    }

    System.out.println("*** Stop active and re-elect");
    // stop active node
    stopMessageStages(msgStages, 0, 1);
    shutdown(groupMgr, 0, 1);

    ElectionIfNecessaryThread reElectThreads[] = new ElectionIfNecessaryThread[virtuals];
    for (int i = 1; i < virtuals; ++i) {
      reElectThreads[i] = new ElectionIfNecessaryThread(managers[i], ids[0]);
    }
    for (int i = 1; i < virtuals; ++i) {
      reElectThreads[i].start();
    }
    for (int i = 1; i < virtuals; ++i) {
      reElectThreads[i].join();
    }
    ThreadUtil.reallySleep(1000);

    // verify
    activeCount = 0;
    for (int i = 1; i < virtuals; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] (" + (active ? "active" : "non-active") + ")state is " + sinks[i]);
    }
    assertEquals("Active coordinator", 1, activeCount);

    // shut them down
    stopMessageStages(msgStages, 1, virtuals);
    shutdown(groupMgr, 1, nodes);
  }

  private TCGroupManagerImpl createTCGroupManager(Node node) throws Exception {
    StageManager stageManager = new StageManagerImpl(threadGroup, new QueueFactory());
    TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), node.getHost(), node.getPort(), node
        .getGroupPort(), stageManager, null);
    ConfigurationContext context = new ConfigurationContextImpl(stageManager);
    stageManager.startAll(context, Collections.EMPTY_LIST);
    return gm;
  }

  private StateManager createStateManageNode(int localIndex, ChangeSink[] sinks, TCGroupManagerImpl[] groupMgr,
                                             L2StateMessageStage[] messageStage) throws Exception {
    TCGroupManagerImpl gm = groupMgr[localIndex];
    gm.setDiscover(new TCGroupMemberDiscoveryStatic(gm));

    MyGroupEventListener gel = new MyGroupEventListener(gm.getLocalNodeID());
    gm.registerForGroupEvents(gel);
    sinks[localIndex] = new ChangeSink(localIndex);
    MyStateManagerConfig config = new MyStateManagerConfig();
    config.electionTime = 5;
    StateManager mgr = new StateManagerImpl(logger, gm, sinks[localIndex], config, WeightGeneratorFactory
        .createDefaultFactory(), new TestClusterStatePersistor());
    messageStage[localIndex] = new L2StateMessageStage(mgr);
    gm.routeMessages(L2StateMessage.class, messageStage[localIndex].getSink());
    messageStage[localIndex].start();
    return (mgr);
  }

  private static class L2StateMessageStage extends Thread {
    private final MockSink               sink;
    private final NoExceptionLinkedQueue processQ = new NoExceptionLinkedQueue();
    private final StateManager           mgr;
    private volatile boolean             stop     = false;

    public L2StateMessageStage(StateManager mgr) {
      this.mgr = mgr;
      this.sink = new MockSink() {
        @Override
        public void add(EventContext ec) {
          processQ.put(ec);
        }
      };
      setDaemon(true);
      setName("L2StateMessageStageThread");
    }

    public synchronized void requestStop() {
      stop = true;
    }

    public synchronized boolean isStopped() {
      return stop;
    }

    public Sink getSink() {
      return sink;
    }

    @Override
    public void run() {
      while (!isStopped()) {
        L2StateMessage m = (L2StateMessage) processQ.poll(3000);
        if (m != null) {
          mgr.handleClusterStateMessage(m);
        }
      }
    }
  }

  private static class ElectionThread extends Thread {
    private StateManager mgr;

    public ElectionThread(StateManager mgr) {
      setMgr(mgr);
    }

    public void setMgr(StateManager mgr) {
      this.mgr = mgr;
    }

    @Override
    public void run() {
      mgr.startElection();
    }
  }

  private static class MyStateManagerConfig implements StateManagerConfig {
    public int electionTime;

    @Override
    public int getElectionTimeInSecs() {
      return electionTime;
    }
  }

  private static class ElectionIfNecessaryThread extends Thread {
    private final StateManager mgr;
    private final NodeID       disconnectedNode;

    public ElectionIfNecessaryThread(StateManager mgr, NodeID disconnectedNode) {
      this.mgr = mgr;
      this.disconnectedNode = disconnectedNode;
    }

    @Override
    public void run() {
      mgr.startElectionIfNecessary(disconnectedNode);
    }
  }

  private static class ChangeSink extends MockSink {
    private final int         serverIndex;
    private StateChangedEvent event = null;

    public ChangeSink(int index) {
      serverIndex = index;
    }

    @Override
    public void add(EventContext context) {
      event = (StateChangedEvent) context;
      System.out.println("*** Server[" + serverIndex + "]: " + event);
    }

    public State getState() {
      if (event == null) return null;
      return event.getCurrentState();
    }

    @Override
    public String toString() {
      State st = getState();
      return ((st != null) ? st.toString() : "<state unknown>");
    }

  }

  private static class MyGroupEventListener implements GroupEventsListener {

    private final NodeID gmNodeID;

    public MyGroupEventListener(NodeID nodeID) {
      this.gmNodeID = nodeID;
    }

    @Override
    public void nodeJoined(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeJoined -> " + nodeID);
    }

    @Override
    public void nodeLeft(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeLeft -> " + nodeID);
    }

  }

}
