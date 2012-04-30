/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author Ludovic Orban
 */
class ClearTextBufferManager implements BufferManager {

  private final SocketChannel channel;
  private final ByteBuffer    sendBuffer = ByteBuffer.allocate(512);
  private final ByteBuffer    recvBuffer = ByteBuffer.allocate(512);

  ClearTextBufferManager(SocketChannel channel) {
    this.channel = channel;
  }

  public ByteBuffer getSendBuffer() {
    return sendBuffer;
  }

  public ByteBuffer getRecvBuffer() {
    return recvBuffer;
  }

  public int send() throws IOException {
    sendBuffer.flip();
    int written = channel.write(sendBuffer);
    sendBuffer.compact();
    return written;
  }

  public int recv() throws IOException {
    return channel.read(recvBuffer);
  }

  public void close() {
    //
  }
}
