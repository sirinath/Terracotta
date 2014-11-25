/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.TCRuntimeException;

import java.nio.ByteBuffer;

/**
 * Only wanted to write the real dump() implementation once, so this wrapper should mask whether we are working with an
 * NIO byte buffer or a TC byte buffer.
 */
class ByteishWrapper implements ByteishBuffer {

  private ByteBuffer   nioBuf;
  private TCByteBuffer tcBuf;

  ByteishWrapper(ByteBuffer nioBuf, TCByteBuffer tcBuf) {
    if (nioBuf != null) {
      this.nioBuf = nioBuf;
    } else if (tcBuf != null) {
      this.tcBuf = tcBuf;
    } else {
      throw new TCRuntimeException("Must specify a ByteBuffer or TCByteBuffer");
    }
  }

  @Override
  public byte get(int position) {
    return nioBuf != null ? nioBuf.get(position) : tcBuf.get(position);
  }

  @Override
  public int limit() {
    return nioBuf != null ? nioBuf.limit() : tcBuf.limit();
  }

}
