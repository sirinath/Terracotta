/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.logging.TCLogger;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressIterator;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.IOException;

/**
 * This guy establishes a connection to the server for the Client.
 */
public class ClientConnectionEstablisher {

  private static final long               CONNECT_RETRY_INTERVAL = 1000;

  private static final Object             RECONNECT              = new Object();
  private static final Object             QUIT                   = new Object();

  private final String                    desc;
  private final int                       maxReconnectTries;
  private final int                       timeout;
  private final ConnectionAddressProvider connAddressProvider;
  private final TCConnectionManager       connManager;

  private final SynchronizedBoolean       connecting             = new SynchronizedBoolean(false);

  private Thread                          connectionEstablisher;

  private NoExceptionLinkedQueue          reconnectRequest       = new NoExceptionLinkedQueue();

  public ClientConnectionEstablisher(TCConnectionManager connManager, ConnectionAddressProvider connAddressProvider,
                                     int maxReconnectTries, int timeout) {
    this.connManager = connManager;
    this.connAddressProvider = connAddressProvider;
    this.maxReconnectTries = maxReconnectTries;
    this.timeout = timeout;

    if (maxReconnectTries == 0) desc = "none";
    else if (maxReconnectTries < 0) desc = "unlimited";
    else desc = "" + maxReconnectTries;

  }

  /**
   * Blocking open. Causes a connection to be made. Will throw exceptions if the connect fails.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws TCTimeoutException
   * @throws MaxConnectionsExceededException
   */
  public TCConnection open(ClientMessageTransport cmt) throws TCTimeoutException, IOException {
    synchronized (connecting) {
      Assert.eval("Can't call open() concurrently", !connecting.get());
      connecting.set(true);

      try {
        return connectTryAllOnce(cmt);
      } finally {
        connecting.set(false);
      }
    }
  }

  private TCConnection connectTryAllOnce(ClientMessageTransport cmt) throws TCTimeoutException, IOException {
    final ConnectionAddressIterator addresses = connAddressProvider.getIterator();
    TCConnection rv = null;
    while (addresses.hasNext()) {
      final ConnectionInfo connInfo = addresses.next();
      try {
        final TCSocketAddress csa = new TCSocketAddress(connInfo);
        rv = connect(csa, cmt);
        break;
      } catch (TCTimeoutException e) {
        if (!addresses.hasNext()) { throw e; }
      } catch (IOException e) {
        if (!addresses.hasNext()) { throw e; }
      }
    }
    return rv;
  }

  /**
   * Tries to make a connection. This is a blocking call.
   * 
   * @return
   * @throws TCTimeoutException
   * @throws IOException
   * @throws MaxConnectionsExceededException
   */
  TCConnection connect(TCSocketAddress sa, ClientMessageTransport cmt) throws TCTimeoutException, IOException {

    TCConnection connection = this.connManager.createConnection(cmt.getProtocolAdapter());
    cmt.fireTransportConnectAttemptEvent();
    connection.connect(sa, timeout);
    return connection;
  }

  public String toString() {
    return "ClientConnectionEstablisher[" + connAddressProvider + ", timeout=" + timeout + "]";
  }

  private void reconnect(ClientMessageTransport cmt) throws MaxConnectionsExceededException {
    try {
      boolean connected = false;
      for (int i = 0; ((maxReconnectTries < 0) || (i < maxReconnectTries)) && !connected; i++) {
        ConnectionAddressIterator addresses = connAddressProvider.getIterator();
        while (addresses.hasNext() && !connected) {
          final ConnectionInfo connInfo = addresses.next();
          try {
            if (i % 20 == 0) {
              cmt.logger.warn("Reconnect attempt " + i + " of " + desc + " reconnect tries to " + connInfo
                              + ", timeout=" + timeout);
            }
            TCConnection connection = connect(new TCSocketAddress(connInfo), cmt);
            cmt.reconnect(connection);
            connected = true;
          } catch (MaxConnectionsExceededException e) {
            throw e;
          } catch (TCTimeoutException e) {
            handleConnectException(e, false, cmt.logger);
          } catch (IOException e) {
            handleConnectException(e, false, cmt.logger);
          } catch (Exception e) {
            handleConnectException(e, true, cmt.logger);
          }
        }
      }
      cmt.endIfDisconnected();
    } finally {
      connecting.set(false);
    }
  }

  private void handleConnectException(Exception e, boolean logFullException, TCLogger logger) {
    if (logger.isDebugEnabled() || logFullException) {
      logger.error("Connect Exception", e);
    } else {
      logger.warn(e.getMessage());
    }
    try {
      Thread.sleep(CONNECT_RETRY_INTERVAL);
    } catch (InterruptedException e1) {
      //
    }
  }

  public void asyncReconnect(ClientMessageTransport cmt) {
    synchronized (connecting) {
      if (connecting.get()) return;

      if (connectionEstablisher == null) {
        connecting.set(true);
        // First time
        connectionEstablisher = new Thread(new AsyncReconnect(cmt, this), "ConnectionEstablisher");
        connectionEstablisher.setDaemon(true);
        connectionEstablisher.start();

      }
      reconnectRequest.put(RECONNECT);
    }
  }

  public void quitReconnectAttempts() {
    reconnectRequest.put(QUIT);
  }

  static class AsyncReconnect implements Runnable {
    private final ClientMessageTransport      cmt;
    private final ClientConnectionEstablisher cce;

    public AsyncReconnect(ClientMessageTransport cmt, ClientConnectionEstablisher cce) {
      this.cmt = cmt;
      this.cce = cce;
    }

    public void run() {
      Object request = null;
      while ((request = cce.reconnectRequest.take()) != null) {
        if (request == RECONNECT) {
          try {
            cce.reconnect(cmt);
          } catch (MaxConnectionsExceededException e) {
            cmt.logger.warn(e);
            cmt.logger.warn("No longer trying to reconnect.");
            return;
          } catch (Throwable t) {
            cmt.logger.warn("Reconnect failed !", t);
          }
        } else if (request == QUIT) {
          break;
        }
      }
    }
  }

}
