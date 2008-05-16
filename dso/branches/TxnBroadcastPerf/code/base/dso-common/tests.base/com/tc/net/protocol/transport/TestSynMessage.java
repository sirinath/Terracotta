/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.TCNetworkMessageEvent;
import com.tc.net.protocol.TCNetworkMessageListener;

import java.util.Iterator;
import java.util.Set;

public class TestSynMessage extends TestTransportHandshakeMessage implements SynMessage {
  private final Set listeners = new CopyOnWriteArraySet();

  public boolean isSyn() {
    return true;
  }

  public boolean isSynAck() {
    return false;
  }

  public boolean isAck() {
    return false;
  }

  public short getStackLayerFlags() {
    // its a test
    return NetworkLayer.TYPE_TEST_MESSAGE;
  }

  public int getCallbackPort() {
    return TransportHandshakeMessage.NO_CALLBACK_PORT;
  }

  public void addListener(TCNetworkMessageListener listener) {
    listeners.add(listener);
  }

  public boolean isEmptyListeners() {
    return (listeners.isEmpty());
  }

  public void notifyMessageEvent(TCNetworkMessageEvent event) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      ((TCNetworkMessageListener) i.next()).notifyMessageEvent(event);
    }
  }
}
