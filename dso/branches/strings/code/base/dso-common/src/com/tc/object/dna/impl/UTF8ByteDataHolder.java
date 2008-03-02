/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.compression.BinaryData;
import com.tc.object.compression.Decompressor;
import com.tc.object.compression.StringDecompressor;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/**
 * Holds byte data for strings. The main purpose of this class is to simply hold the bytes data for a String (no sense
 * turning actually into a String instance in L2)
 * <p>
 * The reason it is UTF8ByteDataHolder and not ByteDataHolder is that asString() assumes that the byte data is a valid
 * UTF-8 encoded bytes and creates String as <code> new String(bytes, "UTF-8"); </code>
 */
public class UTF8ByteDataHolder implements Serializable {

  private final BinaryData binaryData;
  private final Decompressor stringDecompressor;
  private final String encoding;

  // Used for tests
  public UTF8ByteDataHolder(String str) {
    StringDecompressor decompressor = new StringDecompressor();
    this.stringDecompressor = decompressor;
    this.encoding = decompressor.getEncoding();
    try {
      byte[] bytes = str.getBytes(this.encoding);
      this.binaryData = new BinaryData(bytes);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public UTF8ByteDataHolder(BinaryData binaryData) {
    this(binaryData, new StringDecompressor());
  }

  public UTF8ByteDataHolder(BinaryData binaryData, Decompressor stringDecompressor) {
    this.binaryData = binaryData;
    this.stringDecompressor = stringDecompressor;
    this.encoding = ((StringDecompressor)stringDecompressor).getEncoding();   
  }

  public BinaryData getBinaryData(){
    return this.binaryData;
  }
  
  public byte[] getBytes() {
    return getBinaryData().getBytes();
  }

  public String asString() {
    return (isCompressed() ? inflate() : getString());
  }

  private String inflate() {
    return (String)this.stringDecompressor.decompress(getBinaryData());
  }

  private String getString() {
    try {
      return new String(getBinaryData().getBytes(), this.encoding);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public String toString() {
    return asString();
  }

  public int hashCode() {
    return getBinaryData().hashCode();
  }

  public boolean equals(Object obj) {
    if (obj instanceof UTF8ByteDataHolder) {
      UTF8ByteDataHolder other = (UTF8ByteDataHolder) obj;
      return getBinaryData().equals(other.getBinaryData());
    }
    return false;
  }

  public boolean isCompressed() {
    return getBinaryData().isCompressed();
  }

  public int getUnCompressedStringLength() {
    return getBinaryData().getUncompressedLength();
  }
}
