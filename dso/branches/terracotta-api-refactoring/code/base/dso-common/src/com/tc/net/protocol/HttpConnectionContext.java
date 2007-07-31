/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.async.api.EventContext;
import com.tc.bytes.ITCByteBuffer;

import java.net.Socket;

public class HttpConnectionContext implements EventContext {

  private final ITCByteBuffer buffer;
  private final Socket       socket;

  public HttpConnectionContext(Socket socket, ITCByteBuffer buffer) {
    this.socket = socket;
    this.buffer = buffer;
  }

  public ITCByteBuffer getBuffer() {
    return buffer;
  }

  public Socket getSocket() {
    return socket;
  }

}
