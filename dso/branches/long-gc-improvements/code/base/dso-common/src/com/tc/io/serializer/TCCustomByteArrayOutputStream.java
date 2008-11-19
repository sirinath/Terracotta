/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class TCCustomByteArrayOutputStream extends OutputStream {
  
  private byte[] fixedBuffer;
  private int    currentStart = 0;
  private int    next = 0;
  private ByteArrayOutputStream baos = new ByteArrayOutputStream();
  static int DEFAULT_BUFFER_SIZE = 1024*1024*10;
  
  private static final TCLogger logger = TCLogging.getLogger(TCCustomByteArrayOutputStream.class);
  private int    objectCount = 0;         // just for logging

  public TCCustomByteArrayOutputStream() {        
    fixedBuffer = new byte[DEFAULT_BUFFER_SIZE];
  }

  public synchronized void reset() {
    baos.reset();
    currentStart = 0;
    next = 0;
    objectCount = 0;
  }

  public void endOfChunk() {  
    baos.reset();
    currentStart = next;
    objectCount++;
  }

  public CustomArray getCurrentBytes() {
    if (baos.size() == 0) {
      return new CustomArray(currentStart, next - currentStart, this.fixedBuffer);
    } else {
      return new CustomArray(0, baos.size(), baos.toByteArray());
    }
  }

  public String toString() {
    return new String(fixedBuffer, 0, next) + baos.toString();
  }

  public void write(byte[] b, int off, int len) {
    if (baos.size() > 0) {
      baos.write(b, off, len); 
    } else if (next+len <= fixedBuffer.length) {
      System.arraycopy(b, off, fixedBuffer, next, len);
      next += len;
    } else {
      switchToBAOS(len);
      baos.write(b, off, len);
    }
  }

  private void switchToBAOS(int len) {
    logger.debug("No room in fixed buffer for " + len + " bytes.  Buffer contains " + objectCount+" objects.");
    baos.write(fixedBuffer, currentStart, next - currentStart);
    next = currentStart;
  }

  public void write(int b) {
    if (baos.size() > 0) {
      baos.write(b); 
    } else if (next+1 <= fixedBuffer.length) {
      fixedBuffer[next] = (byte) b;
      next++;
    } else {
      switchToBAOS(1);
      baos.write(b);
    }
  }

  public void write(byte[] b) {
    write(b, 0, b.length);
  }

  public static class CustomArray {
    public final int    offset;
    public final int    length;
    public final byte[] buffer;

    public CustomArray(int offset, int length, byte[] buffer) {
      this.offset = offset;
      this.length = length;
      this.buffer = buffer;
    }
  }

}
