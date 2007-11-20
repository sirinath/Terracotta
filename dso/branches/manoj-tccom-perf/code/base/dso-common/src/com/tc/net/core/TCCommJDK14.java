/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;
import com.tc.util.Assert;

import java.nio.channels.ServerSocketChannel;

/**
 * JDK 1.4 (NIO) version of TCComm. Uses a single internal thread and a selector to manage channels associated with
 * <code>TCConnection</code>'s
 * 
 * @author teck
 */
class TCCommJDK14 implements TCComm, TCListenerEventListener {

  private TCWorkerCommManager     workerCommMgr       = null;
  private volatile boolean        started             = false;
  private CoreNIOServices         commThread          = null;
  private final String            commThreadName      = "TCComm Main Selector Thread";
  protected static final TCLogger logger              = TCLogging.getLogger(TCComm.class);

  public CoreNIOServices          DEFAULT_COMM_THREAD = null;

  public TCCommJDK14() {
    // no worker threads for you ...
  }

  TCCommJDK14(int workerCommCount) {
    if (workerCommCount > 0) {
      workerCommMgr = new TCWorkerCommManager(this, workerCommCount);
    } else {
      logger.info("Comm Worker Threads NOT requested");
    }
  }

  void stopListener(final ServerSocketChannel ssc, final Runnable callback) {
    Assert.assertNotNull(this.DEFAULT_COMM_THREAD);
    this.DEFAULT_COMM_THREAD.stopListener(ssc, callback);
  }

  void requestAcceptInterest(TCListenerJDK14 lsnr, ServerSocketChannel ssc) {
    Assert.assertNotNull(this.DEFAULT_COMM_THREAD);
    this.DEFAULT_COMM_THREAD.requestAcceptInterest(lsnr, ssc);
  }

  public void closeEvent(TCListenerEvent event) {
    Assert.assertNotNull(this.DEFAULT_COMM_THREAD);
    this.DEFAULT_COMM_THREAD.listenerRemoved();
  }

  void listenerAdded(TCListener listener) {
    Assert.assertNotNull(this.DEFAULT_COMM_THREAD);
    this.DEFAULT_COMM_THREAD.listenerAdded(listener);
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
      commThread = new CoreNIOServices(commThreadName, this, workerCommMgr);
      this.DEFAULT_COMM_THREAD = commThread;
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

}
