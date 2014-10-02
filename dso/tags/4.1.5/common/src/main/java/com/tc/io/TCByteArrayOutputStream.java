/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io;

import java.io.OutputStream;

/**
 * No-synch, reset()'able byte array stream, with public access to underlying
 * byte[]
 *
 * @author teck
 */
public final class TCByteArrayOutputStream extends OutputStream {

    private int size;

    private byte[] buffer;

    public TCByteArrayOutputStream() {
        this(64);
    }

    public TCByteArrayOutputStream(int initialSize) {
        buffer = new byte[initialSize];
    }

    private void ensureCapacity(int newCap) {
        byte newBuffer[] = new byte[Math.max(buffer.length * 2, newCap)];
        System.arraycopy(buffer, 0, newBuffer, 0, size);
        buffer = newBuffer;
    }

    @Override
    public final void write(int b) {
        int newSize = size + 1;
        if (newSize > buffer.length)
            ensureCapacity(newSize);
        buffer[size] = (byte) b;
        size = newSize;
    }

    @Override
    public final void write(byte b[], int offset, int len) {
        int newSize = size + len;
        if (newSize > buffer.length)
            ensureCapacity(newSize);
        System.arraycopy(b, offset, buffer, size, len);
        size = newSize;
    }

    public final void reset() {
        size = 0;
    }

    public final byte[] getInternalArray() {
        return buffer;
    }

    public final int size() {
        return size;
    }

}
