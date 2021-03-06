/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayer;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactory;
import com.tc.properties.ReconnectConfig;

public class MockOnceAndOnlyOnceProtocolNetworkLayerFactory implements OnceAndOnlyOnceProtocolNetworkLayerFactory {

  public OnceAndOnlyOnceProtocolNetworkLayer layer;

  public OnceAndOnlyOnceProtocolNetworkLayer createNewClientInstance(Sink sendSink, Sink receiveSink,
                                                                     ReconnectConfig reconnectConfig) {
    return layer;
  }

  public OnceAndOnlyOnceProtocolNetworkLayer createNewServerInstance(Sink sendSink, Sink receiveSink,
                                                                     ReconnectConfig reconnectConfig) {
    return layer;
  }
}
