/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCInternalError;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NIOWorkarounds;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;
import com.tc.util.Assert;
import com.tc.util.Util;
import com.tc.util.runtime.Os;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * JDK 1.4 (NIO) version of TCComm. Uses a single internal thread and a selector to manage channels associated with
 * <code>TCConnection</code>'s
 * 
 * @author teck
 */
class TCCommJDK14 implements TCComm, TCListenerEventListener {

  private TCCommThread            commThread                    = null;
  private TCWorkerCommManager     workerCommMgr                 = null;
  private volatile boolean        started                       = false;
  protected static final TCLogger logger                        = TCLogging.getLogger(TCComm.class);

  final int                       MODIFY_INTEREST_COMM_THREAD   = 0;
  final int                       MODIFY_INTEREST_WORKER_THREAD = 1;

  public TCCommJDK14() {
    // no worker threads for you ...
  }

  TCCommJDK14(int workerCommCount) {
    workerCommMgr = new TCWorkerCommManager(this, workerCommCount);
  }

  protected Selector createSelector() {
    Selector selector1 = null;

    final int tries = 3;

    for (int i = 0; i < tries; i++) {
      try {
        selector1 = Selector.open();
        return selector1;
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      } catch (NullPointerException npe) {
        if (i < tries && NIOWorkarounds.selectorOpenRace(npe)) {
          System.err.println("Attempting to work around sun bug 6427854 (attempt " + (i + 1) + " of " + tries + ")");
          try {
            Thread.sleep(new Random().nextInt(20) + 5);
          } catch (InterruptedException ie) {
            //
          }
          continue;
        }
        throw npe;
      }
    }

    return selector1;
  }

  protected void stopImpl() {
    try {
      if (commThread.selector != null) {
        commThread.selector.wakeup();
      }
    } catch (Exception e) {
      logger.error("Exception trying to stop TCComm", e);
    }
  }

  void addSelectorTask(Runnable task) {
    this.addSelectorTask(task, workerCommMgr.INVALID_WORKER_COMM_ID);
  }

