/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

/**
 * Holds byte data for strings. The main purpose of this class is to simply hold the bytes data for a String (no sense
 * turning actually into a String instance in L2)
 * <p>
 * The reason it is UTF8ByteDataHolder and not ByteDataHolder is that asString() assumes that the byte data is a valid
 * UTF-8 encoded bytes and creates String as <code> new String(bytes, "UTF-8"); </code>
 */
public class UTF8ByteDataHolder implements Serializable {

  private final byte[]  bytes;
  private final boolean isCompressed;
  private int           hash = 0;

  // Used for tests
  public UTF8ByteDataHolder(String str) {
    this.isCompressed = false;
    try {
      this.bytes = str.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  public UTF8ByteDataHolder(byte[] b) {
    this(b, false);
  }

  public UTF8ByteDataHolder(byte[] b, boolean isCompressed) {
    this.bytes = b;
    this.isCompressed = isCompressed;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public String asString() {
    return (isCompressed ? inflate() : getString());
  }

  /**
   * TODO::There are 4 copies of byte arrays happening in this method. Ridiculous.
   */
  private String inflate() {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      InflaterInputStream iis = new InflaterInputStream(bais);
      ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
      byte uncompressed[] = new byte[4096];
      int read;
      while ((read = iis.read(uncompressed)) != -1) {
        baos.write(uncompressed, 0, read);
      }
      iis.close();
      return new String(baos.toByteArray(), "UTF-8");
    } catch (IOException e) {
      throw new AssertionError(e);
    }
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
    if (hash == 0) {
      int tmp = isCompressed ? 21 : 37;
      for (int i = 0, n = bytes.length; i < n; i++) {
        tmp = 31 * tmp + bytes[i++];
      }
      hash = tmp;
    }
    return hash;
  }

  public boolean equals(Object obj) {
    if (obj instanceof UTF8ByteDataHolder) {
      UTF8ByteDataHolder other = (UTF8ByteDataHolder) obj;
      return ((isCompressed == other.isCompressed) && (Arrays.equals(this.bytes, other.bytes)) && this.getClass()
          .equals(other.getClass()));
    }
    return false;
  }

  public boolean isCompressed() {
    return isCompressed;
  }
}
