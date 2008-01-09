/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCGroupMemberDiscoveryStatic implements TCGroupMemberDiscovery {
  private static final TCLogger logger            = TCLogging.getLogger(TCGroupMemberDiscoveryStatic.class);

  private Node                  local;
  private Node[]                nodes;
  private TCGroupManager        manager;
  private AtomicBoolean         running           = new AtomicBoolean(false);
  private AtomicBoolean         stopAttempt       = new AtomicBoolean(false);
  private boolean               debug             = true;
  private long                  connectIntervalms = 1000;

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
    if (nodes == null || nodes.length == 0) {
      throw new GroupException("Wrong nodes");
    }
    
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
    for (int i = 0; i < nodes.length; ++i) {
      Node n = nodes[i];

      // skip local one
      if (local.equals(n) || (n == null)) continue;

      TCSocketAddress remote;
      try {
        remote = new TCSocketAddress(n.getHost(), n.getPort());
      } catch (UnknownHostException e) {
        nodes[i] = null;
        logger.error("Removed bad node:" + n + " " + e);
        continue;
      }

      if (getMember(remote) == null) {
        try {
          if (debug) {
            logger.debug(manager.getNodeID().toString() + " opens channel to " + remote);
          }
          manager.openChannel(n.getHost(), n.getPort());
        } catch (TCTimeoutException e) {
          logger.warn("Node:" + n + " " + e);
        } catch (UnknownHostException e) {
          logger.warn("Node:" + n + " " + e);
        } catch (MaxConnectionsExceededException e) {
          logger.warn("Node:" + n + " " + e);
        } catch (IOException e) {
          logger.warn("Node:" + n + " " + e);
        }
      }
    }
  }

  private TCGroupMember getMember(TCSocketAddress remote) {
    TCGroupMember member = null;
    Iterator it = manager.getMembers().iterator();
    while (it.hasNext()) {
      TCGroupMember m = (TCGroupMember) it.next();
      if (remote.equals(m.getChannel().getRemoteAddress())) {
        member = m;
        break;
      }
    }
    return member;
  }

  public void stop() {
    if (!running.get()) return;
    stopAttempt.set(false);
  }

  public void setLocalNode(Node local) {
    this.local = local;
  }

  public Node getLocalNode() {
    return local;
  }

}