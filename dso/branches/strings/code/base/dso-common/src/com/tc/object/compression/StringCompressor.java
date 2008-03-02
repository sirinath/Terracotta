/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.io.TCDataOutput;

import java.io.UnsupportedEncodingException;

public class StringCompressor implements Compressor {
  
  public static final String DEFAULT_ENCODING = "UTF-8";
  private final String encoding;
  private final Compressor byteArrayCompressor;
  
  public StringCompressor(){
    this(DEFAULT_ENCODING);
  }

  public StringCompressor(String encoding) {
    this.encoding = encoding;
    this.byteArrayCompressor = new ByteArrayCompressor();
  }
  
  public void writeCompressed(Object object, TCDataOutput output) {
    String string = (String)object;
    try {
      this.byteArrayCompressor.writeCompressed(string.getBytes(this.encoding), output);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }  
  
}
