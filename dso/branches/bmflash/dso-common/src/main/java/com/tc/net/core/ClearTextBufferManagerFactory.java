/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import java.nio.channels.SocketChannel;

/**
 * @author Ludovic Orban
 */
public class ClearTextBufferManagerFactory implements BufferManagerFactory {
  @Override
  public BufferManager createBufferManager(SocketChannel socketChannel, boolean client) {
    return new ClearTextBufferManager(socketChannel);
  }
}
