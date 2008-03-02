/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.io.TCByteArrayOutputStream;

import java.util.zip.DeflaterOutputStream;

public class ByteArrayCompressor implements Compressor {

  public BinaryData compress(Object object) {
    try {
      TCByteArrayOutputStream byteArrayOS = new TCByteArrayOutputStream(4096);
      // Stride is 512 bytes by default, should I increase ?
      DeflaterOutputStream dos = new DeflaterOutputStream(byteArrayOS);
      byte[] uncompressed = (byte[])object;
      dos.write(uncompressed);
      dos.close();
      byte[] compressed = byteArrayOS.getInternalArray();
      // XXX:: We are writting the original array's length so that we save a couple of copies when decompressing
      return new BinaryData(compressed, uncompressed.length);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

}
