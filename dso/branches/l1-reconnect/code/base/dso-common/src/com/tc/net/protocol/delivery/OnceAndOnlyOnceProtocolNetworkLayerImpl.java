/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolException;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.transport.AbstractMessageTransport;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.WireProtocolMessage;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * NetworkLayer implementation for once and only once message delivery protocol.
 */
public class OnceAndOnlyOnceProtocolNetworkLayerImpl extends AbstractMessageTransport implements
    OnceAndOnlyOnceProtocolNetworkLayer, OOOProtocolMessageDelivery {
  private static final TCLogger           logger       = TCLogging
                                                           .getLogger(OnceAndOnlyOnceProtocolNetworkLayerImpl.class);
  private final OOOProtocolMessageFactory messageFactory;
  private final OOOProtocolMessageParser  messageParser;
  boolean                                 wasConnected = false;
  private MessageChannelInternal          receiveLayer;
  private MessageTransport                sendLayer;
  private GuaranteedDeliveryProtocol      delivery;

  public OnceAndOnlyOnceProtocolNetworkLayerImpl(OOOProtocolMessageFactory messageFactory,
                                                 OOOProtocolMessageParser messageParser, Sink workSink) {
    super(logger);
    this.messageFactory = messageFactory;
    this.messageParser = messageParser;
    this.delivery = new GuaranteedDeliveryProtocol(this, workSink, new LinkedQueue());
    this.delivery.start();
  }

  /*********************************************************************************************************************
   * Network layer interface...
   */

  public void setSendLayer(NetworkLayer layer) {
    if (!(layer instanceof MessageTransport)) { throw new IllegalArgumentException(
                                                                                   "Error: send layer must be MessageTransport!"); }
    this.setSendLayer((MessageTransport) layer);
  }

  public void setSendLayer(MessageTransport transport) {
    this.sendLayer = transport;
    delivery.setTransport(sendLayer);
  }

  public void setReceiveLayer(NetworkLayer layer) {
    if (!(layer instanceof MessageChannelInternal)) { throw new IllegalArgumentException(
                                                                                         "Error: receive layer must be MessageChannelInternal, was "
                                                                                             + layer.getClass()
                                                                                                 .getName()); }
    this.receiveLayer = (MessageChannelInternal) layer;
    delivery.setUpperLayer(receiveLayer);
  }

  public void send(TCNetworkMessage message) {
    delivery.send(message);
  }

  public void receive(TCByteBuffer[] msgData) {
    OOOProtocolMessage msg = createProtocolMessage(msgData);
    delivery.receive(msg);
  }

  public boolean isConnected() {
    Assert.assertNotNull(sendLayer);
    return sendLayer.isConnected();
  }

  public NetworkStackID open() throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException {
    Assert.assertNotNull(sendLayer);
    return sendLayer.open();
  }

  public void close() {
    Assert.assertNotNull(sendLayer);

    // TODO: There is definitely something missing here. We need to cancel/quiesce the delivery instance before closing
    // the transport

    sendLayer.close();
  }

  /*********************************************************************************************************************
   * Transport listener interface...
   */

  public void notifyTransportConnected(MessageTransport transport) {
    logNotifyTransportConnected(transport);
    this.delivery.resume();
    receiveLayer.notifyTransportConnected(this);
  }

  private void logNotifyTransportConnected(MessageTransport transport) {
    if (logger.isDebugEnabled()) {
      logger.debug("notifyTransportConnected(" + transport + ")");
    }
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    this.delivery.pause();
    receiveLayer.notifyTransportDisconnected(this);
  }

  public void pause() {
    this.delivery.pause();
  }
  
  public void resume() {
    this.delivery.resume();
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    //
    receiveLayer.notifyTransportConnectAttempt(this);
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // XXX: do we do anything here? We've probably done everything we need to do when close() was called.
    receiveLayer.notifyTransportClosed(this);
  }

  /*********************************************************************************************************************
   * Protocol Message Delivery interface
   */

  public void sendAckRequest() {
    sendToSendLayer(this.messageFactory.createNewAckRequestMessage());
  }

  public void sendAck(long sequence) {
    sendToSendLayer(this.messageFactory.createNewAckMessage(sequence));
  }

  public void sendMessage(OOOProtocolMessage msg) {
    sendToSendLayer(msg);
  }

  public void receiveMessage(OOOProtocolMessage msg) {
    Assert.assertNotNull("Receive layer is null.", this.receiveLayer);
    Assert.assertNotNull("Attempt to null msg", msg);
    Assert.eval(msg.isSend());

    this.receiveLayer.receive(msg.getPayload());
  }

  public OOOProtocolMessage createProtocolMessage(long sequence, final TCNetworkMessage msg) {
    OOOProtocolMessage rv = messageFactory.createNewSendMessage(sequence, msg);

    final Runnable callback = msg.getSentCallback();
    if (callback != null) {
      rv.setSentCallback(new Runnable() {
        public void run() {
          callback.run();
        }
      });
    }

    return rv;
  }

  private void sendToSendLayer(OOOProtocolMessage msg) {
    // this method doesn't do anything at the moment, but it is a good spot to plug in things you might want to do
    // every message flowing down from the layer (like logging for example)
    this.sendLayer.send(msg);
  }

  private OOOProtocolMessage createProtocolMessage(TCByteBuffer[] msgData) {
    try {
      return messageParser.parseMessage(msgData);
    } catch (TCProtocolException e) {
      // XXX: this isn't the right thing to do here
      throw new TCRuntimeException(e);
    }
  }

  public void attachNewConnection(TCConnection connection) {
    throw new AssertionError("Must not call!");
  }

  public ConnectionID getConnectionId() {
    return sendLayer.getConnectionId();
  }

  public TCSocketAddress getLocalAddress() {
    return sendLayer.getLocalAddress();
  }

  public TCSocketAddress getRemoteAddress() {
    return sendLayer.getRemoteAddress();
  }

  public void receiveTransportMessage(WireProtocolMessage message) {
    throw new AssertionError("Must not call!");
  }

  public void sendToConnection(TCNetworkMessage message) {
    throw new AssertionError("Must not call!");
  }
}