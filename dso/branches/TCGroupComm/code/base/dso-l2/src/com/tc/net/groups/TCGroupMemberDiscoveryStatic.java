/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.TCExceptionResultException;
import com.tc.util.concurrent.TCFuture;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCGroupMemberDiscoveryStatic implements TCGroupMemberDiscovery {
  private static final TCLogger                     logger            = TCLogging
                                                                          .getLogger(TCGroupMemberDiscoveryStatic.class);

  private Node                                      local;
  private final Node[]                              nodes;
  private TCGroupManagerImpl                        manager;
  private final AtomicBoolean                       running           = new AtomicBoolean(false);
  private final AtomicBoolean                       stopAttempt       = new AtomicBoolean(false);
  private final ConcurrentHashMap<Node, NodeStatus> statusMap         = new ConcurrentHashMap<Node, NodeStatus>();
  private final static long                         connectIntervalms = 1000;
  private final static long                         handshakeTimeout;
  static {
    handshakeTimeout = TCPropertiesImpl.getProperties().getLong(TCGroupManagerImpl.NHA_TCCOMM_HANDSHAKE_TIMEOUT);
  }

  public TCGroupMemberDiscoveryStatic(L2TVSConfigurationSetupManager configSetupManager) {
    nodes = makeAllNodes(configSetupManager);
  }

  /*
   * for testing purpose only
   */
  TCGroupMemberDiscoveryStatic(Node[] nodes) {
    for (Node node : nodes) {
      statusMap.put(node, new NodeStatus(node));
    }
    this.nodes = nodes;
  }

  private Node[] makeAllNodes(L2TVSConfigurationSetupManager configSetupManager) {
    String[] l2s = configSetupManager.allCurrentlyKnownServers();
    Node[] rv = new Node[l2s.length];
    for (int i = 0; i < l2s.length; i++) {
      NewL2DSOConfig l2;
      try {
        l2 = configSetupManager.dsoL2ConfigFor(l2s[i]);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException("Error getting l2 config for: " + l2s[i], e);
      }
      rv[i] = makeNode(l2, null);
    }
    return rv;
  }

  private Node makeNode(NewL2DSOConfig l2, String bind) {
    Node node = new Node(l2.host().getString(), l2.l2GroupPort().getInt(), bind);
    statusMap.put(node, new NodeStatus(node));
    return (node);
  }

  public void setTCGroupManager(TCGroupManagerImpl manager) {
    this.manager = manager;
  }

  TCGroupMember openChannel(String hostname, int groupPort) throws TCTimeoutException, UnknownHostException,
      MaxConnectionsExceededException, IOException {
    return (manager.openChannel(hostname, groupPort));
  }

  NodeID getLocalNodeID() {
    return (manager.getLocalNodeID());
  }

  public void start() throws GroupException {
    if (nodes == null || nodes.length == 0) { throw new GroupException("No nodes"); }

    if (running.getAndSet(true)) {
      Assert.failure("Not to start discovert second time");
    }

    Thread discover = new Thread(new Runnable() {
      public void run() {
        while (!stopAttempt.get()) {
          openChannels();
          ThreadUtil.reallySleep(connectIntervalms);
        }
        running.set(false);
      }
    }, "Static Member discovery");
    discover.setDaemon(true);
    discover.start();
  }

  /*
   * Open channel to each not connected Node
   */
  protected void openChannels() {

    for (int i = 0; i < nodes.length; ++i) {
      Node n = nodes[i];
      Assert.assertNotNull(n);

      // skip local one
      if (local.equals(n)) continue;

      NodeStatus status = statusMap.get(n);
      Assert.assertNotNull(status);

      if (findNodeID(n) == null) {
        if (status.isTimeToCheck(local)) {
          status.newFuture();
          ChannelOpener chOpener = new ChannelOpener(this, local, n, status);
          chOpener.start();
          // interrupt/cancel thread if make no progress
          CancelThread cancelThread = new CancelThread(this, n, chOpener, handshakeTimeout);
          cancelThread.start();
        }
      } else {
        status.setOk();
      }
    }
  }

  private static class CancelThread extends Thread {
    private final TCGroupMemberDiscoveryStatic discover;
    private final Node                         node;
    private final Thread                       target;
    private final long                         timeout;

    public CancelThread(TCGroupMemberDiscoveryStatic discover, Node node, Thread target, long timeout) {
      super("CancelThread on " + target);
      this.discover = discover;
      this.node = node;
      this.target = target;
      this.timeout = timeout;
    }

    public void run() {
      ThreadUtil.reallySleep(timeout);
      // cancel thread if made no progress
      if (target.isAlive() && (discover.findNodeID(node) == null)) {
        target.interrupt();
        logger.warn("Cancelled channel opener " + target);
      }
    }
  }

  private static class ChannelOpener extends Thread {
    private final TCGroupMemberDiscoveryStatic discover;
    private final Node                         node;
    private final NodeStatus                   status;

    ChannelOpener(TCGroupMemberDiscoveryStatic discover, Node local, Node node, NodeStatus status) {
      super(local.toString() + " open channel to " + node);
      this.discover = discover;
      this.node = node;
      this.status = status;
    }

    public void run() {

      TCFuture runStatus = status.getFuture();
      Assert.assertNotNull(status);
      try {
        if (logger.isDebugEnabled()) logger.debug(discover.getLocalNodeID().toString() + " opens channel to " + node);
        discover.openChannel(node.getHost(), node.getPort());
        runStatus.set(new Object());
      } catch (TCTimeoutException e) {
        status.setBad();
        status.setException(e);
        logger.warn("Node:" + node + " " + e);
      } catch (UnknownHostException e) {
        status.setVerybad();
        status.setException(e);
        logger.warn("Node:" + node + " " + e);
      } catch (MaxConnectionsExceededException e) {
        status.setBad();
        status.setException(e);
        logger.warn("Node:" + node + " " + e);
      } catch (IOException e) {
        status.setBad();
        status.setException(e);
        logger.warn("Node:" + node + " " + e);
      } catch (RuntimeException e) {
        // catch InterrupptedException thrown from TCFuture
        // at ClientMessageTransport.waitForSynAck
        status.setBad();
        status.setException(e);
        logger.warn("Node:" + node + " " + e);
      }
    }
  }

  private NodeIdComparable findNodeID(Node node) {
    return (manager.findNodeID(node));
  }

  public void stop() {
    stopAttempt.set(true);
  }

  public void setLocalNode(Node local) {
    this.local = local;
  }

  public Node getLocalNode() {
    return local;
  }

  private static class NodeStatus {
    public final static int STATUS_UNKNOWN  = 0;
    public final static int STATUS_OK       = 1;
    public final static int STATUS_BAD      = 2;
    public final static int STATUS_VERY_BAD = 3;

    private int             status;
    private int             badCount;
    private long            timestamp;
    private final Node      node;
    private TCFuture        future;

    NodeStatus(Node node) {
      this.node = node;
      status = STATUS_UNKNOWN;
      badCount = 0;
    }

    synchronized TCFuture newFuture() {
      return (future = new TCFuture());
    }

    synchronized TCFuture getFuture() {
      return (future);
    }

    synchronized void clrFuture() {
      future = null;
    }

    synchronized void setException(Throwable e) {
      future.setException(e);
    }

    synchronized Object peekFuture() throws TCTimeoutException, InterruptedException, TCExceptionResultException {
      if (future == null) return null;
      return (future.get(1));
    }

    synchronized boolean isTimeToCheck(Node local) {
      try {
        peekFuture();
      } catch (TCTimeoutException e) {
        // not ready yet
        logger.warn("Waiting connection from " + local + " to " + node);
        return false;
      } catch (InterruptedException e) {
        logger.warn("Interrupped connection from " + local + " to " + node);
      } catch (TCExceptionResultException e) {
        logger.warn("ResultException on connection from " + local + " to " + node);
      }

      switch (status) {
        case STATUS_UNKNOWN:
        case STATUS_OK:
          return true;
        case STATUS_BAD:
          // check 60 times then every min
          if (badCount <= 60) {
            ++badCount;
            timestamp = System.currentTimeMillis();
            return true;
          }
          if (System.currentTimeMillis() > (timestamp + 1000 * 60)) {
            timestamp = System.currentTimeMillis();
            return true;
          } else {
            return false;
          }
        case STATUS_VERY_BAD:
          // check every 5 min
          if (System.currentTimeMillis() > (timestamp + 1000 * 60 * 5)) {
            timestamp = System.currentTimeMillis();
            return true;
          } else {
            return false;
          }
        default:
          return true;
      }
    }

    synchronized void setOk() {
      status = STATUS_OK;
      badCount = 0;
    }

    synchronized boolean isOk() {
      return (status == STATUS_OK);
    }

    synchronized void setBad() {
      ++badCount;
      status = STATUS_BAD;
    }

    synchronized boolean isBad() {
      return (status == STATUS_BAD);
    }

    synchronized void setVerybad() {
      status = STATUS_VERY_BAD;
      timestamp = System.currentTimeMillis();
    }

    synchronized boolean isVerybad() {
      return (status == STATUS_VERY_BAD);
    }

    synchronized void reset() {
      status = STATUS_UNKNOWN;
      badCount = 0;
    }

  }

}