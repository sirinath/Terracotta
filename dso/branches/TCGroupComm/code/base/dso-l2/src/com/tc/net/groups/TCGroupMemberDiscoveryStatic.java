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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TCGroupMemberDiscoveryStatic implements TCGroupMemberDiscovery {
  private static final TCLogger logger            = TCLogging.getLogger(TCGroupMemberDiscoveryStatic.class);

  private Node                  local;
  private Node[]                nodes;
  private TCGroupManager        manager;
  private AtomicBoolean         running           = new AtomicBoolean(false);
  private AtomicBoolean         stopAttempt       = new AtomicBoolean(false);
  private boolean               debug             = false;
  private long                  connectIntervalms = 1000;
  private final Lock            discoverLock      = new ReentrantLock();

  public TCGroupMemberDiscoveryStatic(L2TVSConfigurationSetupManager configSetupManager) {
    nodes = makeAllNodes(configSetupManager);
  }

  /*
   * for testing purpose
   */
  public TCGroupMemberDiscoveryStatic(Node[] nodes) {
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
      rv[i] = makeNode(l2);
    }
    return rv;
  }

  private static Node makeNode(NewL2DSOConfig l2) {
    return new Node(l2.host().getString(), l2.l2GroupPort().getInt());
  }

  public Node[] getAllNodes() {
    return nodes;
  }

  public void setTCGroupManager(TCGroupManager manager) {
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
          discoverLock.lock();
          openChannels();
          discoverLock.unlock();
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

      // skip local one
      if (local.equals(n) || (n == null)) continue;

      if (getMember(n) == null) {
        toConnectList.add(n);
      }
    }

    ChannelOpener chOpeners[] = new ChannelOpener[toConnectList.size()];
    for (int i = 0; i < toConnectList.size(); ++i) {
      chOpeners[i] = new ChannelOpener(toConnectList.get(i));
      chOpeners[i].start();
      ThreadUtil.reallySleep(10); // avoid storm
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
    private Node node;

    ChannelOpener(Node node) {
      super(local.toString() + " open channel to " + node);
      this.node = node;
    }

    public void run() {
      try {
        if (debug) {
          logger.debug(manager.getNodeID().toString() + " opens channel to " + node);
        }
        manager.openChannel(node.getHost(), node.getPort());
      } catch (TCTimeoutException e) {
        logger.warn("Node:" + node + " " + e);
      } catch (UnknownHostException e) {
        logger.warn("Node:" + node + " " + e);
      } catch (MaxConnectionsExceededException e) {
        logger.warn("Node:" + node + " " + e);
      } catch (IOException e) {
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
      if (sid.equals(((NodeIDImpl) (m.getNodeID())).getName())) {
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

  public void pause() {
    if(debug) {
      logger.info("Lock discovery of " + manager);
    }
    discoverLock.lock();
  }

  public void resume() {
    discoverLock.unlock();
    if(debug) {
      logger.info("Unlock discovery of " + manager);
    }

  }

}