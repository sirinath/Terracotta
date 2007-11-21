/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;

/**
 * JDK 1.4 (NIO) version of TCComm. Uses a single internal thread and a selector to manage channels associated with
 * <code>TCConnection</code>'s
 *
 * @author teck
 */
class TCCommJDK14 implements TCComm, TCListenerEventListener {

  protected static final TCLogger   logger         = TCLogging.getLogger(TCComm.class);

  private final TCWorkerCommManager workerCommMgr;
  private final CoreNIOServices     commThread;
  private final String              commThreadName = "TCComm Main Selector Thread";
  private volatile boolean          started        = false;

  public TCCommJDK14() {
    // no worker threads for you ...
    this(-1);
  }

  TCCommJDK14(int workerCommCount) {
    if (workerCommCount > 0) {
      workerCommMgr = new TCWorkerCommManager(this, workerCommCount);
    } else {
      logger.info("Comm Worker Threads NOT requested");
      workerCommMgr = null;
    }

    commThread = new CoreNIOServices(commThreadName, this, workerCommMgr);
  }

  public void closeEvent(TCListenerEvent event) {
    this.commThread.listenerRemoved();
  }

  void listenerAdded(TCListener listener) {
    this.commThread.listenerAdded(listener);
  }

  public boolean isStarted() {
    return started;
  }

  public boolean isStopped() {
    return !started;
  }

  public final synchronized void start() {
    if (!started) {
      started = true;
      if (logger.isDebugEnabled()) {
        logger.debug("Start requested");
      }

      // The worker comm threads
      if (workerCommMgr != null) {
        workerCommMgr.start();
      }

      // The Main Listener
      commThread.start();
    }
  }

  public final synchronized void stop() {
    if (started) {
      started = false;
      if (logger.isDebugEnabled()) {
        logger.debug("Stop requested");
      }
      commThread.requestStop();
      if (workerCommMgr != null && workerCommMgr.isStarted() == true) {
        workerCommMgr.stop();
      }
    }
  }

  public CoreNIOServices nioServiceThreadForNewConnection() {
    // For now we're always assuming that client side comms use the main selector
    return commThread;
  }

  public CoreNIOServices nioServiceThreadForNewListener() {
    return commThread;
  }

}
