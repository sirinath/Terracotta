/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author Ludovic Orban
 */
class ClearTextBufferManager implements BufferManager {
  private static final TCLogger logger         = TCLogging.getLogger(ClearTextBufferManager.class);
  private static final String   BUFFER_SIZE    = "clear.text.buffer.size";
  private static final int      BUFFER_SIZE_KB = Integer.getInteger(BUFFER_SIZE, 16) * 1024;
  private final SocketChannel   channel;
  private final ByteBuffer      sendBuffer     = ByteBuffer.allocate(BUFFER_SIZE_KB);
  private final ByteBuffer      recvBuffer     = ByteBuffer.allocate(BUFFER_SIZE_KB);

  ClearTextBufferManager(SocketChannel channel) {
    this.channel = channel;
    if (logger.isDebugEnabled()) {
      logger.debug("ClearTextBufferManager " + BUFFER_SIZE + " " + BUFFER_SIZE_KB);
    }
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
