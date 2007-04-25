/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.net.protocol.AbstractNetworkStackHarness;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;

public class OOONetworkStackHarness extends AbstractNetworkStackHarness {

  private final OnceAndOnlyOnceProtocolNetworkLayerFactory factory;
  private Sink                                             sink;
  private OnceAndOnlyOnceProtocolNetworkLayer              oooLayer;

  OOONetworkStackHarness(ServerMessageChannelFactory channelFactory, MessageTransport transport,
                         OnceAndOnlyOnceProtocolNetworkLayerFactory factory, Sink sink) {
    super(channelFactory, transport);
    this.factory = factory;
    this.sink = sink;
  }

  OOONetworkStackHarness(MessageTransportFactory transportFactory, MessageChannelInternal channel,
                         OnceAndOnlyOnceProtocolNetworkLayerFactory factory, Sink sink) {
    super(transportFactory, channel);
    this.factory = factory;
    this.sink = sink;
  }

  protected void connectStack() {
    channel.setSendLayer(oooLayer);
    oooLayer.setReceiveLayer(channel);

    oooLayer.setSendLayer(transport);
    transport.setReceiveLayer(oooLayer);
    transport.addTransportListener(oooLayer);
    transport.addTransportListener(channel);
  }

  protected void createIntermediateLayers() {
    oooLayer = factory.createNewInstance(sink);
  }
}
