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
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCGroupMemberDiscoveryStatic implements TCGroupMemberDiscovery {
  private static final TCLogger               logger            = TCLogging
                                                                    .getLogger(TCGroupMemberDiscoveryStatic.class);

  private Node                                local;
  private Node[]                              nodes;
  private TCGroupManagerImpl                  manager;
  private AtomicBoolean                       running           = new AtomicBoolean(false);
  private AtomicBoolean                       stopAttempt       = new AtomicBoolean(false);
  private boolean                             debug             = false;
  private long                                connectIntervalms = 1000;
  private ConcurrentHashMap<Node, NodeStatus> statusMap         = new ConcurrentHashMap<Node, NodeStatus>();

  public TCGroupMemberDiscoveryStatic(L2TVSConfigurationSetupManager configSetupManager) {
    nodes = makeAllNodes(configSetupManager);
  }

  /*
   * for testing purpose
   */
  public TCGroupMemberDiscoveryStatic(Node[] nodes) {
    for (Node node : nodes) {
      statusMap.put(node, new NodeStatus());
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
    statusMap.put(node, new NodeStatus());
    return (node);
  }

  public Node[] getAllNodes() {
    return nodes;
  }

  public void setTCGroupManager(TCGroupManagerImpl manager) {
    this.manager = manager;
  }

  public void start() throws GroupException {
    if (nodes == null || nodes.length == 0) { throw new GroupException("Wrong nodes"); }

    if (running.get()) return;
    stopAttempt.set(false);
    running.set(true);
    Thread discover = new Thread(new Runnable() {
      public void run() {
        while (!stopAttempt.get()) {
          openChannels();
          ThreadUtil.reallySleep(connectIntervalms);
        }
        stopAttempt.set(false);
        running.set(false);
      }
    }, "Member discovery");
    discover.start();
  }

  /*
   * Open channel to each unconnected Node
   */
  protected void openChannels() {

    ArrayList<Node> toConnectList = new ArrayList<Node>();
    for (int i = 0; i < nodes.length; ++i) {
      Node n = nodes[i];
      NodeStatus status = statusMap.get(n);

      // skip local one
      if (local.equals(n) || (n == null)) continue;

      if (getMember(n) == null) {
        if (status.isTimeToCheck()) toConnectList.add(n);
      } else {
        status.setOk();
      }
    }

    ChannelOpener chOpeners[] = new ChannelOpener[toConnectList.size()];
    for (int i = 0; i < toConnectList.size(); ++i) {
      chOpeners[i] = new ChannelOpener(toConnectList.get(i));
      chOpeners[i].start();
    }
    for (int i = 0; i < toConnectList.size(); ++i) {
      try {
        chOpeners[i].join();
      } catch (InterruptedException e) {
        logger.warn("Connect to " + toConnectList.get(i) + " " + e);
      }
    }
  }

  private class ChannelOpener extends Thread {
    private Node       node;
    private NodeStatus status;

    ChannelOpener(Node node) {
      super(local.toString() + " open channel to " + node);
      this.node = node;
      this.status = statusMap.get(this.node);
    }

    public void run() {
      try {
        if (debug) {
          logger.debug(manager.getNodeID().toString() + " opens channel to " + node);
        }
        manager.openChannel(node.getHost(), node.getPort());
      } catch (TCTimeoutException e) {
        status.setBad();
        logger.warn("Node:" + node + " " + e);
      } catch (UnknownHostException e) {
        status.setVerybad();
        logger.warn("Node:" + node + " " + e);
      } catch (MaxConnectionsExceededException e) {
        status.setBad();
        logger.warn("Node:" + node + " " + e);
      } catch (IOException e) {
        status.setBad();
        logger.warn("Node:" + node + " " + e);
      }
    }
  }

  private TCGroupMember getMember(Node node) {
    String sid = manager.makeGroupNodeName(node.getHost(), node.getPort());
    TCGroupMember member = null;
    Iterator it = manager.getMembers().iterator();
    while (it.hasNext()) {
      TCGroupMember m = (TCGroupMember) it.next();
      if (sid.equals(m.getPeerNodeID().getName())) {
        member = m;
        break;
      }
    }
    return member;
  }

  public void stop() {
    if (!running.get()) return;
    stopAttempt.set(true);
  }

  public void setLocalNode(Node local) {
    this.local = local;
  }

  public Node getLocalNode() {
    return local;
  }

  private class NodeStatus {
    public final int STATUS_UNKNOWN  = 0;
    public final int STATUS_OK       = 1;
    public final int STATUS_BAD      = 2;
    public final int STATUS_VERY_BAD = 3;

    private int      status;
    private int      badCount;
    private long     timestamp;

    public NodeStatus() {
      status = STATUS_UNKNOWN;
      badCount = 0;
    }

    public synchronized boolean isTimeToCheck() {
      if (status == STATUS_UNKNOWN || status == STATUS_OK) return true;

      switch (status) {
        case STATUS_UNKNOWN:
        case STATUS_OK:
          return true;
        case STATUS_BAD:
          // check 10 times then every min
          if (badCount <= 10) {
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

    public synchronized void setOk() {
      status = STATUS_OK;
      badCount = 0;
    }

    public synchronized boolean isOk() {
      return (status == STATUS_OK);
    }

    public synchronized void setBad() {
      ++badCount;
      status = STATUS_BAD;
    }

    public synchronized boolean isBad() {
      return (status == STATUS_BAD);
    }

    public synchronized void setVerybad() {
      status = STATUS_VERY_BAD;
      timestamp = System.currentTimeMillis();
    }

    public synchronized boolean isVerybad() {
      return (status == STATUS_VERY_BAD);
    }

    public synchronized void reset() {
      status = STATUS_UNKNOWN;
      badCount = 0;
    }

  }

}