/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

/**
 * Monitors transport events.
 */
public class TransportEventMonitor implements MessageTransportListener {

  private final LinkedQueue connectedEvents      = new LinkedQueue();
  private final LinkedQueue disconnectedEvents   = new LinkedQueue();
  private final LinkedQueue connectAttemptEvents = new LinkedQueue();
  private final LinkedQueue closedEvents         = new LinkedQueue();

  public void notifyTransportConnected(MessageTransport transport) {
    try {
      connectedEvents.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    try {
      disconnectedEvents.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    try {
      this.connectAttemptEvents.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void notifyTransportClosed(MessageTransport transport) {
    try {
      this.closedEvents.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public boolean waitForConnect(long timeout) throws InterruptedException {
    return this.connectedEvents.poll(timeout) != null;
  }

  public boolean waitForDisconnect(long timeout) throws InterruptedException {
    return this.disconnectedEvents.poll(timeout) != null;
  }

  public boolean waitForConnectAttempt(long timeout) throws InterruptedException {
    return this.connectAttemptEvents.poll(timeout) != null;
  }

  public boolean waitForClose(long timeout) throws InterruptedException {
    return this.closedEvents.poll(timeout) != null;
  }

}