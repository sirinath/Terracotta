/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import com.tc.bytes.ITCByteBuffer;

import java.io.IOException;

public interface TCByteBufferInput extends TCDataInput {

  /**
   * Duplicate this stream. The resulting stream will share data with the source stream (ie. no copying), but the two
   * streams will have independent read positions. The read position of the result stream will initially be the same as
   * the source stream
   */
  public abstract TCByteBufferInput duplicate();

  /**
   * Effectively the same thing as calling duplicate().limit(int), but potentially creating far less garbage (depending
   * on the size difference between the original stream and the slice you want)
   */
  public abstract TCByteBufferInput duplicateAndLimit(final int limit);

  public abstract ITCByteBuffer[] toArray();

  /**
   * Artificially limit the length of this input stream starting at the current read position. This operation is
   * destructive to the stream contents (ie. data trimmed off by setting limit can never be read with this stream).
   */
  public abstract TCDataInput limit(int limit);

  public abstract int getTotalLength();

  public abstract int available();

  public abstract void close();

  public abstract void mark(int readlimit);

  // XXX: This is a TC special version of mark() to be used in conjunction with tcReset()...We should eventually
  // implement the general purpose mark(int) method as specified by InputStream. NOTE: It has some unusual semantics
  // that make it a little trickier to implement (in our case) than you might think (specifially the readLimit field)
  public abstract void mark();

  public abstract boolean markSupported();

  public abstract int read(byte[] b);

  public abstract int read();

  public abstract void reset();

  /**
   * Reset this input stream to the position recorded by the last call to mark(). This method discards the previous
   * value of the mark
   * 
   * @throws IOException if mark() has never been called on this stream
   */
  public abstract void tcReset();

  public abstract long skip(long skip);

  public abstract int readInt() throws IOException;

  public abstract byte readByte() throws IOException;

  public abstract boolean readBoolean() throws IOException;

  public abstract char readChar() throws IOException;

  public abstract double readDouble() throws IOException;

  public abstract long readLong() throws IOException;

  public abstract float readFloat() throws IOException;

  public abstract short readShort() throws IOException;

  public abstract void readFully(byte[] b) throws IOException;

  public abstract void readFully(byte[] b, int off, int len) throws IOException;

  public abstract int skipBytes(int n);

  public abstract int readUnsignedByte() throws IOException;

  public abstract int readUnsignedShort() throws IOException;

  public abstract String readLine();

  public abstract String readUTF();

}