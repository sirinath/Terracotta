/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.util.StringTCUtil;

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
  private static final int HASH_SEED = 1704124966;
  private static final int FNV_32_PRIME = 0x01000193;

  private final byte[]  bytes;
  private final boolean interned;

  // Used for tests
  public UTF8ByteDataHolder(String str) {
    try {
      this.bytes = str.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }

    if (StringTCUtil.isInterned(str)) {
      this.interned = true;
    } else {
      this.interned = false;
    }
  }

  /**
   * Just byte data
   */
  public UTF8ByteDataHolder(byte[] b) {
    this(b, false);
  }

  /**
   * For a possibly interned, non-compressed string
   */
  public UTF8ByteDataHolder(byte[] b, boolean interned) {
    this.bytes = b;
    this.interned = interned;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public String asString() {
    return getString();
  }

  public boolean isInterned() {
    return this.interned;
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
    return computeHashCode(HASH_SEED);
  } 
  
  protected int computeHashCode(int init) {
    int hash = init;
    for (byte b : bytes) {
      hash ^= b;
      hash *= FNV_32_PRIME;
    }
    return hash;
  }

  public boolean equals(Object obj) {
    if (obj instanceof UTF8ByteDataHolder) {
      UTF8ByteDataHolder other = (UTF8ByteDataHolder) obj;
      return (Arrays.equals(this.bytes, other.bytes)) && this.getClass().equals(other.getClass());
    }
    return false;
  }

}
