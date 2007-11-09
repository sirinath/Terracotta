/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NIOWorkarounds;
import com.tc.util.Assert;
import com.tc.util.Util;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

  private final int             MAX_WORKER_COMM        = 16;
  private boolean               workerCommStarted      = false;
  private boolean               requestWorkerCommStop  = false;
  String                        workerCommName         = "TCWorkerComm # ";

  private TCCommJDK14           parentComm;
  private TCWorkerComm[]        workerCommThreads;
  private static final TCLogger logger                 = TCLogging.getLogger(TCWorkerCommManager.class);

  // Some constants
  public final short            INVALID_WORKER_COMM_ID = -1;

  // Reason for retrieving worker comm info
  public final short            MODIFY_INTEREST_SELF   = 0x01;
  public final short            UNREGISTER_CHANNEL     = 0x02;
  public final short            CLEANUP_CHANNEL        = 0x04;
  public final short            SOME_OTHER_REASON2     = 0x08;

  // Worker comm status
  private final short           WORKER_ACTIVE          = 0x10;
  private final short           WORKER_DEAD            = 0x20;
  private final short           WORKER_IDLE            = 0x40;
  private final short           WORKER_STATUS1         = 0x80;

  TCWorkerCommManager(TCCommJDK14 comm, int workerCommCount) {

    if (workerCommCount <= 0) {
      workerCommStarted = false;
      return;
    }

    if (workerCommCount > MAX_WORKER_COMM) {
      logger.error("Max allowed TC Worker Comm Thread count is " + MAX_WORKER_COMM);
    }

    this.nextWorkerCommId = INVALID_WORKER_COMM_ID;
    this.totalWorkerComm = 0;
    this.parentComm = comm;

    workerCommThreads = new TCWorkerComm[workerCommCount];
    totalWorkerComm = workerCommCount;
    workerCommChannelMap = new HashMap(); // only for cleanup purposes
  }

  public synchronized int getNextFreeWorkerComm() {
    int iter = 0;
    do {
      nextWorkerCommId++;
      nextWorkerCommId = nextWorkerCommId % totalWorkerComm;

      iter += 1;
      if (iter >= 2 * totalWorkerComm) return INVALID_WORKER_COMM_ID;
    } while (workerCommThreads[nextWorkerCommId].status == WORKER_DEAD);
    return nextWorkerCommId;
  }

  // ???
  private int getTotalWorkerComms() {
    return totalWorkerComm;
  }

  public void start() {

    // don't start the threads if they don't need it .. eg: L1 clients
    if ((parentComm == null) || (workerCommThreads.length == 0)) {
      workerCommStarted = false;
      return;
    }

    workerCommStarted = true;

    for (int i = 0; i < workerCommThreads.length; i++) {
      workerCommThreads[i] = new TCWorkerComm(i);
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
    if (workerCommStarted == true) {

      requestWorkerCommStop = true;

      for (int i = 0; i < totalWorkerComm; i++) {
        if (workerCommThreads[i].status == WORKER_ACTIVE || workerCommThreads[i].status == WORKER_IDLE) {
          Assert.eval(requestWorkerCommStop == true);
          try {
            workerCommThreads[i].workerSelector.wakeup();
          } catch (Exception e) {
            logger.error("Exception trying to stop TC worker comm" + e);
          }
        }
      }

    }
  }

  private boolean isStopped() {
    return requestWorkerCommStop;
  }

  void selectLoop(Selector localSelector, LinkedQueue localSelectorTasks) throws IOException {
    Assert.assertNotNull("selector", localSelector);
    Assert.eval("Not started", isStarted());

    while (true) {
      final int numKeys;
      try {
        numKeys = localSelector.select();
      } catch (IOException ioe) {
        if (NIOWorkarounds.linuxSelectWorkaround(ioe)) {
          logger.warn("working around Sun bug 4504001");
          continue;
        }
        throw ioe;
      }

      if (isStopped()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Select loop terminating");
        }
        return;
      }

      boolean isInterrupted = false;
      // run any pending selector tasks
      while (true) {
        Runnable task = null;
        while (true) {
          try {
            task = (Runnable) localSelectorTasks.poll(0);
            break;
          } catch (InterruptedException ie) {
            logger.error("Error getting task from task queue", ie);
            isInterrupted = true;
          }
        }

        if (null == task) {
          break;
        }

        try {
          task.run();
        } catch (Exception e) {
          logger.error("error running selector task", e);
        }
      }
      Util.selfInterruptIfNeeded(isInterrupted);

      final Set selectedKeys = localSelector.selectedKeys();
      if ((0 == numKeys) && (0 == selectedKeys.size())) {
        continue;
      }

      for (Iterator iter = selectedKeys.iterator(); iter.hasNext();) {
        SelectionKey key = (SelectionKey) iter.next();
        iter.remove();

        if (null == key) {
          logger.error("Selection key is null");
          continue;
        }

        try {

          if (key.isAcceptable()) {
            // workerComm doesn't handle OP_ACCEPT
            Assert.eval(false);
          }

          if (key.isConnectable()) {
            // workerComm doesn't handle OP_ACCEPT
            Assert.eval(false);
          }
          
          if (key.isReadable()) {
            ((TCJDK14ChannelReader) key.attachment()).doRead((ScatteringByteChannel) key.channel());
          }

          if (key.isValid() && key.isWritable()) {
            ((TCJDK14ChannelWriter) key.attachment()).doWrite((GatheringByteChannel) key.channel());
          }

        } catch (CancelledKeyException cke) {
          logger.warn(cke.getClass().getName() + " occured");
        }
      } // for
    } // while (true)
  }

  public void addSelectorTask(Runnable task, int workerCommId) {

    Assert.eval(!isWorkerCommThread(Thread.currentThread()));
    Assert.eval(workerCommId >= 0);

    boolean isInterrupted = false;

    try {
      while (true) {
        try {
          workerCommThreads[workerCommId].workerSelectorTasks.put(task);
          break;
        } catch (InterruptedException e) {
          logger.warn(e);
          isInterrupted = true;
        }
      }
    } finally {
      workerCommThreads[workerCommId].workerSelector.wakeup();
    }
    Util.selfInterruptIfNeeded(isInterrupted);

  }

  public Selector getWorkerSelector(int reason) {
    Assert.eval(isWorkerCommThread(Thread.currentThread()));
    return getWorkerSelector(((TCWorkerComm) Thread.currentThread()).getWorkerCommId(), reason);
  }

  public Selector getWorkerSelector(int workerCommId, int reason) {
    return workerCommThreads[workerCommId].workerSelector;
    // reson is for logging
  }

  public boolean isWorkerCommThread(Thread currentThread) {

    if (currentThread == null) { return false; }
    if (!isStarted()) { return false; }

    for (int i = 0; i < workerCommThreads.length; i++) {
      if (workerCommThreads[i] == currentThread) { return true; }
    }

    return false;
  }

  public int getWorkerCommForSocketChannel(Channel ch) {
    Object tmp = null;
    tmp = workerCommChannelMap.get(ch);
    if (tmp != null) {
      return ((Integer) (tmp)).intValue();
    } else {
      return INVALID_WORKER_COMM_ID;
    }
  }

  public void setWorkerCommChannelMap(SocketChannel sc, int commId) {
    Assert.eval(sc != null);
    Assert.eval(commId != INVALID_WORKER_COMM_ID);
    workerCommChannelMap.put(sc, new Integer(commId));
  }

  /* The Real Worker */
  class TCWorkerComm extends Thread {
    private int         status;
    private int         workerCommId;
    private Selector    workerSelector;
    private LinkedQueue workerSelectorTasks = new LinkedQueue();

    private void setWorkerCommId(int workerId) {
      workerCommId = workerId;
    }

    public TCWorkerComm(int workerId) {

      setWorkerCommId(workerId);
      setDaemon(true);
      setName(workerCommName + workerId);
      status = WORKER_IDLE;

      workerSelector = parentComm.createSelector();

      if (logger.isDebugEnabled()) {
        logger.debug("Created TCWorkerComm thread #" + workerCommId);
      }
    }

    public int getWorkerCommId() {
      return workerCommId;
    }

    public void run() {
      try {
        status = WORKER_ACTIVE;
        selectLoop(workerSelector, workerSelectorTasks);
      } catch (Throwable t) {
        t.printStackTrace();
      } finally {
        // workerCommStarted = false; No ... only after the closure of all worker threads.
        status = WORKER_DEAD; // probably i need to move this to someother meaningful place.
        parentComm.dispose(workerSelector, workerSelectorTasks, this.workerCommId);
      }
    }
  } // end of TCWorkerComm - The real worker

}