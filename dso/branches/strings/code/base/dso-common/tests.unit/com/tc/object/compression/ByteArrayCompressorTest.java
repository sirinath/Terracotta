/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.exception.ImplementMe;
import com.tc.io.TCDataOutput;

import junit.framework.TestCase;

public class ByteArrayCompressorTest extends TestCase {

  private Compressor compressor;
  private MockTCDataOutput mockOutput;
  
  protected void setUp() throws Exception {
    super.setUp();
    compressor = new ByteArrayCompressor();
    mockOutput = new MockTCDataOutput();
  }
  
  public void testSimple() throws Exception {
    byte[] data = getTestData();
    
    compressor.writeCompressed(data, mockOutput);
    
    assertEquals(data.length, mockOutput.uncompressedLength);
    assertTrue(data.length > mockOutput.compressedLength);
    assertTrue(data.length > mockOutput.compressedData.length);
    assertEquals(mockOutput.compressedLength, mockOutput.length);
    assertEquals(0, mockOutput.offset);
  }
  
  private byte[] getTestData() {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<10000; i++){
      sb.append("foo" + i);
    }
    return sb.toString().getBytes();
  }

  private class MockTCDataOutput implements TCDataOutput{
    private int uncompressedLength = -1;
    private int compressedLength = -1;
    private byte[] compressedData;
    private int offset;
    private int length;
    
    public void write(byte[] value, int offset, int length) {
      this.compressedData = value;
      this.offset = offset;
      this.length = length;
    }
    
    public void writeInt(int value) {
      if (uncompressedLength == -1){
        uncompressedLength = value;
      } else {
        compressedLength = value;
      }
    }
    
    public void close() {
      throw new ImplementMe();
    }

    public void write(int value) {
      throw new ImplementMe();
    }

    public void write(byte[] value) {
      throw new ImplementMe();

    }

    public void writeBoolean(boolean value) {
      throw new ImplementMe();

    }

    public void writeByte(int value) {
      throw new ImplementMe();

    }

    public void writeChar(int value) {
      throw new ImplementMe();

    }

    public void writeDouble(double value) {
      throw new ImplementMe();

    }

    public void writeFloat(float value) {
      throw new ImplementMe();

    }

    public void writeLong(long value) {
      throw new ImplementMe();

    }

    public void writeShort(int value) {
      throw new ImplementMe();

    }

    public void writeString(String string) {
      throw new ImplementMe();

    }    
    
  }

}
