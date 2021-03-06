/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for ConnectionManagers
 *
 * @author teck
 */
abstract class AbstractTCConnectionManager implements TCConnectionManager {

  AbstractTCConnectionManager(TCComm comm) {
    privateComm = (comm == null);

    if (privateComm) {
      this.comm = (AbstractTCComm) new TCCommFactory().getInstance(true);
    } else {
      this.comm = (AbstractTCComm) comm;
    }

    this.connEvents = new ConnectionEvents();
    this.listenerEvents = new ListenerEvents();
  }

  public TCConnection[] getAllConnections() {
    synchronized (connections) {
      return (TCConnection[]) connections.toArray(EMPTY_CONNECTION_ARRAY);
    }
  }

  public TCListener[] getAllListeners() {
    synchronized (listeners) {
      return (TCListener[]) listeners.toArray(EMPTY_LISTENER_ARRAY);
    }
  }

  public final synchronized TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory)
      throws IOException {
    return createListener(addr, factory, Constants.DEFAULT_ACCEPT_QUEUE_DEPTH, true);
  }

  public final synchronized TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory,
                                                      int backlog, boolean reuseAddr) throws IOException {
    checkShutdown();

    TCListener rv = createListenerImpl(addr, factory, backlog, reuseAddr);
    rv.addEventListener(listenerEvents);
    rv.addEventListener(comm);
    comm.listenerAdded(rv);

    synchronized (listeners) {
      listeners.add(rv);
    }

    return rv;
  }

  public final synchronized TCConnection createConnection(TCProtocolAdaptor adaptor) {
    checkShutdown();

    TCConnection rv = createConnectionImpl(adaptor, connEvents);
    newConnection(rv);

    return rv;
  }

  public synchronized void closeAllConnections(long timeout) {
    closeAllConnections(false, timeout);
  }

  public synchronized void asynchCloseAllConnections() {
    closeAllConnections(true, 0);
  }

  private void closeAllConnections(boolean async, long timeout) {
    TCConnection[] conns;

    synchronized (connections) {
      conns = (TCConnection[]) connections.toArray(EMPTY_CONNECTION_ARRAY);
    }

    for (int i = 0; i < conns.length; i++) {
      TCConnection conn = conns[i];

      try {
        if (async) {
          conn.asynchClose();
        } else {
          conn.close(timeout);
        }
      } catch (Exception e) {
        logger.error("Exception trying to close " + conn, e);
      }
    }
  }

  public synchronized void closeAllListeners() {
    TCListener[] list;

    synchronized (listeners) {
      list = (TCListener[]) listeners.toArray(EMPTY_LISTENER_ARRAY);
    }

    for (int i = 0; i < list.length; i++) {
      TCListener lsnr = list[i];

      try {
        lsnr.stop();
      } catch (Exception e) {
        logger.error("Exception trying to close " + lsnr, e);
      }
    }
  }

  public final synchronized void shutdown() {
    if (shutdown.attemptSet()) {
      closeAllListeners();
      asynchCloseAllConnections();

      if (privateComm) {
        comm.stop();
      }
    }
  }

  void connectionClosed(TCConnection conn) {
    synchronized (connections) {
      connections.remove(conn);
    }
  }

  void newConnection(TCConnection conn) {
    synchronized (connections) {
      connections.add(conn);
    }
  }

  void removeConnection(AbstractTCConnection connection) {
    synchronized (connections) {
      connections.remove(connection);
    }
  }

  protected TCConnectionEventListener getConnectionListener() {
    return connEvents;
  }

  protected abstract TCListener createListenerImpl(TCSocketAddress addr, ProtocolAdaptorFactory factory, int backlog,
                                                   boolean reuseAddr) throws IOException;

  protected abstract TCConnection createConnectionImpl(TCProtocolAdaptor adaptor, TCConnectionEventListener listener);

  private final void checkShutdown() {
    if (shutdown.isSet()) { throw new IllegalStateException("connection manager shutdown"); }
  }

  private class ConnectionEvents implements TCConnectionEventListener {
    public final void connectEvent(TCConnectionEvent event) {
      if (logger.isDebugEnabled()) {
        logger.debug("connect event: " + event.toString());
      }
    }

    public final void closeEvent(TCConnectionEvent event) {
      if (logger.isDebugEnabled()) {
        logger.debug("close event: " + event.toString());
      }
    }

    public final void errorEvent(TCConnectionErrorEvent event) {
      try {
        final Throwable err = event.getException();

        if (err != null) {
          if (err instanceof IOException) {
            if (logger.isInfoEnabled()) {
              logger.info("error event on connection " + event.getSource() + ": " + err.getMessage());
            }
          } else {
            logger.error(err);
          }
        }
      } finally {
        event.getSource().asynchClose();
      }
    }

    public final void endOfFileEvent(TCConnectionEvent event) {
      if (logger.isDebugEnabled()) {
        logger.debug("EOF event: " + event.toString());
      }

      event.getSource().asynchClose();
    }
  }

  private class ListenerEvents implements TCListenerEventListener {
    public void closeEvent(TCListenerEvent event) {
      synchronized (listeners) {
        listeners.remove(event.getSource());
      }
    }
  }

  protected static final TCConnection[] EMPTY_CONNECTION_ARRAY = new TCConnection[] {};
  protected static final TCListener[]   EMPTY_LISTENER_ARRAY   = new TCListener[] {};

  protected static final TCLogger       logger                 = TCLogging.getLogger(TCConnectionManager.class);

  protected final AbstractTCComm        comm;

  private final Set                     connections            = new HashSet();
  private final Set                     listeners              = new HashSet();
  private final SetOnceFlag             shutdown               = new SetOnceFlag();
  private final boolean                 privateComm;
  private final ConnectionEvents        connEvents;
  private final ListenerEvents          listenerEvents;

}