  void addSelectorTask(final Runnable task, int workerCommId) {
    boolean isInterrupted = false;

    if (workerCommId > workerCommMgr.INVALID_WORKER_COMM_ID) {
      // this request is for one the worker comm threads
      workerCommMgr.addSelectorTask(task, workerCommId);
    } else {
      Assert.eval(!isCommThread());
      try {
        while (true) {
          try {
            commThread.selectorTasks.put(task);
            break;
          } catch (InterruptedException e) {
            logger.warn(e);
            isInterrupted = true;
          }
        }
      } finally {
        commThread.selector.wakeup();
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  void stopListener(final ServerSocketChannel ssc, final Runnable callback) {
    if (!isCommThread()) {
      Runnable task = new Runnable() {
        public void run() {
          TCCommJDK14.this.stopListener(ssc, callback);
        }
      };
      addSelectorTask(task);
      return;
    }

    try {
      cleanupChannel(ssc, null);
    } catch (Exception e) {
      logger.error(e);
    } finally {
      try {
        callback.run();
      } catch (Exception e) {
        logger.error(e);
      }
    }
  }

  void unregister(SelectableChannel channel) {
    SelectionKey key = null;
    if (isCommThread()) {
      key = channel.keyFor(commThread.selector);
    } else if (isWorkerCommStarted()) {
      key = channel.keyFor(workerCommMgr.getWorkerSelector(workerCommMgr.UNREGISTER_CHANNEL));
    } else {
      // this shdn't happen
      Assert.eval(false);
    }

    if (key != null) {
      key.cancel();
      key.attach(null);
    }
  }

  void cleanupChannel(final Channel ch, final Runnable callback) {
    this.cleanupChannel(ch, callback, workerCommMgr.INVALID_WORKER_COMM_ID);
  }

  void cleanupChannel(final Channel ch, final Runnable callback, int workerCommId) {
    Selector localSelector = null;
    boolean forWorkerComm = false;

    if (null == ch) {
      // not expected
      logger.warn("null channel passed to cleanupChannel()", new Throwable());
      return;
    }

    if (isWorkerCommThread()) {
      if (workerCommId == workerCommMgr.INVALID_WORKER_COMM_ID) {
        workerCommId = workerCommMgr.getWorkerCommForSocketChannel(ch);
      }
      localSelector = workerCommMgr.getWorkerSelector(workerCommId, workerCommMgr.CLEANUP_CHANNEL);
      forWorkerComm = true;
    } else if (isWorkerCommStarted()) {
      final int commId = workerCommMgr.getWorkerCommForSocketChannel(ch);
      if (commId != workerCommMgr.INVALID_WORKER_COMM_ID) {
        addSelectorTask(new Runnable() {
          public void run() {
            TCCommJDK14.this.cleanupChannel(ch, callback, commId);
          }
        }, commId);
        return;
      } else {
        // probably for comm thread
      }
    } else {
      Assert.eval(workerCommId == workerCommMgr.INVALID_WORKER_COMM_ID);
    }

    if (!forWorkerComm) {
      if (!isCommThread()) {
        Assert.eval(workerCommId == workerCommMgr.INVALID_WORKER_COMM_ID);
        if (logger.isDebugEnabled()) {
          logger.debug("queue'ing channel close operation");
        }

        final int commId = workerCommId;

        addSelectorTask(new Runnable() {
          public void run() {
            TCCommJDK14.this.cleanupChannel(ch, callback, commId);
          }
        });
        return;
      } else {
        Assert.eval(isCommThread());
        localSelector = commThread.selector;
      }
    }

    try {
      if (ch instanceof SelectableChannel) {
        SelectableChannel sc = (SelectableChannel) ch;

        try {
          SelectionKey sk = sc.keyFor(localSelector);
          if (sk != null) {
            sk.attach(null);
            sk.cancel();
          }
        } catch (Exception e) {
          logger.warn("Exception trying to clear selection key", e);
        }
      }

      if (ch instanceof SocketChannel) {
        SocketChannel sc = (SocketChannel) ch;

        Socket s = sc.socket();

        if (null != s) {
          synchronized (s) {

            if (s.isConnected()) {
              try {
                if (!s.isOutputShutdown()) {
                  s.shutdownOutput();
                }
              } catch (Exception e) {
                logger.warn("Exception trying to shutdown socket output: " + e.getMessage());
              }

              try {
                if (!s.isClosed()) {
                  s.close();
                }
              } catch (Exception e) {
                logger.warn("Exception trying to close() socket: " + e.getMessage());
              }
            }
          }
        }
      } else if (ch instanceof ServerSocketChannel) {
        ServerSocketChannel ssc = (ServerSocketChannel) ch;

        try {
          ssc.close();
        } catch (Exception e) {
          logger.warn("Exception trying to close() server socket" + e.getMessage());
        }
      }

      try {
        ch.close();
      } catch (Exception e) {
        logger.warn("Exception trying to close channel", e);
      }
    } catch (Exception e) {
      // this is just a catch all to make sure that no exceptions will be thrown by this method, please do not remove
      logger.error("Unhandled exception in cleanupChannel()", e);
    } finally {
      try {
        if (callback != null) {
          callback.run();
        }
      } catch (Throwable t) {
        logger.error("Unhandled exception in cleanupChannel callback.", t);
      }
    }

  }

  void requestConnectInterest(TCConnectionJDK14 conn, SocketChannel sc) {
    handleRequest(InterestRequest.createSetInterestRequest(sc, conn, SelectionKey.OP_CONNECT));
  }

  void requestReadInterest(TCJDK14ChannelReader reader, ScatteringByteChannel channel) {
    handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, reader, SelectionKey.OP_READ));
  }

  void requestWriteInterest(final TCJDK14ChannelWriter writer, final GatheringByteChannel channel, int workerCommId) {
    handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, writer, SelectionKey.OP_WRITE,
                                                           workerCommId));
  }

  void requestAcceptInterest(TCListenerJDK14 lsnr, ServerSocketChannel ssc) {
    handleRequest(InterestRequest.createSetInterestRequest(ssc, lsnr, SelectionKey.OP_ACCEPT));
  }

  void removeWriteInterest(TCConnectionJDK14 conn, SelectableChannel channel, int workerCommId) {
    handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_WRITE, workerCommId));
  }

  void removeReadInterest(TCConnectionJDK14 conn, SelectableChannel channel, int workerCommId) {
    handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_READ));
  }

  public void closeEvent(TCListenerEvent event) {
    commThread.listenerAdded(event.getSource());
  }

  void listenerAdded(TCListener listener) {
    commThread.listenerAdded(listener);
  }

  private void handleRequest(final InterestRequest req) {
    // ignore the request if we are stopped/stopping
    if (isStopped()) { return; }

    if (isCommThread()) {
      modifyInterest(req, MODIFY_INTEREST_COMM_THREAD);
    } else if (isWorkerCommThread()) {
      Assert.eval(req.workerCommId > workerCommMgr.INVALID_WORKER_COMM_ID);
      modifyInterest(req, MODIFY_INTEREST_WORKER_THREAD);
    } else {
      if (req.workerCommId > workerCommMgr.INVALID_WORKER_COMM_ID) {
        Assert.eval(isWorkerCommStarted());
        addSelectorTask(new Runnable() {
          public void run() {
            TCCommJDK14.this.handleRequest(req);
          }
        }, req.workerCommId);
      } else {
        addSelectorTask(new Runnable() {
          public void run() {
            TCCommJDK14.this.handleRequest(req);
          }
        });
      }
    }
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
            doAccept(key);
            continue;
          }

          if (key.isConnectable()) {
            doConnect(key);
            continue;
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

  void dispose(Selector localSelector, LinkedQueue localSelectorTasks) {
    dispose(localSelector, localSelectorTasks, workerCommMgr.INVALID_WORKER_COMM_ID);
  }

  void dispose(Selector localSelector, LinkedQueue localSelectorTasks, int workerCommId) {
    if (localSelector != null) {

      if (workerCommId > workerCommMgr.INVALID_WORKER_COMM_ID) {
        Assert.eval(isWorkerCommThread());
      }

      for (Iterator keys = localSelector.keys().iterator(); keys.hasNext();) {
        try {
          SelectionKey key = (SelectionKey) keys.next();
          cleanupChannel(key.channel(), null, workerCommId);
        }

        catch (Exception e) {
          logger.warn("Exception trying to close channel", e);
        }
      }

      try {
        localSelector.close();
      } catch (Exception e) {
        if ((Os.isMac()) && (Os.isUnix()) && (e.getMessage().equals("Bad file descriptor"))) {
          // I can't find a specific bug about this, but I also can't seem to prevent the exception on the Mac.
          // So just logging this as warning.
          logger.warn("Exception trying to close selector: " + e.getMessage());
        } else {
          logger.error("Exception trying to close selector", e);
        }
      }
    }

    // drop any old selector tasks
    localSelectorTasks = new LinkedQueue();
  }

  private boolean isCommThread() {
    return isCommThread(Thread.currentThread());
  }

  private boolean isWorkerCommThread() {
    return workerCommMgr.isWorkerCommThread(Thread.currentThread());
  }

  private boolean isWorkerCommStarted() {
    return workerCommMgr.isStarted();
  }

  private boolean isCommThread(Thread thread) {
    if (thread == null) { return false; }
    return thread == commThread;
  }

  private void doConnect(SelectionKey key) {
    SocketChannel sc = (SocketChannel) key.channel();
    TCConnectionJDK14 conn = (TCConnectionJDK14) key.attachment();

    try {
      if (sc.finishConnect()) {
        sc.register(commThread.selector, SelectionKey.OP_READ, conn);
        conn.finishConnect();
      } else {
        String errMsg = "finishConnect() returned false, but no exception thrown";

        if (logger.isInfoEnabled()) {
          logger.info(errMsg);
        }

        conn.fireErrorEvent(new Exception(errMsg), null);
      }
    } catch (IOException ioe) {
      if (logger.isInfoEnabled()) {
        logger.info("IOException attempting to finish socket connection", ioe);
      }

      conn.fireErrorEvent(ioe, null);
    }
  }

  private void modifyInterest(InterestRequest request, int type) {

    Selector localSelector = null;
    if (type == MODIFY_INTEREST_COMM_THREAD) {
      Assert.eval(request.workerCommId == workerCommMgr.INVALID_WORKER_COMM_ID);
      localSelector = commThread.selector;
    } else if (type == MODIFY_INTEREST_WORKER_THREAD) {
      Assert.eval(request.workerCommId > workerCommMgr.INVALID_WORKER_COMM_ID);
      localSelector = workerCommMgr.getWorkerSelector(request.workerCommId, workerCommMgr.MODIFY_INTEREST_SELF);
      Assert.eval(localSelector != null);
    } else {
      // this shd not happen
      Assert.eval(false);
    }

    try {
      final int existingOps;

      SelectionKey key = request.channel.keyFor(localSelector);
      if (key != null) {
        existingOps = key.interestOps();
      } else {
        existingOps = 0;
      }

      if (logger.isDebugEnabled()) {
        logger.debug(request);
      }

      if (request.add) {
        request.channel.register(localSelector, existingOps | request.interestOps, request.attachment);
      } else if (request.set) {
        request.channel.register(localSelector, request.interestOps, request.attachment);
      } else if (request.remove) {
        request.channel.register(localSelector, existingOps ^ request.interestOps, request.attachment);
      } else {
        throw new TCInternalError();
      }
    } catch (ClosedChannelException cce) {
      logger.warn("Exception trying to process interest request: " + cce);

    } catch (CancelledKeyException cke) {
      logger.warn("Exception trying to process interest request: " + cke);
    }
  }

  private void doAccept(final SelectionKey key) {
    Assert.eval(isCommThread());

    SocketChannel sc = null;

    final TCListenerJDK14 lsnr = (TCListenerJDK14) key.attachment();

    try {
      final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
      sc = ssc.accept();
      sc.configureBlocking(false);
      final Socket s = sc.socket();

      try {
        s.setSendBufferSize(64 * 1024);
      } catch (IOException ioe) {
        logger.warn("IOException trying to setSendBufferSize()");
      }

      try {
        s.setTcpNoDelay(true);
      } catch (IOException ioe) {
        logger.warn("IOException trying to setTcpNoDelay()", ioe);
      }

      if (isWorkerCommStarted()) {
        // Multi threaded server model
        final int commId = workerCommMgr.getNextFreeWorkerComm();
        Assert.eval(commId > workerCommMgr.INVALID_WORKER_COMM_ID);

        final TCConnectionJDK14 conn = lsnr.createConnection(sc, commId);
        final SocketChannel sc1 = sc;

        workerCommMgr.setWorkerCommChannelMap(sc, commId);
        addSelectorTask(new Runnable() {
          InterestRequest workerReq = InterestRequest
                                        .createAddInterestRequest(sc1, conn,
                                                                  (SelectionKey.OP_READ | SelectionKey.OP_WRITE),
                                                                  commId);

          public void run() {
            TCCommJDK14.this.handleRequest(workerReq);
          }
        }, commId);
      } else {
        final TCConnectionJDK14 conn = lsnr.createConnection(sc);
        // Single threaded server model
        sc.register(((TCCommThread) Thread.currentThread()).selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                    conn);
      }
    } catch (IOException ioe) {
      if (logger.isInfoEnabled()) {
        logger.info("IO Exception accepting new connection", ioe);
      }

      cleanupChannel(sc, null);
    }
  }

  public final boolean isStarted() {
    return started;
  }

  public final boolean isStopped() {
    return !started;
  }

  public final synchronized void start() {
    Selector selector;
    if (!started) {
      started = true;
      if (logger.isDebugEnabled()) {
        logger.debug("Start requested");
      }

      // The Main Listener Selector
      selector = createSelector();
      if (selector == null) { throw new RuntimeException("Could not start selector"); }

      commThread = new TCCommThread(this, selector);
      commThread.start();

      // The worker comm threads
      if (workerCommMgr != null) {
        workerCommMgr.start();
      }
    }
  }

  public final synchronized void stop() {
    if (started) {
      started = false;
      if (logger.isDebugEnabled()) {
        logger.debug("Stop requested");
      }
      stopImpl();
      workerCommMgr.stop();
    }
  }

  private static class InterestRequest {
    final SelectableChannel channel;
    final Object            attachment;
    final boolean           set;
    final boolean           add;
    final boolean           remove;
    final int               interestOps;
    int                     workerCommId = -1;

    static InterestRequest createAddInterestRequest(SelectableChannel channel, Object attachment, int interestOps) {
      return new InterestRequest(channel, attachment, interestOps, false, true, false, -1);
    }

    static InterestRequest createAddInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                    int workerCommId) {
      return new InterestRequest(channel, attachment, interestOps, false, true, false, workerCommId);
    }

    static InterestRequest createSetInterestRequest(SelectableChannel channel, Object attachment, int interestOps) {
      return new InterestRequest(channel, attachment, interestOps, true, false, false, -1);
    }

    static InterestRequest createSetInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                    int workerCommId) {
      return new InterestRequest(channel, attachment, interestOps, true, false, false, workerCommId);
    }

    static InterestRequest createRemoveInterestRequest(SelectableChannel channel, Object attachment, int interestOps) {
      return new InterestRequest(channel, attachment, interestOps, false, false, true, -1);
    }

    static InterestRequest createRemoveInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                       int workerCommId) {
      return new InterestRequest(channel, attachment, interestOps, false, false, true, workerCommId);
    }

    private InterestRequest(SelectableChannel channel, Object attachment, int interestOps, boolean set, boolean add,
                            boolean remove, int commId) {
      Assert.eval(remove ^ set ^ add);
      Assert.eval(channel != null);

      this.channel = channel;
      this.attachment = attachment;
      this.set = set;
      this.add = add;
      this.remove = remove;
      this.interestOps = interestOps;

      // the best hack... sorry
      this.workerCommId = commId;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();

      buf.append("Interest modify request: ").append(channel.toString()).append("\n");
      buf.append("Ops: ").append(Constants.interestOpsToString(interestOps)).append("\n");
      buf.append("Set: ").append(set).append(", Remove: ").append(remove).append(", Add: ").append(add).append("\n");
      buf.append("Attachment: ");

      if (attachment != null) {
        buf.append(attachment.toString());
      } else {
        buf.append("null");
      }

      buf.append("\n");

      return buf.toString();
    }

  }

  // Little helper class to drive the selector. The main point of this class
  // is to isolate the try/finally block around the entire selection process
  private static class TCCommThread extends Thread {
    final TCCommJDK14  commInstance;
    final Selector     selector;
    final LinkedQueue  selectorTasks = new LinkedQueue();
    final Set          listeners     = new HashSet();
    final int          number        = getNextCounter();
    final String       baseName      = "TCComm Selector Thread " + number;

    private static int counter       = 1;

    private static synchronized int getNextCounter() {
      return counter++;
    }

    TCCommThread(TCCommJDK14 comm, Selector selector) {
      this.selector = selector;
      commInstance = comm;
      setDaemon(true);
      setName(baseName);

      if (logger.isDebugEnabled()) {
        logger.debug("Creating a new selector thread (" + toString() + ")", new Throwable());
      }
    }

    String makeListenString(TCListener listener) {
      StringBuffer buf = new StringBuffer();
      buf.append("(listen ");
      buf.append(listener.getBindAddress().getHostAddress());
      buf.append(':');
      buf.append(listener.getBindPort());
      buf.append(')');
      return buf.toString();
    }

    synchronized void listenerRemoved(TCListener listener) {
      listeners.remove(makeListenString(listener));
      updateThreadName();
    }

    synchronized void listenerAdded(TCListener listener) {
      listeners.add(makeListenString(listener));
      updateThreadName();
    }

    private void updateThreadName() {
      StringBuffer buf = new StringBuffer(baseName);
      for (final Iterator iter = listeners.iterator(); iter.hasNext();) {
        buf.append(' ');
        buf.append(iter.next());
      }

      setName(buf.toString());
    }

    public void run() {
      try {
        commInstance.selectLoop(selector, selectorTasks);
      } catch (Throwable t) {
        logger.error("Unhandled exception from selectLoop", t);
        t.printStackTrace();
      } finally {
        commInstance.dispose(selector, selectorTasks);
      }
    }
  }

}
