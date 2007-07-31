/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.bytes.ITCByteBuffer;
import com.tc.exception.TCRuntimeException;

import java.nio.ByteBuffer;

/**
 * Only wanted to write the real dump() implementation once, so this wrapper should mask whether we are working with an
 * NIO byte buffer or a TC byte buffer.
 */
class ByteishWrapper implements ByteishBuffer {

  private ByteBuffer   nioBuf;
  private ITCByteBuffer tcBuf;

  private ByteishWrapper() {
    throw new TCRuntimeException("Private constructor should not be called");
  }

  ByteishWrapper(ByteBuffer nioBuf, ITCByteBuffer tcBuf) {
    if (nioBuf != null) {
      this.nioBuf = nioBuf;
    } else if (tcBuf != null) {
      this.tcBuf = tcBuf;
    } else {
      throw new TCRuntimeException("Must specify a ByteBuffer or TCByteBuffer");
    }
  }

  public byte get(int position) {
    return nioBuf != null ? nioBuf.get(position) : tcBuf.get(position);
  }

  public int limit() {
    return nioBuf != null ? nioBuf.limit() : tcBuf.limit();
  }

}