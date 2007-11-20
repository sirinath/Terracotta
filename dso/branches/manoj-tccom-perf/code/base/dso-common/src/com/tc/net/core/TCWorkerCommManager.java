/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * The whole intention of this class is to manage the workerThreads for each Listener. While creating a ... some docs
 * here.
 * 
 * @author Manoj G
 */
public class TCWorkerCommManager {
  private int                   totalWorkerComm;
  private int                   nextWorkerCommId;
  private Map                   workerCommChannelMap;

  private boolean               workerCommStarted      = false;
  String                        workerCommName         = "TCWorkerComm # ";

  private TCCommJDK14           parentComm;
  private CoreNIOServices[]     workerCommThreads;
  private static final TCLogger logger                 = TCLogging.getLogger(TCWorkerCommManager.class);

  public final short            INVALID_WORKER_COMM_ID = -1;

  TCWorkerCommManager(TCCommJDK14 comm, int workerCommCount) {

    if (workerCommCount <= 0) {
      workerCommStarted = false;
      return;
    }

    logger.info("Creating " + workerCommCount + " worker comm threads.");

    this.nextWorkerCommId = INVALID_WORKER_COMM_ID;
    this.totalWorkerComm = 0;
    this.parentComm = comm;

    // workerCommThreads = new TCWorkerComm[workerCommCount];
    workerCommThreads = new CoreNIOServices[workerCommCount];
    totalWorkerComm = workerCommCount;
    workerCommChannelMap = new HashMap(); // only for cleanup purposes
  }

  public synchronized CoreNIOServices getNextFreeWorkerComm() {
    int iter = 0;
    do {
      nextWorkerCommId++;
      nextWorkerCommId = nextWorkerCommId % totalWorkerComm;

      iter += 1;
      if (iter >= 2 * totalWorkerComm) return null;
    } while (workerCommThreads[nextWorkerCommId].isStarted() != true);
    return workerCommThreads[nextWorkerCommId];
  }

  public void start() {

    // don't start the threads if they don't need it .. eg: L1 clients
    if ((parentComm == null) || (workerCommThreads.length == 0)) {
      workerCommStarted = false;
      return;
    }

    workerCommStarted = true;

    for (int i = 0; i < workerCommThreads.length; i++) {
      workerCommThreads[i] = new CoreNIOServices(workerCommName + i, this);
      workerCommThreads[i].start();
    }
  }

  public boolean isStarted() {
    if (workerCommStarted == true) {
      Assert.eval(totalWorkerComm > 0);
    }
    return workerCommStarted;
  }

  public void stop() {
    if (isStarted()) {
      for (int i = 0; i < totalWorkerComm; i++) {
        workerCommThreads[i].requestStop();
      }
    }
  }

  public CoreNIOServices getWorkerCommForSocketChannel(Channel ch) {
    return (CoreNIOServices) (workerCommChannelMap.get(ch));
  }

  public void setWorkerCommChannelMap(SocketChannel sc, CoreNIOServices nioServiceThread) {
    Assert.eval(sc != null);
    Assert.eval(nioServiceThread != null);
    workerCommChannelMap.put(sc, nioServiceThread);
  }

}
