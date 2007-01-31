/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.logging.TCLogger;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;

public class StateManagerImpl implements StateManager, Runnable {

  private final TCLogger                consoleLogger;
  private final DistributedObjectServer server;

  public StateManagerImpl(TCLogger consoleLogger, DistributedObjectServer server) {
    this.consoleLogger = consoleLogger;
    this.server = server;
  }

  // TODO:: This implementation is just for initial testing. This will be replaced with actual l2 co-ordination
  public void start() {
    Thread t = new Thread(this, "L2 State co-ordinator");
    t.setDaemon(true);
    t.start();
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
