/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.io.TCDataInput;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

public class ByteArrayDecompressor implements Decompressor {

  public Object readCompressed(TCDataInput compressedInput) {
    byte[] compressedData;
    try {
      int uncompressedLength = compressedInput.readInt();
      compressedData = readByteArray(compressedInput);
      return inflateCompressedData(compressedData, uncompressedLength);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
  
  /* This method is an optimized method for writing char array when no check is needed */
  // private void writeCharArray(char[] chars, TCDataOutput output) {
  // output.writeInt(chars.length);
  // for (int i = 0, n = chars.length; i < n; i++) {
  // output.writeChar(chars[i]);
  // }
  // }
  public byte[] readByteArray(TCDataInput input) throws IOException {
    int length = input.readInt();
    byte[] array = new byte[length];
    input.readFully(array);
    return array;
  }  
  
  private byte[] inflateCompressedData(byte[] data, int length) throws IOException {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    InflaterInputStream iis = new InflaterInputStream(bais);
    byte uncompressed[] = new byte[length];
    int read;
    int offset = 0;
    while (length > 0 && (read = iis.read(uncompressed, offset, length)) != -1) {
      offset += read;
      length -= read;
    }
    iis.close();
    Assert.assertEquals(0, length);
    return uncompressed;
  }

}
