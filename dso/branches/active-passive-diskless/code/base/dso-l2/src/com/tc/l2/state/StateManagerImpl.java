/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.NodeID;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;

public class StateManagerImpl implements StateManager, Runnable {

  private static final TCLogger         logger  = TCLogging.getLogger(StateManagerImpl.class);

  private final TCLogger                consoleLogger;
  private final DistributedObjectServer server;
  private final GroupManager            groupManager;
  private final SetOnceFlag             started = new SetOnceFlag(false);
  private NodeID                        myNodeId;

  public StateManagerImpl(TCLogger consoleLogger, DistributedObjectServer server, GroupManager groupManager) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.groupManager = groupManager;
  }

  // TODO:: This implementation is just for initial testing. This will be replaced with actual l2 co-ordination
  public void start() {
    try {
      started.set();
      this.myNodeId = groupManager.join();
      logger.info("L2 Node ID = " + myNodeId);
    } catch (GroupException e) {
      logger.error("Caught Exception :", e);
      throw new AssertionError(e);
    }
    // Thread t = new Thread(this, "L2 State co-ordinator");
    // t.setDaemon(true);
    // t.start();
  }

  public void run() {
    while (true) {
      ThreadUtil.reallySleep(15000);
      consoleLogger.info("Moving to Active state");
      try {
        server.startActiveMode();
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
      ThreadUtil.reallySleep(15000);
      consoleLogger.info("Moving to Passive state");
      try {
        server.stopActiveMode();
      } catch (TCTimeoutException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

}
