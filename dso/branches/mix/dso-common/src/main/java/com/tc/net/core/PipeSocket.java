/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;

/**
 * @author Ludovic Orban
 */
public class PipeSocket extends Socket {

  private final Pipe       inputPipe;
  private final Pipe       outputPipe;
  private volatile boolean closed = false;

  public PipeSocket() throws IOException {
    this.inputPipe = Pipe.open();
    this.outputPipe = Pipe.open();
    this.outputPipe.source().configureBlocking(false);
  }

  public int addToReceiveBuffer(ByteBuffer buffer) throws IOException {
    return inputPipe.sink().write(buffer);
  }

  public int getFromSendBuffer(ByteBuffer buffer) throws IOException {
    try {
      return outputPipe.source().read(buffer);
    } catch (AsynchronousCloseException e) {
      return -1;
    }
  }

  @Override
  public InputStream getInputStream() {
    return Channels.newInputStream(inputPipe.source());
  }

  @Override
  public OutputStream getOutputStream() {
    return new PipeSocketOutputStream(Channels.newOutputStream(outputPipe.sink()));
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) return;
    closed = true;
    super.close();
    inputPipe.source().close();
    outputPipe.sink().close();
  }

  public void dispose() throws IOException {
    if (!isClosed()) {
      close();
    }
    inputPipe.sink().close();
    outputPipe.source().close();
  }

  public void onWrite() {
    //
  }

  public void closeRead() throws IOException {
    inputPipe.source().close();
  }

  public void closeWrite() throws IOException {
    outputPipe.sink().close();
  }

  private final class PipeSocketOutputStream extends OutputStream {

    private final OutputStream delegate;

    PipeSocketOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
      delegate.write(b);
      onWrite();
    }
  }
}
