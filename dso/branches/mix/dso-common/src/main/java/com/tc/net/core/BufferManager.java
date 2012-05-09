/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public interface BufferManager {
  ByteBuffer getSendBuffer();
  ByteBuffer getRecvBuffer();

  int send() throws IOException;
  int recv() throws IOException;

  void close() throws IOException;
}
