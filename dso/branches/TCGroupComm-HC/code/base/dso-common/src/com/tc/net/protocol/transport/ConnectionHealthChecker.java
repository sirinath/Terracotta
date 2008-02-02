/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.net.protocol.transport;

public interface ConnectionHealthChecker extends MessageTransportListener {

  public void start();

  public boolean isRunning();

  public void stop();
}