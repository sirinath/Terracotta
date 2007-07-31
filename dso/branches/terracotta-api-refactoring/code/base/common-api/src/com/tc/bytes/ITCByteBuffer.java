/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

import com.tc.lang.Recyclable;

import java.nio.ByteBuffer;

public interface ITCByteBuffer extends Recyclable {

  public abstract ITCByteBuffer clear();

  public abstract int capacity();

  public abstract int position();

  public abstract ITCByteBuffer flip();

  public abstract boolean hasRemaining();

  public abstract int limit();

  public abstract ITCByteBuffer limit(int newLimit);

  public abstract ITCByteBuffer position(int newPosition);

  public abstract int remaining();

  public abstract ITCByteBuffer rewind();

  public abstract ByteBuffer getNioBuffer();

  public abstract boolean isDirect();

  public abstract byte[] array();

  public abstract byte get();

  public abstract boolean getBoolean();

  public abstract boolean getBoolean(int index);

  public abstract char getChar();

  public abstract char getChar(int index);

  public abstract double getDouble();

  public abstract double getDouble(int index);

  public abstract float getFloat();

  public abstract float getFloat(int index);

  public abstract int getInt();

  public abstract int getInt(int index);

  public abstract long getLong();

  public abstract long getLong(int index);

  public abstract short getShort();

  public abstract short getShort(int index);

  public abstract ITCByteBuffer get(byte[] dst);

  public abstract ITCByteBuffer get(byte[] dst, int offset, int length);

  public abstract byte get(int index);

  public abstract ITCByteBuffer put(byte b);

  public abstract ITCByteBuffer put(byte[] src);

  public abstract ITCByteBuffer put(byte[] src, int offset, int length);

  public abstract ITCByteBuffer put(int index, byte b);

  public abstract ITCByteBuffer putBoolean(boolean b);

  public abstract ITCByteBuffer putBoolean(int index, boolean b);

  public abstract ITCByteBuffer putChar(char c);

  public abstract ITCByteBuffer putChar(int index, char c);

  public abstract ITCByteBuffer putDouble(double d);

  public abstract ITCByteBuffer putDouble(int index, double d);

  public abstract ITCByteBuffer putFloat(float f);

  public abstract ITCByteBuffer putFloat(int index, float f);

  public abstract ITCByteBuffer putInt(int i);

  public abstract ITCByteBuffer putInt(int index, int i);

  public abstract ITCByteBuffer putLong(long l);

  public abstract ITCByteBuffer putLong(int index, long l);

  public abstract ITCByteBuffer putShort(short s);

  public abstract ITCByteBuffer putShort(int index, short s);

  public abstract ITCByteBuffer duplicate();

  public abstract ITCByteBuffer put(ITCByteBuffer src);

  public abstract ITCByteBuffer slice();

  public abstract int arrayOffset();

  public abstract ITCByteBuffer asReadOnlyBuffer();

  public abstract boolean isReadOnly();

  public abstract boolean hasArray();

  // Can be called only once on any of the views and the root is gone
  public abstract void recycle();

  public abstract ITCByteBuffer get(int index, byte[] dst);

  public abstract ITCByteBuffer get(int index, byte[] dst, int offset, int length);

  public abstract ITCByteBuffer put(int index, byte[] src);

  public abstract ITCByteBuffer put(int index, byte[] src, int offset, int length);

  public abstract ITCByteBuffer putUint(long i);

  public abstract ITCByteBuffer putUint(int index, long i);

  public abstract ITCByteBuffer putUshort(int s);

  public abstract ITCByteBuffer putUshort(int index, int s);

  public abstract long getUint();

  public abstract long getUint(int index);

  public abstract int getUshort();

  public abstract int getUshort(int index);

  public abstract short getUbyte();

  public abstract short getUbyte(int index);

  public abstract ITCByteBuffer putUbyte(int index, short value);

  public abstract ITCByteBuffer putUbyte(short value);

  public abstract void commit();

  public abstract void checkedOut();

}