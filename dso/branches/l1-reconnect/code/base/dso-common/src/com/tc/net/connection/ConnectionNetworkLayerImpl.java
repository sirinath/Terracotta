/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.connection;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventCaller;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.WireProtocolMessage;
import com.tc.net.protocol.transport.WireProtocolMessageImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

public class ConnectionNetworkLayerImpl implements ConnectionNetworkLayer {
  private final static TCLogger             logger         = TCLogging.getLogger(ConnectionNetworkLayerImpl.class);

  private final SynchronizedBoolean         isOpen         = new SynchronizedBoolean(false);
  private final List                        eventListeners = new CopyOnWriteArrayList();
  private final ClientConnectionEstablisher connectionEstablisher;
  private TCConnection                      connection;

  public ConnectionNetworkLayerImpl(ClientConnectionEstablisher connectionEstablisher) {
    this.connectionEstablisher = connectionEstablisher;
  }

  public final void addListener(TCConnectionEventListener listener) {
    if (listener == null) { return; }
    eventListeners.add(listener); // don't need sync
  }

  public final void removeListener(TCConnectionEventListener listener) {
    if (listener == null) { return; }
    eventListeners.remove(listener); // don't need sync
  }

  public final void close() {
    final boolean doClose = isOpen.set(false);
    if (doClose) {
      // see DEV-659: we used to throw an assertion error here if already closed
      logger.warn("Can only close an open connection");
      return;
    }
    synchronized (isOpen) {
      if (connection != null && !this.connection.isClosed()) {
        TCConnectionEventCaller.fireCloseEvent(eventListeners, new TCConnectionEvent(connection), logger);
        connection.asynchClose();
        connection = null;
      }
    }
  }

  public boolean isConnected() {
    return isOpen.get();
  }

  public NetworkStackID open() throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException,
      IOException {
    synchronized (isOpen) {
      Assert.eval("can't open an already open transport", !isOpen.get());
      try {
        connection = connectionEstablisher.open();
        isOpen.set(true);
        return new NetworkStackID(System.identityHashCode(connection));
      } catch (TCTimeoutException e) {
        throw e;
      } catch (IOException e) {
        throw e;
      }
    }
  }

  public void receive(TCByteBuffer[] msgData) {
    throw new AssertionError("Must not use");
  }

  public void send(TCNetworkMessage message) {
    if (!isOpen.get()) {
      logger.warn("Ignoring message sent to non-established transport: " + message);
      return;
    }
    sendToConnection(message);
  }

  public void setReceiveLayer(NetworkLayer layer) {
    throw new ImplementMe();

  }

  public void setSendLayer(NetworkLayer layer) {
    throw new ImplementMe();

  }

  private final void sendToConnection(TCNetworkMessage message) {
    if (message == null) throw new AssertionError("Attempt to send a null message.");
    if (!(message instanceof WireProtocolMessage)) {
      final TCNetworkMessage payload = message;

      message = WireProtocolMessageImpl.wrapMessage(message, connection);
      Assert.eval(message.getSentCallback() == null);

      final Runnable callback = payload.getSentCallback();
      if (callback != null) {
        message.setSentCallback(new Runnable() {
          public void run() {
            callback.run();
          }
        });
      }
    }
  }
}
