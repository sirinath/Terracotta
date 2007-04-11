/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.cluster.ClusterEventListener;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class ServerCrashAndRestartTestApp extends ServerCrashingAppBase implements ClusterEventListener {

  // TODO: remove
  protected String appId;

  public ServerCrashAndRestartTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);

    // TODO: remove
    this.appId = appId;
  }

  private final int           initialNodeCount = getParticipantCount();
  private final CyclicBarrier barrier          = new CyclicBarrier(initialNodeCount);
  private String              thisNode;

  public void runTest() throws Throwable {

    // TODO: remove
    System.err.println("********  test app for appId=[" + appId + "]");

    ManagerUtil.addClusterEventListener(this);
    final boolean isMasterNode = barrier.barrier() == 0;
    if (isMasterNode) {
      getConfig().getServerControl().crash();
      getConfig().getServerControl().start(30 * 1000);
    }
    barrier.barrier();
  }

  public void nodeConnected(String nodeId) {
    trace("nodeConnected");
  }

  public void nodeDisconnected(String nodeId) {
    trace("nodeDisconnected");
  }

  public void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    this.thisNode = thisNodeId;
    trace("thisNodeConnected");
  }

  public void thisNodeDisconnected(String thisNodeId) {
    trace("thisNodeDisconnected");
  }

  private void trace(String msg) {
    System.err.println("### " + msg + " -> nodeId=" + thisNode + ", ThreadId=" + Thread.currentThread().getName());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addIncludePattern(ServerCrashAndRestartTestApp.class.getName());
    config.addRoot("barrier", ServerCrashAndRestartTestApp.class.getName() + ".barrier");
    config.addWriteAutolock("* " + ServerCrashAndRestartTestApp.class.getName() + ".*(..)");

    config.addIncludePattern(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + ".*(..)");
  }
}
