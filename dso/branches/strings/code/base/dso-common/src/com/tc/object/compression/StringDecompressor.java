/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import java.io.UnsupportedEncodingException;

public class StringDecompressor implements Decompressor {
  
  public static final String DEFAULT_ENCODING = StringCompressor.DEFAULT_ENCODING;
  private final String encoding;
  private final Decompressor byteArrayDecompressor;
  
  public StringDecompressor() {
    this(DEFAULT_ENCODING);
  }

  public StringDecompressor(String encoding) {
    this(encoding, new ByteArrayDecompressor());
  }

  public StringDecompressor(String encoding, Decompressor byteArrayDecompressor) {
    this.encoding = encoding;
    this.byteArrayDecompressor = byteArrayDecompressor;
  }

  public Object decompress(BinaryData data) {
    byte[] uncompressedData = (byte[])this.byteArrayDecompressor.decompress(data);
    try {
      return new String(uncompressedData, this.encoding);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public String getEncoding() {
    return encoding;
  }
}
