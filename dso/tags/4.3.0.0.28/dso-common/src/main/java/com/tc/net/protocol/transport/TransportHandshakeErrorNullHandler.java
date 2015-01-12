/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;


public class TransportHandshakeErrorNullHandler implements TransportHandshakeErrorHandler {


  @Override
  public void handleHandshakeError(TransportHandshakeErrorContext e) {
    // NOP
  }

}
