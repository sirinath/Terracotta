/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.TCNetworkMessageEvent;
import com.tc.net.protocol.TCNetworkMessageListener;

import java.util.Iterator;
import java.util.Set;

public class TestSynAckMessage extends TestTransportHandshakeMessage implements SynAckMessage {
  private final Set listeners = new CopyOnWriteArraySet();

  public String getErrorContext() {
    throw new ImplementMe();
  }

  public short getErrorType() {
    throw new ImplementMe();
  }

  public boolean hasErrorContext() {
    throw new ImplementMe();
  }

  public boolean isSyn() {
    return false;
  }

  public boolean isSynAck() {
    return true;
  }

  public boolean isAck() {
    return false;
  }

  public void recycle() {
    return;
  }

  public short getStackLayerFlags() {
    return NetworkLayer.TYPE_TEST_MESSAGE;
  }

  public int getCallbackPort() {
    throw new ImplementMe();
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
