/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.compression.Decompressor;
import com.tc.object.compression.StringDecompressor;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Holds byte data for strings. The main purpose of this class is to simply hold the bytes data for a String (no sense
 * turning actually into a String instance in L2)
 * <p>
 * The reason it is UTF8ByteDataHolder and not ByteDataHolder is that asString() assumes that the byte data is a valid
 * UTF-8 encoded bytes and creates String as <code> new String(bytes, "UTF-8"); </code>
 */
public class UTF8ByteDataHolder implements Serializable {

  private final byte[] bytes;
  private final int    uncompressedLength;
  private final Decompressor stringDecompressor;
  private final String encoding;

  // Used for tests
  public UTF8ByteDataHolder(String str) {
    this.uncompressedLength = -1;
    StringDecompressor decompressor = new StringDecompressor();
    this.stringDecompressor = decompressor;
    this.encoding = decompressor.getEncoding();
    try {
      this.bytes = str.getBytes(this.encoding);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public UTF8ByteDataHolder(byte[] b) {
    this(b, -1, new StringDecompressor());
  }

  public UTF8ByteDataHolder(byte[] b, int uncompressedLength, Decompressor stringDecompressor) {
    this.bytes = b;
    this.uncompressedLength = uncompressedLength;
    this.stringDecompressor = stringDecompressor;
    this.encoding = ((StringDecompressor)stringDecompressor).getEncoding();   
  }

  public byte[] getBytes() {
    return bytes;
  }

  public String asString() {
    return (isCompressed() ? inflate() : getString());
  }

  private String inflate() {
    return DNAEncodingImpl.inflateCompressedString(bytes, uncompressedLength);
  }

  private String getString() {
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public String toString() {
    return asString();
  }

  public int hashCode() {
    int hash = isCompressed() ? 21 : 37;
    for (int i = 0, n = bytes.length; i < n; i++) {
      hash = 31 * hash + bytes[i++];
    }
    return hash;
  }

  public boolean equals(Object obj) {
    if (obj instanceof UTF8ByteDataHolder) {
      UTF8ByteDataHolder other = (UTF8ByteDataHolder) obj;
      return ((uncompressedLength == other.uncompressedLength) && (Arrays.equals(this.bytes, other.bytes)) && this
          .getClass().equals(other.getClass()));
    }
    return false;
  }

  public boolean isCompressed() {
    return uncompressedLength != -1;
  }

  public int getUnCompressedStringLength() {
    return uncompressedLength;
  }
}
