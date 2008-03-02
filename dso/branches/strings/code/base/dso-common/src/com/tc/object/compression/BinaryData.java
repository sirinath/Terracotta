/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;


/**
 * Encapsulates compressed or uncompressed binary data, easily readible from TCDataInput or writable to
 * TCDataOutput
 */
public final class BinaryData implements Serializable {

  static final int DEFAULT_UNCOMPRESSED_LENGTH = -1;
  
  private final byte[] bytes;
  private final int uncompressedLength;

  public BinaryData(byte[] bytes) {
    this(bytes, DEFAULT_UNCOMPRESSED_LENGTH);
  }

  public BinaryData(byte[] bytes, int uncompressedLength) {
    this.bytes = bytes;
    this.uncompressedLength = uncompressedLength;
  }

  public byte[] getBytes() {
    return this.bytes;
  }

  public int getUncompressedLength() {
    return this.uncompressedLength;
  }

  public boolean isCompressed() {
    return (this.uncompressedLength != DEFAULT_UNCOMPRESSED_LENGTH);
  }

  public void writeTo(TCDataOutput dataOutput) {
    if (isCompressed()){
      dataOutput.writeInt(this.uncompressedLength);
    }
    dataOutput.writeInt(this.bytes.length);
    dataOutput.write(this.bytes, 0, this.bytes.length);
  }
  
  public boolean equals(Object obj){
    if (obj instanceof BinaryData) {
      BinaryData other = (BinaryData) obj;
      return ((uncompressedLength == other.uncompressedLength) && (Arrays.equals(this.bytes, other.bytes)) && this
          .getClass().equals(other.getClass()));
    }
    return false;
  }
  
  public int hashCode(){
    int hash = isCompressed() ? 21 : 37;
    for (int i = 0, n = bytes.length; i < n; i++) {
      hash = 31 * hash + bytes[i++];
    }
    return hash;
  }

  /* This method is an optimized method for writing char array when no check is needed */
  // private void writeCharArray(char[] chars, TCDataOutput output) {
  // output.writeInt(chars.length);
  // for (int i = 0, n = chars.length; i < n; i++) {
  // output.writeChar(chars[i]);
  // }
  // }
   public static BinaryData readUncompressed(TCDataInput input) throws IOException {
    int length = input.readInt();
//    if (length >= BYTE_WARN) {
//      logger.warn("Attempting to allocate a large byte array of size: " + length);
//    }
    byte[] array = new byte[length];
    input.readFully(array);
    return new BinaryData(array);
  }

  public static BinaryData readCompressed(TCDataInput input) throws IOException {
    int uncompressedLength = input.readInt();
    int length = input.readInt();
    byte[] array = new byte[length];
    input.readFully(array);
    return new BinaryData(array, uncompressedLength);
  }


}
