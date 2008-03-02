/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.io.TCDataInput;

import java.io.UnsupportedEncodingException;

public class StringDecompressor implements Decompressor {
  
  public static final String DEFAULT_ENCODING = StringCompressor.DEFAULT_ENCODING;
  private final String encoding;
  private final Decompressor byteArrayDecompressor;
  
  public StringDecompressor() {
    this(DEFAULT_ENCODING);
  }

  public StringDecompressor(String encoding) {
    this.encoding = encoding;
    this.byteArrayDecompressor = new ByteArrayDecompressor();
  }

  public Object readCompressed(TCDataInput compressedInput) {
    byte[] uncompressedData = (byte[])this.byteArrayDecompressor.readCompressed(compressedInput);
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
