/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

/**
 * JDK 1.4 (NIO) version of TCComm. Uses a single internal thread and a selector to manage channels associated with
 * <code>TCConnection</code>'s
 * 
 * @author teck
 */
class TCCommJDK14 implements TCComm {

  protected static final TCLogger   logger         = TCLogging.getLogger(TCComm.class);

  private final TCWorkerCommManager workerCommMgr;
  private final CoreNIOServices     commThread;
  private final String              commThreadName = "TCComm Main Selector Thread";

  private volatile boolean          started        = false;

  TCCommJDK14(String name, int workerCommCount, SocketParams socketParams) {
    if (workerCommCount > 0) {
      workerCommMgr = new TCWorkerCommManager(name, workerCommCount, socketParams);
    } else {
      logger.info("Comm Worker Threads NOT requested");
      workerCommMgr = null;
    }
    
    this.commThread = new CoreNIOServices(name + ":" + commThreadName, workerCommMgr, socketParams);
  }

  protected int getWeightForWorkerComm(int workerCommId) {
    if (workerCommMgr != null) { return workerCommMgr.getWeightForWorkerComm(workerCommId); }
    return 0;
  }

  protected CoreNIOServices getWorkerComm(int workerCommId) {
    if (workerCommMgr != null) { return workerCommMgr.getWorkerComm(workerCommId); }
    return null;
  }

  protected long getTotalbytesReadByWorkerComm(int workerCommId) {
    if (workerCommMgr != null) { return workerCommMgr.getTotalBytesReadByWorkerComm(workerCommId); }
    return 0;
  }

  protected long getTotalbytesWrittenByWorkerComm(int workerCommId) {
    if (workerCommMgr != null) { return workerCommMgr.getTotalBytesWrittenByWorkerComm(workerCommId); }
    return 0;
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
      if (workerCommMgr != null) {
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
